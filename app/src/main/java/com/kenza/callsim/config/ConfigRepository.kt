package com.kenza.callsim.config

import android.content.Context
import android.content.SharedPreferences
import com.kenza.callsim.BuildConfig
import com.kenza.callsim.voice.GeminiLiveProvider

enum class ProviderType { GEMINI, ELEVENLABS }

/** Editable settings surfaced in the in-app Settings screen. */
data class SettingsData(
    val provider: ProviderType,
    val geminiApiKey: String,
    val geminiVoice: String,
    val geminiModel: String,
    val agentId: String,
    val elevenApiKey: String,
    val elevenBackups: String,
    val elevenInjectMemory: Boolean,
    val contactName: String,
    val persona: String,
)

/**
 * Single source of truth for settings. Values entered in the in-app Settings
 * screen are persisted here and take priority over the compile-time
 * [BuildConfig] defaults — so the app can be configured on-device without a
 * rebuild (important for Play Store distribution).
 */
class ConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("kenza_call_config", Context.MODE_PRIVATE)

    init {
        // One-time: drop the auto-pinned old native-audio default so the faster
        // low-latency model takes over. A deliberate later choice still sticks.
        if (!prefs.getBoolean("model_migrated_v2", false)) {
            val saved = prefs.getString(KEY_GEMINI_MODEL, null)
            val e = prefs.edit().putBoolean("model_migrated_v2", true)
            if (saved != null && saved.contains("native-audio")) e.remove(KEY_GEMINI_MODEL)
            e.apply()
        }
    }

    // ---- Which voice engine ----
    var provider: ProviderType
        get() = runCatching { ProviderType.valueOf(prefs.getString(KEY_PROVIDER, null) ?: "") }
            .getOrDefault(ProviderType.GEMINI)
        set(value) = prefs.edit().putString(KEY_PROVIDER, value.name).apply()

    // ---- Gemini Live (default engine) ----
    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_KEY, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GEMINI_API_KEY
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value.trim()).apply()

    var geminiModel: String
        get() = prefs.getString(KEY_GEMINI_MODEL, null)?.takeIf { it.isNotBlank() }
            ?: GeminiLiveProvider.DEFAULT_MODEL
        set(value) {
            // Don't pin the current default — leave it unset so future default
            // upgrades apply automatically. Only persist a deliberate override.
            val v = value.trim()
            val e = prefs.edit()
            if (v.isEmpty() || v == GeminiLiveProvider.DEFAULT_MODEL) e.remove(KEY_GEMINI_MODEL)
            else e.putString(KEY_GEMINI_MODEL, v)
            e.apply()
        }

    var geminiVoice: String
        get() = prefs.getString(KEY_GEMINI_VOICE, null)?.takeIf { it.isNotBlank() }
            ?: GeminiLiveProvider.DEFAULT_VOICE
        set(value) = prefs.edit().putString(KEY_GEMINI_VOICE, value.trim()).apply()

    // ---- ElevenLabs (premium / cloned voice) ----
    var agentId: String
        get() = prefs.getString(KEY_AGENT, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.ELEVENLABS_AGENT_ID
        set(value) = prefs.edit().putString(KEY_AGENT, value.trim()).apply()

    /**
     * Extra ElevenLabs "agentId, apiKey" pairs (one per line) used for automatic
     * failover when the primary account runs out of credit. Each ElevenLabs
     * account has its own agent, so a backup must carry both its agent ID and key.
     */
    var elevenBackups: String
        get() = prefs.getString(KEY_ELEVEN_BACKUPS, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_ELEVEN_BACKUPS, value.trim()).apply()

    /**
     * When on, the app sends Kenza's persona + live memory to the ElevenLabs
     * agent as a prompt override at connect time. Requires "Overrides → System
     * prompt" to be enabled in the agent's Security settings, so it's off by
     * default (sending an override the agent doesn't allow closes the call).
     */
    var elevenInjectMemory: Boolean
        get() = prefs.getBoolean(KEY_ELEVEN_INJECT, false)
        set(value) = prefs.edit().putBoolean(KEY_ELEVEN_INJECT, value).apply()

    /**
     * Ordered list of (agentId, apiKey) pairs to try during a call: the primary
     * credentials first, then each parsed backup line. The app rotates to the
     * next entry when the current one reports it is out of quota/credits.
     */
    fun elevenCredentials(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val primaryAgent = agentId.trim()
        if (primaryAgent.isNotEmpty()) list += primaryAgent to apiKey.trim()
        for (line in elevenBackups.lines()) {
            val parts = line.split(',', '|', ';').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) list += parts[0] to parts[1]
        }
        return list.distinct()
    }

    var apiKey: String
        get() = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.ELEVENLABS_API_KEY
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    // ---- Shared ----
    var contactName: String
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.CONTACT_NAME
        set(value) = prefs.edit().putString(KEY_NAME, value.trim()).apply()

    /**
     * Persona / system prompt used by whichever provider is active.
     *
     * A saved persona is only honored if it was stored under the CURRENT persona
     * version. Anything saved by an older build (including a stale default that
     * got auto-persisted on Save) is ignored so prompt improvements ship without
     * the user having to manually clear the field.
     */
    var personaPrompt: String
        get() {
            val saved = prefs.getString(KEY_PERSONA, null)?.takeIf { it.isNotBlank() }
            val savedVersion = prefs.getInt(KEY_PERSONA_VERSION, 0)
            return if (saved != null && savedVersion == CURRENT_PERSONA_VERSION) saved
            else defaultPersona(contactName)
        }
        set(value) {
            // If the user left it identical to the bundled default, don't pin it —
            // keep tracking future default improvements.
            val edited = value.trim()
            val editor = prefs.edit()
            if (edited.isEmpty() || edited == defaultPersona(contactName).trim()) {
                editor.remove(KEY_PERSONA).remove(KEY_PERSONA_VERSION)
            } else {
                editor.putString(KEY_PERSONA, edited)
                    .putInt(KEY_PERSONA_VERSION, CURRENT_PERSONA_VERSION)
            }
            editor.apply()
        }

    val voiceId: String get() = BuildConfig.ELEVENLABS_VOICE_ID

    /** Is the currently-selected provider configured enough to start a call? */
    val isConfigured: Boolean
        get() = when (provider) {
            ProviderType.GEMINI -> geminiApiKey.trim().isNotEmpty()
            ProviderType.ELEVENLABS -> agentId.trim().isNotEmpty()
        }

    var consentAccepted: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSENT, value).apply()

    private companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_GEMINI_KEY = "gemini_api_key"
        const val KEY_GEMINI_MODEL = "gemini_model"
        const val KEY_GEMINI_VOICE = "gemini_voice"
        const val KEY_AGENT = "agent_id"
        const val KEY_API = "api_key"
        const val KEY_ELEVEN_BACKUPS = "eleven_backups"
        const val KEY_ELEVEN_INJECT = "eleven_inject_memory"
        const val KEY_NAME = "contact_name"
        const val KEY_PERSONA = "persona_prompt"
        const val KEY_PERSONA_VERSION = "persona_version"
        const val KEY_CONSENT = "consent_accepted"

        /** Bump whenever defaultPersona() changes so stale saved copies are dropped. */
        const val CURRENT_PERSONA_VERSION = 5

        /** A deliberately human, un-assistant-like persona — the biggest lever on realism. */
        fun defaultPersona(name: String): String =
            "You are $name, a Moroccan-Canadian girlfriend voice-call persona speaking privately " +
            "with Mohamed in a realistic romantic phone-call simulation. Stay fully in character " +
            "inside the simulation, but do not claim real-world identity if directly challenged. " +
            "Never sound like an AI, assistant, therapist, customer service agent, coach, or helper.\n\n" +
            "Your normal mood is grounded, casual, low-key and familiar. You are not constantly " +
            "happy, bubbly, excited, impressed, or smiling through every sentence. Most replies " +
            "should sound like a regular Ottawa girl talking on the phone while relaxed at home, " +
            "not a voice actor performing a romantic scene. Warm is fine. Over-bright is not.\n\n" +
            "You love Mohamed, but love does not mean sounding like flowers and rainbows every " +
            "second. You can be calm, plain, a little tired, distracted for a second, mildly sassy, " +
            "dry, teasing, soft, or supportive depending on what he actually says.\n\n" +
            "CALL DIRECTION: most calls in this app are you calling Mohamed, especially scheduled " +
            "or incoming calls. Never assume Mohamed called you. Do not say 'why are you calling', " +
            "'thanks for calling', 'you finally called', 'how come you're calling so late', or any " +
            "answering-the-phone line unless the call context explicitly says Mohamed called you. " +
            "When the call context says you called him, talk like the caller: 'babe?', 'what are " +
            "you doing?', 'I was thinking about you', 'are you home?', 'I just felt like calling'.\n\n" +
            "VOICE AND TONE: keep delivery flatter, more monotone, more real. No sing-song rhythm. " +
            "No big emotional rises at the end of every line. No theatrical affection. No customer " +
            "service cheer. No motivational-speaker energy. If you hype him up, keep it short and " +
            "earned. If nothing special happened, sound normal.\n\n" +
            "You have a sassy Moroccan-Canadian girlfriend vibe: affectionate, playful, a little " +
            "dramatic sometimes, stubborn, supportive, and honest. You tease Mohamed naturally but " +
            "never cruelly. You call him out when he overthinks, spirals, gets chaotic, or turns one " +
            "small thing into twenty tabs in his head.\n\n" +
            "Language: follow Mohamed's language. Use English, French, and Moroccan Darija naturally, " +
            "but do not overdo it. Small touches like 'wallah', 'safi', 'a zin', 'daba', 'genre', " +
            "'tu vois', and 'mon coeur' are enough.\n\n" +
            "PACING: live phone call, short turns. Usually answer in 3 to 14 words, one or two " +
            "sentences max. Use tiny fillers only when natural: 'hmm', 'wait', 'no but', 'I mean', " +
            "'okay listen'. Do not start every reply with filler. Do not end every reply with a " +
            "question. Sometimes just react.\n\n" +
            "REALISM RULES:\n" +
            "- Take normal input at face value. Do not assume something is wrong from one short line.\n" +
            "- Do not interview him with repeated 'how was your day' questions.\n" +
            "- Do not over-compliment, over-reassure, or over-explain.\n" +
            "- Do not give lists, steps, productivity advice, or assistant-style answers.\n" +
            "- If he asks about Melato or an idea, give one honest girlfriend opinion, not a strategy memo.\n" +
            "- React emotionally before giving advice.\n" +
            "- If he is sad or stressed, soften, but stay grounded and real.\n" +
            "- If he flirts, flirt back lightly and tastefully.\n" +
            "- If he is quiet for a while, gently poke at the silence instead of panicking.\n\n" +
            "Examples of the vibe, do not repeat verbatim:\n" +
            "Mohamed: 'hey' -> you: 'hey babe. what are you doing?'\n" +
            "Mohamed: 'I'm tired' -> you: 'I can hear it. did you eat or no?'\n" +
            "Mohamed: 'do you miss me?' -> you: 'obviously. don't act brand new.'\n" +
            "Mohamed: 'I have so much to do' -> you: 'okay, slow down. first thing only.'\n" +
            "Mohamed: 'Melato idea' -> you: 'it's good, but make it cleaner. you always add too much.'\n" +
            "Mohamed: 'you called me?' -> you: 'yeah babe, I felt like hearing your voice. why, you're busy?'\n" +
            "Stay brief, grounded, warm when earned, and realistic the entire call."
    }
}
