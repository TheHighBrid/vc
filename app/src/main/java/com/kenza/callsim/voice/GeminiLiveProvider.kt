package com.kenza.callsim.voice

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Google Gemini Live API (native audio) provider — one WebSocket does
 * speech-in, the LLM brain, and speech-out. Free tier includes live audio
 * (rate-limited). Uses a preset voice (no cloning).
 *
 * Protocol: wss .../BidiGenerateContent?key=API_KEY
 *   - client sends a "setup" message, server replies "setupComplete"
 *   - client streams {"realtimeInput":{"audio":{...}}} (16 kHz PCM)
 *   - server streams {"serverContent":{"modelTurn":{parts:[{inlineData:{data}}]}}} (24 kHz PCM)
 * Docs: https://ai.google.dev/gemini-api/docs/live-api
 */
class GeminiLiveProvider(
    private val apiKey: String,
    private val model: String,
    private val voiceName: String,
    private val systemPrompt: String,
    private val listener: VoiceProvider.Listener,
) : VoiceProvider {

    companion object {
        private const val TAG = "GeminiLive"
        private const val HOST = "generativelanguage.googleapis.com"
        private const val METHOD =
            "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        const val OUTPUT_SAMPLE_RATE = 24_000
        // Native-audio live model (free tier, most realistic). Overridable in Settings.
        const val DEFAULT_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
        const val DEFAULT_VOICE = "Aoede" // breezy female preset
    }

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var closedByUser = false

    override fun start() {
        closedByUser = false
        if (apiKey.isBlank()) {
            listener.onClosed("No Gemini API key set. Open Settings and paste your key.", fatal = true)
            return
        }
        val url = "wss://$HOST/ws/$METHOD?key=${apiKey.trim()}"
        val req = Request.Builder().url(url).build()
        socket = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "open; sending setup")
                webSocket.send(buildSetup().toString())
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) = handle(text)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = handle(bytes.utf8())

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!closedByUser) listener.onClosed(
                    "closed ($code${if (reason.isNotBlank()) " $reason" else ""})",
                    fatal = isFatal(code, reason)
                )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "failure", t)
                if (closedByUser) return
                val code = response?.code ?: 0
                val msg = response?.let { "HTTP $code" } ?: (t.message ?: "connection failed")
                listener.onClosed(msg, fatal = code in 400..499)
            }
        })
    }

    private fun buildSetup(): JSONObject {
        // Note: languageCode is intentionally omitted — native-audio models
        // reject it; voice selection via voiceName works across all live models.
        val speech = JSONObject().apply {
            put("voiceConfig", JSONObject().apply {
                put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName.ifBlank { DEFAULT_VOICE }))
            })
        }
        val genConfig = JSONObject().apply {
            put("responseModalities", JSONArray().put("AUDIO"))
            put("speechConfig", speech)
        }
        val setup = JSONObject().apply {
            put("model", "models/${model.ifBlank { DEFAULT_MODEL }}")
            put("generationConfig", genConfig)
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", systemPrompt))
                ))
            }
            // Enable transcripts so the call screen can show what was said.
            put("inputAudioTranscription", JSONObject())
            put("outputAudioTranscription", JSONObject())
        }
        return JSONObject().put("setup", setup)
    }

    private fun handle(text: String) {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        when {
            json.has("setupComplete") -> listener.onReady(OUTPUT_SAMPLE_RATE)
            json.has("serverContent") -> {
                val sc = json.getJSONObject("serverContent")
                if (sc.optBoolean("interrupted")) listener.onInterrupted()
                sc.optJSONObject("inputTranscription")?.optString("text")
                    ?.takeIf { it.isNotEmpty() }?.let(listener::onUserText)
                sc.optJSONObject("outputTranscription")?.optString("text")
                    ?.takeIf { it.isNotEmpty() }?.let(listener::onAgentText)
                sc.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
                    for (i in 0 until parts.length()) {
                        val data = parts.optJSONObject(i)?.optJSONObject("inlineData")
                            ?.optString("data").orEmpty()
                        if (data.isNotEmpty()) {
                            runCatching { Base64.decode(data, Base64.DEFAULT) }
                                .getOrNull()?.let(listener::onAgentAudio)
                        }
                    }
                }
            }
            json.has("goAway") -> { /* server will disconnect soon; let onClosed handle reconnect */ }
        }
    }

    override fun sendAudio(pcm16le16k: ByteArray) {
        val s = socket ?: return
        val b64 = Base64.encodeToString(pcm16le16k, Base64.NO_WRAP)
        val msg = JSONObject().put("realtimeInput",
            JSONObject().put("audio", JSONObject()
                .put("data", b64)
                .put("mimeType", "audio/pcm;rate=16000")))
        s.send(msg.toString())
    }

    override fun stop() {
        closedByUser = true
        runCatching { socket?.close(1000, "bye") }
        socket = null
    }

    /** Auth/quota problems should not trigger reconnect loops. */
    private fun isFatal(code: Int, reason: String): Boolean {
        val r = reason.lowercase()
        return code == 1007 || code == 1008 ||
            r.contains("api key") || r.contains("permission") ||
            r.contains("quota") || r.contains("resource_exhausted") ||
            r.contains("unauthenticated") || r.contains("invalid")
    }
}
