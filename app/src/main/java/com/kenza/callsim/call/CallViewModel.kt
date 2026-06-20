package com.kenza.callsim.call

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenza.callsim.config.ConfigRepository
import com.kenza.callsim.voice.ConversationalAiClient
import com.kenza.callsim.voice.MicRecorder
import com.kenza.callsim.voice.PcmPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallViewModel(app: Application) : AndroidViewModel(app) {

    private val config = ConfigRepository(app)

    private val _state = MutableStateFlow(
        CallUiState(
            contactName = config.contactName,
            isConfigured = config.isConfigured
        )
    )
    val state: StateFlow<CallUiState> = _state.asStateFlow()

    private val audioManager =
        app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val ringtone = RingtonePlayer(app)
    private val dtmf = DtmfTones()

    private var client: ConversationalAiClient? = null
    private var mic: MicRecorder? = null
    private var player: PcmPlayer? = null

    private var timerJob: Job? = null
    private var speakingResetJob: Job? = null
    private var pendingStartAfterPermission = false

    // ---- Incoming / outgoing flow -------------------------------------------

    fun simulateIncomingCall() {
        if (_state.value.phase != CallPhase.IDLE) return
        _state.update { it.copy(phase = CallPhase.INCOMING, errorMessage = null) }
        ringtone.start()
    }

    fun placeCall() {
        ringtone.stop()
        _state.update { it.copy(phase = CallPhase.DIALING, errorMessage = null) }
        viewModelScope.launch {
            delay(1500) // simulated "calling…"
            beginConnecting()
        }
    }

    fun answerIncoming() {
        ringtone.stop()
        beginConnecting()
    }

    fun declineIncoming() {
        ringtone.stop()
        endCall()
    }

    private fun beginConnecting() {
        _state.update { it.copy(phase = CallPhase.CONNECTING) }
        if (!config.isConfigured) {
            // Pure UI demo — show the live call screen without needing the mic.
            onSessionActive(demo = true)
        } else {
            ensureMicThenStart()
        }
    }

    // ---- Mic permission ------------------------------------------------------

    private fun ensureMicThenStart() {
        val ctx = getApplication<Application>()
        val granted = ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            startVoiceSession()
        } else {
            pendingStartAfterPermission = true
            // UI observes this and launches the system permission dialog.
            _state.update { it.copy(errorMessage = NEED_MIC) }
        }
    }

    fun onMicPermissionResult(granted: Boolean) {
        if (_state.value.errorMessage == NEED_MIC) {
            _state.update { it.copy(errorMessage = null) }
        }
        if (!pendingStartAfterPermission) return
        pendingStartAfterPermission = false
        if (granted) {
            startVoiceSession()
        } else {
            _state.update {
                it.copy(
                    phase = CallPhase.ENDED,
                    errorMessage = "Microphone permission is required for the call."
                )
            }
            returnHomeSoon()
        }
    }

    // ---- Live voice session --------------------------------------------------

    private fun startVoiceSession() {
        if (!config.isConfigured) {
            // Still allow the UI to "connect" in demo mode so the call screen works.
            onSessionActive(demo = true)
            return
        }

        configureAudioRouting()
        player = PcmPlayer().also { it.start() }

        val ai = ConversationalAiClient(
            agentId = config.agentId,
            apiKey = config.apiKey,
            listener = object : ConversationalAiClient.Listener {
            override fun onConnected() = onSessionActive(demo = false)
            override fun onAgentAudio(pcm: ByteArray) {
                player?.write(pcm)
                markSpeaking()
            }
            override fun onUserTranscript(text: String) {
                _state.update { it.copy(lastUserText = text, activity = AgentActivity.THINKING) }
            }
            override fun onAgentText(text: String) {
                _state.update { it.copy(lastAgentText = text) }
            }
            override fun onInterruption() {
                player?.flush()
                _state.update { it.copy(activity = AgentActivity.LISTENING) }
            }
            override fun onClosed(reason: String) = postError("Disconnected: $reason")
            override fun onError(message: String) = postError("Connection error: $message")
        })
        client = ai
        ai.start()

        mic = MicRecorder { chunk -> ai.sendAudio(chunk) }.also { it.start() }
    }

    private fun onSessionActive(demo: Boolean) {
        _state.update {
            it.copy(
                phase = CallPhase.ACTIVE,
                activity = AgentActivity.LISTENING,
                errorMessage = if (demo) DEMO_NOTICE else null
            )
        }
        startTimer()
    }

    private fun markSpeaking() {
        _state.update { it.copy(activity = AgentActivity.SPEAKING) }
        speakingResetJob?.cancel()
        speakingResetJob = viewModelScope.launch {
            delay(700)
            _state.update {
                if (it.phase == CallPhase.ACTIVE) it.copy(activity = AgentActivity.LISTENING) else it
            }
        }
    }

    private fun postError(message: String) {
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = message) }
            endCall()
        }
    }

    // ---- In-call controls ----------------------------------------------------

    fun toggleMute() {
        val muted = !_state.value.isMuted
        mic?.muted = muted
        _state.update { it.copy(isMuted = muted) }
    }

    fun toggleSpeaker() {
        val on = !_state.value.isSpeakerOn
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = on
        _state.update { it.copy(isSpeakerOn = on) }
    }

    fun toggleKeypad() = _state.update { it.copy(isKeypadVisible = !it.isKeypadVisible) }

    fun pressKeypadKey(key: Char) = dtmf.play(key)

    // ---- Home dialer ---------------------------------------------------------

    fun appendDigit(d: Char) {
        dtmf.play(d)
        _state.update { it.copy(dialedNumber = (it.dialedNumber + d).take(20)) }
    }

    fun deleteDigit() = _state.update {
        if (it.dialedNumber.isEmpty()) it else it.copy(dialedNumber = it.dialedNumber.dropLast(1))
    }

    // ---- End / cleanup -------------------------------------------------------

    fun endCall() {
        stopVoiceSession()
        ringtone.stop()
        timerJob?.cancel()
        _state.update {
            it.copy(
                phase = CallPhase.ENDED,
                isMuted = false,
                isSpeakerOn = false,
                isKeypadVisible = false,
                activity = AgentActivity.IDLE
            )
        }
        returnHomeSoon()
    }

    private fun returnHomeSoon() {
        viewModelScope.launch {
            delay(1200)
            _state.update {
                if (it.phase == CallPhase.ENDED)
                    it.copy(phase = CallPhase.IDLE, callDurationSec = 0, lastAgentText = "", lastUserText = "")
                else it
            }
        }
    }

    private fun stopVoiceSession() {
        mic?.stop(); mic = null
        client?.stop(); client = null
        player?.stop(); player = null
        restoreAudioRouting()
    }

    // ---- Settings / consent --------------------------------------------------

    fun currentSettings(): Triple<String, String, String> =
        Triple(config.agentId, config.apiKey, config.contactName)

    fun saveSettings(agentId: String, apiKey: String, contactName: String) {
        config.agentId = agentId
        config.apiKey = apiKey
        config.contactName = contactName
        _state.update {
            it.copy(contactName = config.contactName, isConfigured = config.isConfigured)
        }
    }

    fun voiceId(): String = config.voiceId

    fun isConsentAccepted(): Boolean = config.consentAccepted
    fun acceptConsent() { config.consentAccepted = true }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update {
                    if (it.phase == CallPhase.ACTIVE) it.copy(callDurationSec = it.callDurationSec + 1) else it
                }
            }
        }
    }

    private fun configureAudioRouting() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = _state.value.isSpeakerOn
    }

    private fun restoreAudioRouting() {
        runCatching {
            audioManager.mode = AudioManager.MODE_NORMAL
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }
    }

    fun tearDown() {
        stopVoiceSession()
        ringtone.stop()
        dtmf.release()
        timerJob?.cancel()
    }

    companion object {
        const val NEED_MIC = "__need_mic__"
        const val DEMO_NOTICE =
            "Demo mode: no ELEVENLABS_AGENT_ID set, so the call screen works but there's no live voice. See README."
    }
}
