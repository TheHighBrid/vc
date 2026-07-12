package com.kenza.callsim.voice

import android.util.Base64
import android.util.Log
import com.kenza.callsim.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Speaks to an ElevenLabs Conversational AI agent over a WebSocket.
 *
 * The agent bundles three things server-side so the client stays thin:
 *   - speech-to-text of the user,
 *   - the LLM "brain" (configure GPT or Gemini in the ElevenLabs dashboard),
 *   - text-to-speech in Kenza's cloned voice.
 *
 * We stream mic PCM up and play agent PCM down. Protocol reference:
 * https://elevenlabs.io/docs/conversational-ai/api-reference/conversational-ai/websocket
 *
 * Agent setup notes (see README):
 *   - Audio output format must be PCM 16000 Hz.
 *   - For a *public* agent only ELEVENLABS_AGENT_ID is required.
 *   - For a *private* agent also set ELEVENLABS_API_KEY; we exchange it for a
 *     short-lived signed URL before connecting.
 */
class ConversationalAiClient(
    private val agentId: String = BuildConfig.ELEVENLABS_AGENT_ID,
    private val apiKey: String = BuildConfig.ELEVENLABS_API_KEY,
    private val listener: Listener
) {

    interface Listener {
        fun onConnected()
        fun onAgentAudio(pcm: ByteArray)
        fun onUserTranscript(text: String)
        fun onAgentText(text: String)
        fun onInterruption()
        fun onClosed()
        fun onError(message: String)
    }

    companion object {
        private const val TAG = "ConvAiClient"
        private const val BASE = "wss://api.elevenlabs.io/v1/convai/conversation"
        private const val SIGNED_URL =
            "https://api.elevenlabs.io/v1/convai/conversation/get_signed_url"

        fun isConfigured(): Boolean = BuildConfig.ELEVENLABS_AGENT_ID.trim().isNotEmpty()
    }

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // long-lived socket
        .build()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var closedByUser = false

    fun start() {
        closedByUser = false
        if (agentId.trim().isEmpty()) {
            listener.onError("No ELEVENLABS_AGENT_ID set. See README → local.properties.")
            return
        }
        // Private agents need a signed URL; do that off the main thread.
        Thread {
            val url = try {
                if (apiKey.trim().isNotEmpty()) fetchSignedUrl() else "$BASE?agent_id=$agentId"
            } catch (e: Exception) {
                Log.e(TAG, "signed url failed", e)
                listener.onError("Could not start session: ${e.message}")
                return@Thread
            }
            connect(url)
        }.start()
    }

    private fun fetchSignedUrl(): String {
        val req = Request.Builder()
            .url("$SIGNED_URL?agent_id=$agentId")
            .header("xi-api-key", apiKey.trim())
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: $body")
            return JSONObject(body).getString("signed_url")
        }
    }

    private fun connect(url: String) {
        val req = Request.Builder().url(url).build()
        socket = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "socket open")
                // Kick off the conversation. Overrides are intentionally omitted
                // so this works even when the agent disallows client overrides.
                webSocket.send("""{"type":"conversation_initiation_client_data"}""")
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handle(webSocket, text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "socket closed $code $reason")
                if (!closedByUser) listener.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "socket failure", t)
                if (!closedByUser) listener.onError(t.message ?: "connection failed")
            }
        })
    }

    private fun handle(webSocket: WebSocket, text: String) {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (json.optString("type")) {
            "audio" -> {
                val b64 = json.optJSONObject("audio_event")?.optString("audio_base_64").orEmpty()
                if (b64.isNotEmpty()) {
                    runCatching { Base64.decode(b64, Base64.DEFAULT) }
                        .getOrNull()?.let(listener::onAgentAudio)
                }
            }
            "user_transcript" -> {
                val t = json.optJSONObject("user_transcription_event")
                    ?.optString("user_transcript").orEmpty()
                if (t.isNotEmpty()) listener.onUserTranscript(t)
            }
            "agent_response" -> {
                val t = json.optJSONObject("agent_response_event")
                    ?.optString("agent_response").orEmpty()
                if (t.isNotEmpty()) listener.onAgentText(t)
            }
            "interruption" -> listener.onInterruption()
            "ping" -> {
                val id = json.optJSONObject("ping_event")?.optInt("event_id") ?: return
                webSocket.send("""{"type":"pong","event_id":$id}""")
            }
            // vad_score, agent_response_correction, client_tool_call, etc. — not needed here.
            else -> Unit
        }
    }


    /** Send a low-priority instruction to the agent without speaking as the user. */
    fun sendContextualUpdate(text: String) {
        val escaped = JSONObject.quote(text)
        socket?.send("""{"type":"contextual_update","text":$escaped}""")
    }

    /** Send one chunk of microphone PCM (16 kHz mono 16-bit) to the agent. */
    fun sendAudio(pcm: ByteArray) {
        val s = socket ?: return
        val b64 = Base64.encodeToString(pcm, Base64.NO_WRAP)
        s.send("""{"user_audio_chunk":"$b64"}""")
    }

    fun stop() {
        closedByUser = true
        runCatching { socket?.close(1000, "bye") }
        socket = null
    }
}
