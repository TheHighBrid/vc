package com.kenza.callsim.voice

/**
 * Latency-sensitive values kept in one place so changes are visible, testable
 * and do not drift between microphone capture and Gemini session setup.
 */
object GeminiLiveTuning {
    /** Google recommends 20-40 ms microphone chunks for Live API audio. */
    const val INPUT_CHUNK_MS = 40

    /** Preserve a tiny amount of speech before server VAD detects its start. */
    const val VAD_PREFIX_PADDING_MS = 20

    /** Natural phone cadence without the two-second "hold queue" effect. */
    const val VAD_SILENCE_DURATION_MS = 600

    /** Keep client playback buffering short so streamed speech starts promptly. */
    const val OUTPUT_BUFFER_MS = 100
}
