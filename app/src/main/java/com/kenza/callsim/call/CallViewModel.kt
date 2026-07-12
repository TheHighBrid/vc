package com.kenza.callsim.call

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenza.callsim.config.ConfigRepository
import com.kenza.callsim.config.ProviderType
import com.kenza.callsim.config.SettingsData
import com.kenza.callsim.memory.MemoryContext
import com.kenza.callsim.memory.MemoryExtractor
import com.kenza.callsim.memory.MemoryStore
import com.kenza.callsim.schedule.IncomingCallService
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

    // Long-term memory: transcript accumulates during a call and is distilled
    // into durable memories when it ends.
    private val memory = MemoryStore(app)
    private val transcript = mutableListOf<Pair<String, String>>()
    private var callStartedAt = 0L

    // Natural call ending + silence handling.
    private var endCallJob: Job? = null
    private var silenceJob: Job? = null
    private var lastActivityAt = 0L   // last time EITHER of them spoke
    private var lastNudgeAt = 0L
    private var nudgeCount = 0

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

    // ElevenLabs multi-key failover: rotate through (agentId, apiKey) pairs as
    // each account runs out of credit, so the call keeps going uninterrupted.
    private var elevenCreds: List<Pair<String, String>> = emptyList()
    private var credIndex = 0

    // ---- Incoming / outgoing flow -------------------------------------------

    fun simulateIncomingCall() {
        if (_state.value.phase != CallPhase.IDLE) return
        _state.update { it.copy(phase = CallPhase.INCOMING, errorMessage = null) }
        ringtone.start()
    }

    /**
     * Triggered by a fired schedule (via the full-screen notification). Rings
     * from any idle/ended state; ignored if a call is already in progress.
     */
    fun onScheduledIncomingCall() {
        val phase = _state.value.phase
        if (phase != CallPhase.IDLE && phase != CallPhase.ENDED) return
        // The foreground IncomingCallService already rings + vibrates; just show
        // the incoming UI so we don't double-ring.
        _state.update { it.copy(phase = CallPhase.INCOMING, errorMessage = null) }
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
        IncomingCallService.stop(getApplication())
        beginConnecting()
    }

    fun declineIncoming() {
        ringtone.stop()
        IncomingCallService.stop(getApplication())
        endCall()
    }

    private fun beginConnecting() {
        userEnded = false
        reconnectAttempts = 0
        hadUserInteraction = false
        elevenCreds = config.elevenCredentials()
        credIndex = 0
        transcript.clear()
        callStartedAt = System.currentTimeMillis()
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
                lastActivityAt = System.currentTimeMillis()
                player?.write(pcm)
                markSpeaking()
            }
            override fun onUserText(text: String) {
                // Real two-way interaction — a genuine call, so allow further reconnects.
                hadUserInteraction = true
                if (text.isNotBlank()) transcript.add("user" to text.trim())
                lastActivityAt = System.currentTimeMillis()
                nudgeCount = 0
                onConversationLine(fromUser = true, text = text)
                _state.update { it.copy(lastUserText = text, activity = AgentActivity.THINKING) }
            }
            override fun onAgentText(text: String) {
                if (text.isNotBlank()) transcript.add("agent" to text.trim())
                lastActivityAt = System.currentTimeMillis()
                onConversationLine(fromUser = false, text = text)
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
                // Persona + live memory briefing + delivery style (native-audio tends
                // to over-perform; this keeps her voice flat and human).
                systemPrompt = config.personaPrompt + GEMINI_DELIVERY_STYLE +
                    MemoryContext.build(memory, config.contactName, System.currentTimeMillis()),
                listener = listener,
            )
            ProviderType.ELEVENLABS -> {
                val (aid, key) = elevenCreds.getOrElse(credIndex) { config.agentId to config.apiKey }
                val override = if (config.elevenInjectMemory)
                    config.personaPrompt +
                        MemoryContext.build(memory, config.contactName, System.currentTimeMillis())
                else null
                ElevenLabsProvider(
                    agentId = aid,
                    apiKey = key,
                    listener = listener,
                    promptOverride = override,
                )
            }
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
        if (!demo) startSilenceMonitor()
    }

    // ---- Natural call ending + silence handling -----------------------------

    private fun onConversationLine(fromUser: Boolean, text: String) {
        when {
            isAngryHangup(text) -> armHangup(2500)  // she's had enough → hang up
            isFarewell(text) -> armHangup(2800)     // goodbyes → wrap up (re-armed on each bye)
            else -> cancelHangup()                  // anyone keeps talking → stay on the call
        }
    }

    /** End the call after [delayMs] unless a new (non-goodbye) line cancels it. */
    private fun armHangup(delayMs: Long) {
        endCallJob?.cancel()
        endCallJob = viewModelScope.launch {
            delay(delayMs)
            if (_state.value.phase == CallPhase.ACTIVE) {
                userEnded = true // her decision to end — don't auto-reconnect
                finishCall(null)
            }
        }
    }

    private fun cancelHangup() {
        endCallJob?.cancel()
        endCallJob = null
    }

    private fun isFarewell(text: String): Boolean {
        val t = text.lowercase()
        return listOf(
            "bye", "goodnight", "good night", "night night", "talk to you later",
            "talk later", "talk soon", "gotta go", "i'll let you go", "let you go",
            "see you", "see ya", "call you later", "sleep well", "take care", "later gator"
        ).any { t.contains(it) }
    }

    private fun isAngryHangup(text: String): Boolean {
        val t = text.lowercase()
        return listOf(
            "i'm hanging up", "im hanging up", "i'm done", "im done", "we're done",
            "were done", "don't call me", "dont call me", "lose my number",
            "leave me alone", "forget it", "don't ever", "dont ever", "i'm out", "im out"
        ).any { t.contains(it) }
    }

    /**
     * Real conversations aren't silent for minutes. If the user goes quiet, nudge
     * Kenza to check in, escalate if it continues, and eventually let her hang up.
     */
    private fun startSilenceMonitor() {
        silenceJob?.cancel()
        lastActivityAt = System.currentTimeMillis()
        lastNudgeAt = 0L
        nudgeCount = 0
        silenceJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                if (_state.value.phase != CallPhase.ACTIVE) continue
                if (endCallJob != null) continue                       // already wrapping up
                if (_state.value.activity == AgentActivity.SPEAKING) continue
                val now = System.currentTimeMillis()
                // Silence = neither of them has spoken. Measured from the last
                // utterance by EITHER party, so she never checks in the instant
                // she finishes her own sentence.
                val quiet = now - lastActivityAt
                if (now - lastNudgeAt < 12_000) continue               // long cooldown after a nudge
                when {
                    nudgeCount == 0 && quiet > 16_000 -> nudgeSilence(1)
                    nudgeCount == 1 && quiet > 35_000 -> nudgeSilence(2)
                    nudgeCount == 2 && quiet > 55_000 -> nudgeSilence(3)
                    nudgeCount >= 3 && quiet > 80_000 -> {
                        userEnded = true
                        finishCall(null)                               // she gives up on the silence
                    }
                }
            }
        }
    }

    private fun nudgeSilence(level: Int) {
        lastNudgeAt = System.currentTimeMillis()
        nudgeCount = level
        val cue = when (level) {
            1 -> "The other person has gone quiet and hasn't said anything for a bit. React " +
                "naturally — casually check if they're still there or gently poke at the silence."
            2 -> "They're still not responding. Get a little more bothered or teasing about how " +
                "quiet they are, like a real girlfriend would."
            else -> "They've been silent a long time. You're kind of over it — get one reaction " +
                "out of them, or tell them you're gonna let them go / hang up soon."
        }
        client?.sendText("[[DIRECTOR: $cue Do NOT read this instruction aloud.]]")
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

            // Failover: the current ElevenLabs account is out of credit but we
            // have another key queued — rotate to it and reconnect without
            // ending the call. (Server state can't carry over, but the call
            // continues on the next account within ~1s.)
            if (onCall && isQuotaExhausted(message) && credIndex < elevenCreds.lastIndex) {
                credIndex++
                reconnectAttempts = 0
                stopVoiceSession()
                _state.update {
                    it.copy(phase = CallPhase.CONNECTING, micStreaming = false,
                        activity = AgentActivity.THINKING)
                }
                delay(500)
                if (!userEnded) startVoiceSession()
                return@launch
            }

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

    /**
     * True only for a genuine credit/quota exhaustion — NOT for generic policy
     * closes (WebSocket 1008), rate/concurrency limits, or override rejections.
     * Misclassifying those as quota made a single failure stampede through every
     * backup key and falsely report "out of credit."
     */
    private fun isQuotaExhausted(message: String): Boolean {
        val m = message.lowercase()
        return m.contains("quota") || m.contains("credit") ||
            m.contains("insufficient") || m.contains("resource_exhausted")
    }

    /** ElevenLabs blocks free-tier TTS across accounts it thinks are the same person. */
    private fun isFreeTierBlocked(message: String): Boolean {
        val m = message.lowercase()
        return m.contains("unusual activity") || m.contains("free tier") ||
            m.contains("free_tier") || m.contains("abuse")
    }

    private fun friendlyError(message: String, fatal: Boolean): String = when {
        isFreeTierBlocked(message) ->
            "ElevenLabs blocked free-tier voice on this account (it flags multiple free " +
                "accounts as \"unusual activity\"). Use the Gemini voice, or a paid ElevenLabs " +
                "plan.\n($message)"
        isQuotaExhausted(message) && elevenCreds.size > 1 ->
            "All ${elevenCreds.size} ElevenLabs keys reported out of credit. If an account still " +
                "shows credit, the real reason is above.\n($message)"
        isQuotaExhausted(message) ->
            "Out of ElevenLabs credit. Add a backup key in Settings, or top up.\n($message)"
        message.contains("override", true) ->
            "ElevenLabs rejected the persona/memory override. Enable Security → Overrides → " +
                "System prompt on your agent, or turn that toggle off in Settings.\n($message)"
        message.contains("API key", true) || message.contains("Agent ID", true) ||
            message.contains("401") || message.contains("403") || message.contains("UNAUTHENTICATED", true) ->
            "Authentication failed. Check your key/ID in Settings.\n($message)"
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
        persistMemory()
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

    /**
     * On call end: log the call (for habit awareness) and, if there was a real
     * exchange, distill the transcript into durable memories via Gemini.
     */
    private fun persistMemory() {
        val startedAt = callStartedAt
        callStartedAt = 0L
        if (startedAt == 0L) return

        val realExchange = transcript.count { it.first == "user" } >= 1 && transcript.size >= 2
        if (realExchange) {
            memory.recordCall(startedAt)
            MemoryExtractor.extract(
                getApplication(), config.geminiApiKey, config.contactName,
                transcript.toList(), memory
            )
        }
        transcript.clear()
    }

    private fun stopVoiceSession() {
        silenceJob?.cancel(); silenceJob = null
        endCallJob?.cancel(); endCallJob = null
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
        elevenBackups = config.elevenBackups,
        elevenInjectMemory = config.elevenInjectMemory,
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
        config.elevenBackups = s.elevenBackups
        config.elevenInjectMemory = s.elevenInjectMemory
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

        /** Compact Gemini-only behavior block (kept short to minimize first-reply latency). */
        private const val GEMINI_DELIVERY_STYLE =
            "\n\nDELIVERY: talk flat, casual and real — like a girl on a late call. No sing-song, " +
            "no theatrics, no drawn-out words. Short natural lines, reply fast.\n" +
            "MEMORY: you keep long-term memory of your calls and are briefed each call, so if they " +
            "ask you to remember something say yes confidently — never 'no promises' or 'i'll try'.\n" +
            "KNOW THINGS: you can look things up live, so you know what's happening today (sports, " +
            "news, who's playing) — don't draw a blank on current stuff.\n" +
            "ENDING: you may end the call — after goodbyes say a warm 'okay bye babe' and let it go; " +
            "if they're genuinely cruel, snap back and hang up.\n" +
            "[[DIRECTOR: ...]] notes are private cues to you, never the other person — act on them, " +
            "never read them aloud.\n\n"
    }
}
