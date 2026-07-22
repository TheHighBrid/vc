package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveTuningTest {

    @Test
    fun microphoneChunksStayInsideGoogleLowLatencyRange() {
        assertTrue(GeminiLiveTuning.INPUT_CHUNK_MS in 20..40)
        assertEquals(1_280, AudioConfig.INPUT_CHUNK_BYTES)
    }

    @Test
    fun vadAllowsNaturalPauseWithoutFeelingQueued() {
        assertTrue(GeminiLiveTuning.VAD_SILENCE_DURATION_MS in 500..800)
        assertTrue(GeminiLiveTuning.VAD_PREFIX_PADDING_MS in 0..100)
    }

    @Test
    fun clientPlaybackTargetRemainsShort() {
        assertTrue(GeminiLiveTuning.OUTPUT_BUFFER_MS <= 100)
    }
}
