package com.kenza.callsim.memory

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread

/**
 * After a call ends, asks Gemini (free, using the user's own key) to distill the
 * transcript into a few durable memories — facts, preferences, events and future
 * plans — the way a caring partner would remember the important bits and let the
 * small talk go. Runs off the main thread and fails silently.
 */
object MemoryExtractor {

    private const val TAG = "MemoryExtractor"
    private const val MODEL = "gemini-2.5-flash"

    fun extract(
        context: Context,
        apiKey: String,
        contactName: String,
        transcript: List<Pair<String, String>>,
        store: MemoryStore,
    ) {
        if (apiKey.isBlank() || transcript.size < 2) return
        val appCtx = context.applicationContext
        thread(name = "memory-extract") {
            runCatching {
                val convo = transcript.joinToString("\n") { (role, text) ->
                    val who = if (role == "user") "User" else contactName
                    "$who: $text"
                }.take(8000)
                val items = requestExtraction(apiKey, contactName, convo)
                if (items.isNotEmpty()) {
                    store.addAll(items)
                    Log.i(TAG, "stored ${items.size} memories")
                }
            }.onFailure { Log.w(TAG, "extraction failed: ${it.message}") }
        }
    }

    private fun requestExtraction(apiKey: String, name: String, convo: String): List<MemoryItem> {
        val instruction =
            "You are the long-term memory of $name, an AI girlfriend on a phone call. " +
                "From the transcript, extract only durable things worth remembering for months, " +
                "like a caring partner would: facts about the user, their preferences, important " +
                "events/feelings, and any plans or promises with rough timing. Ignore small talk, " +
                "greetings and filler. Return ONLY a JSON array (no prose). Each element: " +
                "{\"kind\":\"FACT\"|\"PREFERENCE\"|\"EVENT\"|\"PLAN\", \"text\":\"short phrase about the USER\", " +
                "\"importance\":1-5, \"dueInDays\": integer (only for future PLANs)}. " +
                "Keep each text short and specific. Max 8 items. If nothing is worth remembering, return []. " +
                "IMPORTANT: if the user explicitly asks $name to remember something, ALWAYS include it with importance 5."

        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction))))
            put("contents", JSONArray().put(
                JSONObject().put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", convo)))
            ))
            put("generationConfig", JSONObject()
                .put("temperature", 0.4)
                .put("responseMimeType", "application/json"))
        }

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${apiKey.trim()}")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 40_000
            setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) {
            Log.w(TAG, "HTTP $code: ${resp.take(200)}")
            return emptyList()
        }

        val text = JSONObject(resp)
            .optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
            ?.optString("text").orEmpty()

        return parseItems(text)
    }

    private fun parseItems(json: String): List<MemoryItem> {
        val trimmed = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val arr = runCatching { JSONArray(trimmed) }.getOrNull() ?: return emptyList()
        val now = System.currentTimeMillis()
        val out = mutableListOf<MemoryItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val text = o.optString("text").trim()
            if (text.isEmpty()) continue
            val kind = runCatching { MemoryKind.valueOf(o.optString("kind", "FACT").uppercase()) }
                .getOrDefault(MemoryKind.FACT)
            val due = if (o.has("dueInDays")) now + o.optInt("dueInDays") * 86_400_000L else null
            out += MemoryItem(
                id = UUID.randomUUID().toString(),
                kind = kind,
                text = text,
                createdAt = now,
                importance = o.optInt("importance", 3).coerceIn(1, 5),
                dueAt = due,
            )
        }
        return out
    }
}
