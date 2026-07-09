package com.kenza.callsim.voice

/**
 * A pluggable live-voice backend. The app always captures the microphone as
 * 16 kHz mono signed-16-bit little-endian PCM and feeds it via [sendAudio];
 * the provider streams the agent's reply back through [Listener.onAgentAudio]
 * as PCM at the rate reported by [Listener.onReady].
 *
 * Implementations: [GeminiLiveProvider] (default, preset voice, free tier) and
 * [ElevenLabsProvider] (premium, cloned voice, quota-limited).
 */
interface VoiceProvider {
    fun start()
    fun sendAudio(pcm16le16k: ByteArray)
    fun stop()

    /**
     * Inject a text turn to prompt a spoken reply (e.g. a silence check-in).
     * Only supported by providers that accept client text; no-op otherwise.
     */
    fun sendText(text: String) {}

    interface Listener {
        /** Socket open; handshake in progress. */
        fun onConnected()
        /** Handshake done; [outputSampleRate] is the agent audio rate in Hz. Safe to stream mic now. */
        fun onReady(outputSampleRate: Int)
        /** A chunk of agent speech as signed-16-bit little-endian PCM at the onReady rate. */
        fun onAgentAudio(pcm: ByteArray)
        /** Transcript of what the user said (if the provider supplies it). */
        fun onUserText(text: String)
        /** Transcript / text of the agent's reply. */
        fun onAgentText(text: String)
        /** The user barged in; stop playing buffered agent audio. */
        fun onInterrupted()
        /**
         * Session ended. [fatal] = true for unrecoverable causes (out of quota,
         * bad key, auth) where reconnecting would just loop; the call should end
         * and show [reason] instead of retrying.
         */
        fun onClosed(reason: String, fatal: Boolean)
    }
}
