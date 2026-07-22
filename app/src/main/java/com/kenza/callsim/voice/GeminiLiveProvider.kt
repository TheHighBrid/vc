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
import kotlin.concurrent.thread

/**
 * Google Gemini Live API (native audio) provider. One WebSocket handles
 * speech-in, reasoning and speech-out. Microphone audio is sent through
 * realtimeInput so Gemini can process it incrementally with minimal delay.
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
        private const val MAX_INTERNAL_RESUMES = 4
        const val OUTPUT_SAMPLE_RATE = 24_000
        const val DEFAULT_MODEL = "gemini-3.1-flash-live-preview"
        const val DEFAULT_VOICE = "Kore"
    }

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val connectionLock = Any()
    private var nextConnectionId = 0
    @Volatile private var activeConnectionId = 0
    @Volatile private var socket: WebSocket? = null
    @Volatile private var closedByUser = false
    @Volatile private var latestSessionHandle: String? = null
    @Volatile private var modelGenerating = false
    @Volatile private var goAwayPending = false
    @Volatile private var consecutiveResumeAttempts = 0

    // Current Gemini Live models support compression and resumption. If a custom
    // older model rejects either setup field, retry once without continuity so
    // the optional model override does not make the whole call unusable.
    @Volatile private var continuityEnabled = true
    @Volatile private var retriedWithoutContinuity = false

    override fun start() {
        closedByUser = false
        if (apiKey.isBlank()) {
            listener.onClosed("No Gemini API key set. Open Settings and paste your key.", fatal = true)
            return
        }
        connect(resuming = false)
    }

    private fun connect(resuming: Boolean) {
        val connectionId = synchronized(connectionLock) {
            nextConnectionId += 1
            activeConnectionId = nextConnectionId
            activeConnectionId
        }
        val method = "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        val url = "wss://$HOST/ws/$method?key=${apiKey.trim()}"
        val request = Request.Builder().url(url).build()
        val webSocket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrent(connectionId)) return
                Log.i(TAG, "open; sending setup resuming=$resuming continuity=$continuityEnabled")
                webSocket.send(buildSetup().toString())
                if (!resuming) listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) = handle(connectionId, text)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) =
                handle(connectionId, bytes.utf8())

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrent(connectionId) || closedByUser) return
                if (code == 1007 && continuityEnabled && !retriedWithoutContinuity) {
                    Log.w(TAG, "continuity setup rejected ($reason); retrying without it")
                    continuityEnabled = false
                    retriedWithoutContinuity = true
                    latestSessionHandle = null
                    replaceConnection(resuming = false)
                    return
                }
                val fatal = isFatal(code, reason)
                if (!fatal && resumeConnection("socket closed $code $reason")) return
                listener.onClosed(
                    "closed ($code${if (reason.isNotBlank()) " $reason" else ""})",
                    fatal = fatal,
                )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isCurrent(connectionId) || closedByUser) return
                Log.e(TAG, "failure", t)
                val code = response?.code ?: 0
                val message = response?.let { "HTTP $code" } ?: (t.message ?: "connection failed")
                val fatal = code in 400..499
                if (!fatal && resumeConnection(message)) return
                listener.onClosed(message, fatal = fatal)
            }
        })
        synchronized(connectionLock) {
            if (activeConnectionId == connectionId) socket = webSocket else webSocket.cancel()
        }
    }

    private fun buildSetup(): JSONObject {
        val speech = JSONObject().apply {
            put("voiceConfig", JSONObject().apply {
                put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName.ifBlank { DEFAULT_VOICE }))
            })
        }
        val generationConfig = JSONObject().apply {
            put("responseModalities", JSONArray().put("AUDIO"))
            put("speechConfig", speech)
            put("temperature", 0.82)
            put("topP", 0.82)
            // Deliberately omit thinkingConfig. Gemini 3.1 Flash Live defaults to
            // minimal thinking, which is the provider's lowest-latency setting.
        }
        val realtimeInputConfig = JSONObject().apply {
            put("automaticActivityDetection", JSONObject().apply {
                put("disabled", false)
                put("startOfSpeechSensitivity", "START_SENSITIVITY_HIGH")
                put("endOfSpeechSensitivity", "END_SENSITIVITY_HIGH")
                put("prefixPaddingMs", GeminiLiveTuning.VAD_PREFIX_PADDING_MS)
                put("silenceDurationMs", GeminiLiveTuning.VAD_SILENCE_DURATION_MS)
            })
        }
        val setup = JSONObject().apply {
            put("model", "models/${model.ifBlank { DEFAULT_MODEL }}")
            put("generationConfig", generationConfig)
            put("realtimeInputConfig", realtimeInputConfig)
            if (continuityEnabled) {
                put("contextWindowCompression", JSONObject().put("slidingWindow", JSONObject()))
                put("sessionResumption", JSONObject().apply {
                    latestSessionHandle?.takeIf { it.isNotBlank() }?.let { put("handle", it) }
                })
            }
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", systemPrompt))
                ))
            }
            put("inputAudioTranscription", JSONObject())
            put("outputAudioTranscription", JSONObject())
        }
        return JSONObject().put("setup", setup)
    }

    private fun handle(connectionId: Int, text: String) {
        if (!isCurrent(connectionId)) return
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        when {
            json.has("setupComplete") -> {
                consecutiveResumeAttempts = 0
                goAwayPending = false
                listener.onReady(OUTPUT_SAMPLE_RATE)
            }
            json.has("sessionResumptionUpdate") -> {
                val update = json.optJSONObject("sessionResumptionUpdate") ?: return
                if (update.optBoolean("resumable")) {
                    update.optString("newHandle").takeIf { it.isNotBlank() }?.let {
                        latestSessionHandle = it
                    }
                }
            }
            json.has("serverContent") -> {
                val content = json.getJSONObject("serverContent")
                if (content.optBoolean("interrupted")) {
                    modelGenerating = false
                    listener.onInterrupted()
                }
                content.optJSONObject("inputTranscription")?.optString("text")
                    ?.takeIf { it.isNotEmpty() }?.let(listener::onUserText)
                content.optJSONObject("outputTranscription")?.optString("text")
                    ?.takeIf { it.isNotEmpty() }?.let(listener::onAgentText)
                content.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
                    modelGenerating = true
                    for (index in 0 until parts.length()) {
                        val data = parts.optJSONObject(index)?.optJSONObject("inlineData")
                            ?.optString("data").orEmpty()
                        if (data.isNotEmpty()) {
                            runCatching { Base64.decode(data, Base64.DEFAULT) }
                                .getOrNull()?.let(listener::onAgentAudio)
                        }
                    }
                }
                if (content.optBoolean("generationComplete") || content.optBoolean("turnComplete")) {
                    modelGenerating = false
                    if (goAwayPending) resumeConnection("generation completed after goAway")
                }
            }
            json.has("goAway") -> {
                goAwayPending = true
                Log.i(TAG, "goAway received: ${json.optJSONObject("goAway")?.opt("timeLeft")}")
                scheduleGoAwayResume(connectionId)
            }
        }
    }

    override fun sendAudio(pcm16le16k: ByteArray) {
        val currentSocket = socket ?: return
        val encoded = Base64.encodeToString(pcm16le16k, Base64.NO_WRAP)
        val message = JSONObject().put(
            "realtimeInput",
            JSONObject().put(
                "audio",
                JSONObject()
                    .put("data", encoded)
                    .put("mimeType", "audio/pcm;rate=16000"),
            ),
        )
        currentSocket.send(message.toString())
    }

    override fun sendText(text: String) {
        if (text.isBlank()) return
        // Gemini 3.1 supports clientContent only for seeding initial history.
        // Ongoing director cues must use realtimeInput to remain truly live.
        socket?.send(
            JSONObject()
                .put("realtimeInput", JSONObject().put("text", text))
                .toString()
        )
    }

    private fun scheduleGoAwayResume(connectionId: Int) {
        thread(name = "gemini-go-away") {
            val deadline = System.currentTimeMillis() + 4_000L
            while (isCurrent(connectionId) && modelGenerating && System.currentTimeMillis() < deadline) {
                Thread.sleep(100L)
            }
            if (isCurrent(connectionId) && goAwayPending && !closedByUser) {
                resumeConnection("server goAway")
            }
        }
    }

    @Synchronized
    private fun resumeConnection(reason: String): Boolean {
        if (closedByUser || !continuityEnabled) return false
        val handle = latestSessionHandle?.takeIf { it.isNotBlank() } ?: return false
        if (consecutiveResumeAttempts >= MAX_INTERNAL_RESUMES) return false
        consecutiveResumeAttempts += 1
        Log.i(TAG, "resuming Gemini session after $reason; attempt=$consecutiveResumeAttempts")
        latestSessionHandle = handle
        replaceConnection(resuming = true)
        return true
    }

    private fun replaceConnection(resuming: Boolean) {
        val previous = synchronized(connectionLock) {
            activeConnectionId = ++nextConnectionId
            val old = socket
            socket = null
            old
        }
        runCatching { previous?.cancel() }
        connect(resuming)
    }

    private fun isCurrent(connectionId: Int): Boolean = activeConnectionId == connectionId

    override fun stop() {
        closedByUser = true
        val previous = synchronized(connectionLock) {
            activeConnectionId = ++nextConnectionId
            val old = socket
            socket = null
            old
        }
        runCatching { previous?.close(1000, "bye") }
    }

    private fun isFatal(code: Int, reason: String): Boolean {
        val lower = reason.lowercase()
        return code == 1008 ||
            lower.contains("api key") || lower.contains("permission") ||
            lower.contains("quota") || lower.contains("resource_exhausted") ||
            lower.contains("unauthenticated") || lower.contains("invalid")
    }
}
