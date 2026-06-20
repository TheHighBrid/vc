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
        /**
         * Fired when the agent sends conversation_initiation_metadata. [sampleRate]
         * is the agent's output rate (Hz); audio delivered to onAgentAudio is
         * already normalized to signed 16-bit little-endian PCM at this rate.
         */
        fun onReady(sampleRate: Int)
        fun onAgentAudio(pcm: ByteArray)
        fun onUserTranscript(text: String)
        fun onAgentText(text: String)
        fun onInterruption()
        fun onClosed(reason: String)
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
    @Volatile private var outputIsMulaw = false

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
                // The first client frame must be the initiation message. We send
                // the minimal valid form (no overrides) — same as the official SDK.
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
                if (!closedByUser) listener.onClosed("closed ($code${if (reason.isNotBlank()) " $reason" else ""})")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "socket failure", t)
                val detail = response?.let { "HTTP ${it.code}" } ?: t.message ?: "connection failed"
                if (!closedByUser) listener.onError(detail)
            }
        })
    }

    private fun handle(webSocket: WebSocket, text: String) {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (json.optString("type")) {
            "conversation_initiation_metadata" -> {
                val fmt = json.optJSONObject("conversation_initiation_metadata_event")
                    ?.optString("agent_output_audio_format").orEmpty()
                outputIsMulaw = fmt.startsWith("ulaw", ignoreCase = true)
                val sampleRate = parseSampleRate(fmt)
                Log.i(TAG, "agent output format=$fmt -> rate=$sampleRate mulaw=$outputIsMulaw")
                listener.onReady(sampleRate)
            }
            "audio" -> {
                val b64 = json.optJSONObject("audio_event")?.optString("audio_base_64").orEmpty()
                if (b64.isNotEmpty()) {
                    runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()?.let { raw ->
                        val pcm = if (outputIsMulaw) ulawToPcm16(raw) else raw
                        listener.onAgentAudio(pcm)
                    }
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

    /** Format strings look like "pcm_16000", "pcm_24000", "ulaw_8000". */
    private fun parseSampleRate(format: String): Int =
        format.substringAfterLast('_').toIntOrNull() ?: 16_000

    /**
     * Decode 8-bit G.711 µ-law to signed 16-bit little-endian PCM. ElevenLabs
     * uses this for the "ulaw_8000" output format; AudioTrack only plays PCM.
     */
    private fun ulawToPcm16(ulaw: ByteArray): ByteArray {
        val bias = 0x84
        val out = ByteArray(ulaw.size * 2)
        for (i in ulaw.indices) {
            val u = ulaw[i].toInt().inv() and 0xFF
            val exponent = (u shr 4) and 0x07
            val mantissa = u and 0x0F
            var sample = ((mantissa shl 3) + bias) shl exponent
            sample -= bias
            if (u and 0x80 != 0) sample = -sample
            out[i * 2] = (sample and 0xFF).toByte()
            out[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return out
    }
}
