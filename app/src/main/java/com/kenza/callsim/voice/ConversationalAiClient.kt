package com.kenza.callsim.voice

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ElevenLabs Conversational AI provider (premium option — Kenza's cloned voice).
 *
 * The agent bundles speech-to-text, the LLM brain (GPT/Gemini configured in the
 * ElevenLabs dashboard), and cloned-voice TTS. We stream mic PCM up and play the
 * agent's PCM down. Quota-limited, so it's offered as the optional premium voice.
 *
 * Protocol: https://elevenlabs.io/docs/conversational-ai/api-reference/websocket
 */
class ElevenLabsProvider(
    private val agentId: String,
    private val apiKey: String,
    private val listener: VoiceProvider.Listener,
    // Optional persona + memory sent as a prompt override at connect time.
    // Requires "Overrides" enabled on the agent; null = use the dashboard prompt.
    private val promptOverride: String? = null,
) : VoiceProvider {

    companion object {
        private const val TAG = "ElevenLabsProvider"
        private const val BASE = "wss://api.elevenlabs.io/v1/convai/conversation"
        private const val SIGNED_URL =
            "https://api.elevenlabs.io/v1/convai/conversation/get_signed_url"
    }

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // long-lived socket
        .build()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var closedByUser = false
    @Volatile private var outputIsMulaw = false

    override fun start() {
        closedByUser = false
        if (agentId.trim().isEmpty()) {
            listener.onClosed("No ElevenLabs Agent ID set. Open Settings to add one.", fatal = true)
            return
        }
        // Private agents need a signed URL; do that off the main thread.
        Thread {
            val url = try {
                if (apiKey.trim().isNotEmpty()) fetchSignedUrl() else "$BASE?agent_id=$agentId"
            } catch (e: Exception) {
                Log.e(TAG, "signed url failed", e)
                listener.onClosed("Could not start session: ${e.message}", fatal = true)
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
                webSocket.send(buildInitMessage())
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) = handle(webSocket, text)

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "socket closed $code $reason")
                if (!closedByUser) listener.onClosed(
                    "closed ($code${if (reason.isNotBlank()) " $reason" else ""})",
                    fatal = isFatal(code, reason)
                )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "socket failure", t)
                if (closedByUser) return
                val code = response?.code ?: 0
                val detail = response?.let { "HTTP $code" } ?: t.message ?: "connection failed"
                listener.onClosed(detail, fatal = code in 400..499)
            }
        })
    }

    private fun buildInitMessage(): String {
        val init = JSONObject().put("type", "conversation_initiation_client_data")
        if (!promptOverride.isNullOrBlank()) {
            val agent = JSONObject().put("prompt", JSONObject().put("prompt", promptOverride))
            init.put("conversation_config_override", JSONObject().put("agent", agent))
        }
        return init.toString()
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
                if (t.isNotEmpty()) listener.onUserText(t)
            }
            "agent_response" -> {
                val t = json.optJSONObject("agent_response_event")
                    ?.optString("agent_response").orEmpty()
                if (t.isNotEmpty()) listener.onAgentText(t)
            }
            "interruption" -> listener.onInterrupted()
            "ping" -> {
                val id = json.optJSONObject("ping_event")?.optInt("event_id") ?: return
                webSocket.send("""{"type":"pong","event_id":$id}""")
            }
            else -> Unit
        }
    }

    override fun sendAudio(pcm16le16k: ByteArray) {
        val s = socket ?: return
        val b64 = Base64.encodeToString(pcm16le16k, Base64.NO_WRAP)
        s.send("""{"user_audio_chunk":"$b64"}""")
    }

    override fun stop() {
        closedByUser = true
        runCatching { socket?.close(1000, "bye") }
        socket = null
    }

    private fun parseSampleRate(format: String): Int =
        format.substringAfterLast('_').toIntOrNull() ?: 16_000

    /** Out-of-credits / auth failures shouldn't trigger reconnect loops. */
    private fun isFatal(code: Int, reason: String): Boolean {
        val r = reason.lowercase()
        return code == 1008 || r.contains("quota") || r.contains("credit") ||
            r.contains("unauthorized") || r.contains("limit") || r.contains("exceeded")
    }

    /** Decode 8-bit G.711 µ-law to signed 16-bit little-endian PCM. */
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
