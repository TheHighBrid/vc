package com.kenza.callsim.call

/** High-level phase of the simulated call, drives which screen is shown. */
enum class CallPhase {
    IDLE,        // Home / keypad
    INCOMING,    // Ringing, waiting for the user to answer
    DIALING,     // User placed the call, "calling…"
    CONNECTING,  // Answered, opening the voice session
    ACTIVE,      // Live two-way conversation
    ENDED        // Brief "Call Ended" before returning home
}

/** Status text shown under the contact name. */
enum class AgentActivity { IDLE, LISTENING, THINKING, SPEAKING }

data class CallUiState(
    val phase: CallPhase = CallPhase.IDLE,
    val contactName: String = "Kenza",
    val dialedNumber: String = "",
    val callDurationSec: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isKeypadVisible: Boolean = false,
    val activity: AgentActivity = AgentActivity.IDLE,
    val micStreaming: Boolean = false,
    val lastAgentText: String = "",
    val lastUserText: String = "",
    val errorMessage: String? = null,
    val isConfigured: Boolean = false
)
