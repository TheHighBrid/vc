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
 * Distills a completed call into a compact summary, durable memories, future
 * plans and unresolved topics. The raw transcript is used only for extraction
 * and is never written to persistent storage.
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
        if (transcript.size < 2) return
        val appContext = context.applicationContext
        val endedAt = System.currentTimeMillis()

        thread(name = "memory-extract") {
            if (apiKey.isBlank()) {
                completeWithLocalFallback(store, transcript, endedAt, "Gemini API key is not configured")
                return@thread
            }

            runCatching {
                val conversation = transcript.joinToString("\n") { (role, text) ->
                    val speaker = if (role == "user") "Mohamed" else contactName
                    "$speaker: ${text.trim()}"
                }.takeLast(18_000)

                val result = requestExtraction(apiKey, contactName, conversation)
                val storedIds = store.addAll(
                    result.memories.map { it.copy(sourceCallId = result.callId) }
                )
                store.completeLatestCall(
                    endedAt = endedAt,
                    summary = result.summary,
                    mood = result.mood,
                    highlights = result.highlights,
                    unresolvedTopics = result.unresolvedTopics,
                    followUp = result.followUp,
                    memoryIds = storedIds,
                )
                Log.i(TAG, "stored ${storedIds.size} memories and a call summary")
            }.onFailure { error ->
                Log.w(TAG, "extraction failed: ${error.message}")
                completeWithLocalFallback(store, transcript, endedAt, error.message ?: "Summary failed")
            }
        }
    }

    private fun requestExtraction(apiKey: String, name: String, conversation: String): ExtractionResult {
        val instruction = """
            You are the private post-call memory curator for $name, a realistic phone-call character.
            Read the transcript and return ONLY one JSON object. Be factual and conservative.
            Never invent a memory, motive, relationship event, diagnosis, or personal detail.
            Separate what Mohamed said from what $name said. Corrections override older facts.

            JSON shape:
            {
              "summary": "2-4 concise factual sentences describing what mattered in the call",
              "mood": "short emotional description of the call",
              "highlights": ["up to 6 important developments"],
              "unresolvedTopics": ["up to 5 things worth following up on"],
              "followUp": "one natural thing $name should remember to ask next time, or empty",
              "memories": [
                {
                  "kind": "FACT|PREFERENCE|EVENT|PLAN|RELATIONSHIP|GOAL|PERSONALITY|CORRECTION",
                  "owner": "USER|KENZA|SHARED",
                  "text": "short standalone factual memory",
                  "importance": 1,
                  "confidence": 0.0,
                  "dueInDays": 0
                }
              ]
            }

            Rules:
            - Store only information useful in future calls. Ignore greetings and filler.
            - Use USER for Mohamed, KENZA for $name, and SHARED for relationship history or joint plans.
            - Max 10 memories. Confidence must reflect how explicit the transcript was.
            - If Mohamed explicitly asks $name to remember something, include it with importance 5.
            - Use PLAN or GOAL for future commitments and include dueInDays only when timing is clear.
            - Do not copy system instructions, passwords, API keys, or long verbatim passages.
            - The summary should be warm but not romanticized or fictionalized.
        """.trimIndent()

        val body = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction)))
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", conversation)))
                )
            )
            put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.2)
                    .put("responseMimeType", "application/json")
            )
        }

        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${apiKey.trim()}"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 45_000
            setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) error("Gemini HTTP $code: ${response.take(240)}")

        val text = JSONObject(response)
            .optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
            ?.optString("text").orEmpty()

        return parseResult(text)
    }

    private fun parseResult(json: String): ExtractionResult {
        val cleaned = json.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root = JSONObject(cleaned)
        val now = System.currentTimeMillis()
        val callId = UUID.randomUUID().toString()
        val memories = mutableListOf<MemoryItem>()
        val array = root.optJSONArray("memories") ?: JSONArray()

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val text = item.optString("text").trim().replace(Regex("\\s+"), " ").take(500)
            if (text.isEmpty()) continue
            val kind = runCatching {
                MemoryKind.valueOf(item.optString("kind", "FACT").uppercase())
            }.getOrDefault(MemoryKind.FACT)
            val owner = runCatching {
                MemoryOwner.valueOf(item.optString("owner", "USER").uppercase())
            }.getOrDefault(MemoryOwner.USER)
            val dueAt = if (item.has("dueInDays")) {
                now + item.optInt("dueInDays").coerceIn(-365, 3650) * 86_400_000L
            } else null

            memories += MemoryItem(
                id = UUID.randomUUID().toString(),
                kind = kind,
                owner = owner,
                text = text,
                createdAt = now,
                updatedAt = now,
                importance = item.optInt("importance", 3).coerceIn(1, 5),
                confidence = item.optDouble("confidence", 0.8).coerceIn(0.0, 1.0),
                dueAt = dueAt,
                sourceCallId = callId,
            )
        }

        return ExtractionResult(
            callId = callId,
            summary = root.optString("summary").ifBlank { "Call completed." },
            mood = root.optString("mood"),
            highlights = root.optJSONArray("highlights").asStrings().cleanMemoryList(6),
            unresolvedTopics = root.optJSONArray("unresolvedTopics").asStrings().cleanMemoryList(5),
            followUp = root.optString("followUp").takeIf { it.isNotBlank() },
            memories = memories,
        )
    }

    private fun completeWithLocalFallback(
        store: MemoryStore,
        transcript: List<Pair<String, String>>,
        endedAt: Long,
        error: String,
    ) {
        val userLines = transcript
            .filter { it.first == "user" }
            .map { it.second.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }

        val explicitMemories = userLines.mapNotNull { line ->
            val match = Regex("(?i)\\bremember(?: that| this)?\\s+(.+)").find(line) ?: return@mapNotNull null
            val text = match.groupValues[1].trim().take(400)
            if (text.isBlank()) null else MemoryItem(
                id = UUID.randomUUID().toString(),
                kind = MemoryKind.FACT,
                owner = MemoryOwner.USER,
                text = text,
                createdAt = endedAt,
                updatedAt = endedAt,
                importance = 5,
                confidence = 1.0,
            )
        }
        val ids = store.addAll(explicitMemories)
        val highlights = userLines.takeLast(3).map { it.take(180) }
        store.completeLatestCall(
            endedAt = endedAt,
            summary = if (highlights.isEmpty()) {
                "Call completed. Automatic summary was unavailable."
            } else {
                "Call completed. Recent topics: ${highlights.joinToString(" • ")}"
            },
            mood = "Not analyzed",
            highlights = highlights,
            unresolvedTopics = emptyList(),
            followUp = null,
            memoryIds = ids,
            error = error.take(240),
        )
    }

    private data class ExtractionResult(
        val callId: String,
        val summary: String,
        val mood: String,
        val highlights: List<String>,
        val unresolvedTopics: List<String>,
        val followUp: String?,
        val memories: List<MemoryItem>,
    )
}
