package com.kenza.callsim.call

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenza.callsim.config.ConfigRepository
import com.kenza.callsim.config.ProviderType
import com.kenza.callsim.config.SettingsData
import com.kenza.callsim.voice.ElevenLabsProvider
import com.kenza.callsim.voice.GeminiLiveProvider
import com.kenza.callsim.voice.MicRecorder
import com.kenza.callsim.voice.PcmPlayer
import com.kenza.callsim.voice.VoiceProvider
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

    private var client: VoiceProvider? = null
    private var mic: MicRecorder? = null
    private var player: PcmPlayer? = null

    private var timerJob: Job? = null
    private var speakingResetJob: Job? = null
    private var pendingStartAfterPermission = false

    // Auto-reconnect: when a provider ends a session (e.g. a server-side
    // conversation-duration limit) we transparently re-establish the call. To
    // avoid replay loops (e.g. ElevenLabs out of quota replaying its greeting),
    // the retry budget only refreshes once the user has actually spoken — proof
    // of a real two-way call rather than a failing reconnect cycle. Fatal causes
    // (out of quota, bad key, mic failure) never reconnect.
    private var userEnded = false
    private var reconnectAttempts = 0
    private val maxConsecutiveReconnects = 4
    private var hadUserInteraction = false

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
        userEnded = false
        reconnectAttempts = 0
        hadUserInteraction = false
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

        val ai = buildProvider(object : VoiceProvider.Listener {
            override fun onConnected() {
                _state.update { it.copy(phase = CallPhase.CONNECTING) }
            }
            override fun onReady(outputSampleRate: Int) {
                // Now we know the agent's audio rate — open the player to match,
                // then start streaming the mic and go live.
                if (player == null) {
                    player = PcmPlayer(outputSampleRate).also { it.start() }
                }
                if (mic == null) {
                    mic = MicRecorder(
                        onChunk = { chunk -> client?.sendAudio(chunk) },
                        onError = { msg -> handleDisconnect("Microphone: $msg", fatal = true) },
                        onStreaming = { _state.update { it.copy(micStreaming = true) } }
                    ).also { it.start() }
                }
                onSessionActive(demo = false)
            }
            override fun onAgentAudio(pcm: ByteArray) {
                mic?.agentSpeaking = true
                player?.write(pcm)
                markSpeaking()
            }
            override fun onUserText(text: String) {
                // Real two-way interaction — a genuine call, so allow further reconnects.
                hadUserInteraction = true
                _state.update { it.copy(lastUserText = text, activity = AgentActivity.THINKING) }
            }
            override fun onAgentText(text: String) {
                _state.update { it.copy(lastAgentText = text) }
            }
            override fun onInterrupted() {
                player?.flush()
                _state.update { it.copy(activity = AgentActivity.LISTENING) }
            }
            override fun onClosed(reason: String, fatal: Boolean) =
                handleDisconnect("Disconnected: $reason", fatal)
        })
        client = ai
        ai.start()
    }

    private fun buildProvider(listener: VoiceProvider.Listener): VoiceProvider =
        when (config.provider) {
            ProviderType.GEMINI -> GeminiLiveProvider(
                apiKey = config.geminiApiKey,
                model = config.geminiModel,
                voiceName = config.geminiVoice,
                systemPrompt = config.personaPrompt,
                listener = listener,
            )
            ProviderType.ELEVENLABS -> ElevenLabsProvider(
                agentId = config.agentId,
                apiKey = config.apiKey,
                listener = listener,
            )
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
            // Hold the mic gate just past the last audio chunk so the speaker
            // tail doesn't bleed in, then reopen it for the user's turn. Kept
            // short so the user can reply quickly (AEC covers residual echo).
            delay(350)
            mic?.agentSpeaking = false
            _state.update {
                if (it.phase == CallPhase.ACTIVE) it.copy(activity = AgentActivity.LISTENING) else it
            }
        }
    }

    /**
     * Called when the voice session drops. If the user is still on the call and
     * the failure isn't fatal, transparently reconnect so a server-side
     * conversation-duration limit (or a network blip) doesn't end the call.
     */
    private fun handleDisconnect(message: String, fatal: Boolean) {
        viewModelScope.launch {
            if (userEnded) return@launch
            val phase = _state.value.phase
            val onCall = phase == CallPhase.ACTIVE || phase == CallPhase.CONNECTING

            // Only a session with real two-way interaction refreshes the retry
            // budget. This lets a genuine long call reconnect indefinitely past a
            // server duration limit, while a quota/greeting replay loop (user
            // never gets to speak) exhausts the budget and stops.
            if (hadUserInteraction) {
                reconnectAttempts = 0
                hadUserInteraction = false
            }

            if (onCall && !fatal && reconnectAttempts < maxConsecutiveReconnects) {
                reconnectAttempts++
                stopVoiceSession()
                _state.update {
                    it.copy(phase = CallPhase.CONNECTING, micStreaming = false, activity = AgentActivity.THINKING)
                }
                delay(700)
                if (!userEnded) startVoiceSession()
            } else {
                _state.update { it.copy(errorMessage = friendlyError(message, fatal)) }
                finishCall(null)
            }
        }
    }

    private fun friendlyError(message: String, fatal: Boolean): String = when {
        message.contains("quota", true) || message.contains("RESOURCE_EXHAUSTED", true) ||
            message.contains("credit", true) || message.contains("limit", true) ->
            "Out of provider quota/credits. Switch voice provider in Settings, or top up your plan."
        message.contains("API key", true) || message.contains("Agent ID", true) ||
            message.contains("401") || message.contains("403") || message.contains("UNAUTHENTICATED", true) ->
            "Authentication failed. Check your key/ID in Settings.\n($message)"
        fatal -> message
        else -> message
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

    /** User hung up — stop for good (no auto-reconnect). */
    fun endCall() {
        userEnded = true
        finishCall(null)
    }

    private fun finishCall(error: String?) {
        stopVoiceSession()
        ringtone.stop()
        timerJob?.cancel()
        _state.update {
            it.copy(
                phase = CallPhase.ENDED,
                errorMessage = error ?: it.errorMessage,
                isMuted = false,
                isSpeakerOn = false,
                isKeypadVisible = false,
                micStreaming = false,
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

    fun currentSettings() = SettingsData(
        provider = config.provider,
        geminiApiKey = config.geminiApiKey,
        geminiVoice = config.geminiVoice,
        geminiModel = config.geminiModel,
        agentId = config.agentId,
        elevenApiKey = config.apiKey,
        contactName = config.contactName,
        persona = config.personaPrompt,
    )

    fun saveSettings(s: SettingsData) {
        config.provider = s.provider
        config.geminiApiKey = s.geminiApiKey
        config.geminiVoice = s.geminiVoice
        config.geminiModel = s.geminiModel
        config.agentId = s.agentId
        config.apiKey = s.elevenApiKey
        config.contactName = s.contactName
        config.personaPrompt = s.persona
        _state.update {
            it.copy(contactName = config.contactName, isConfigured = config.isConfigured)
        }
    }

    fun voiceId(): String = config.voiceId

    fun clearError() = _state.update { it.copy(errorMessage = null) }

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
