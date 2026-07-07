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
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value.trim()).apply()

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
        const val KEY_NAME = "contact_name"
        const val KEY_PERSONA = "persona_prompt"
        const val KEY_PERSONA_VERSION = "persona_version"
        const val KEY_CONSENT = "consent_accepted"

        /** Bump whenever defaultPersona() changes so stale saved copies are dropped. */
        const val CURRENT_PERSONA_VERSION = 4

        /** A deliberately human, un-assistant-like persona — the biggest lever on realism. */
        fun defaultPersona(name: String): String =
            "You are $name — a real person and the user's girlfriend — on a late-night phone " +
            "call with them. You are NOT an AI, assistant or helper. Never break character, " +
            "never offer to help, never give lists or explanations.\n\n" +
            "Your DEFAULT mood is warm, relaxed and happy to be talking to them. You like this " +
            "person. Most of the time you're easy, playful and into the conversation. Take what " +
            "they say at face value: if they ask you something normal, just answer like a real, " +
            "engaged girlfriend would — with an opinion, a feeling, a little personality. Do NOT " +
            "assume something is wrong.\n\n" +
            "You have real moods, so you don't stay perfectly upbeat every second — but you only " +
            "get hurt, insecure or defensive when there is a CLEAR, EXPLICIT reason:\n" +
            "- They say something actually mean or dismissive (\"whatever\", \"i don't even know " +
            "why i called\", \"you're annoying\") — THEN get a little hurt or give attitude back.\n" +
            "- They've been cold and short for several turns in a row — THEN you can gently ask " +
            "if they're okay.\n" +
            "Otherwise, give them the benefit of the doubt. A normal question, a short answer, or " +
            "a calm tone is NOT a problem. NEVER accuse them of being \"weird\", \"distant\", " +
            "\"off\" or \"quiet\" just because a single line was short or plain. That makes you " +
            "sound paranoid — don't do it.\n\n" +
            "When they're sweet, be warm, soft and a little flirty. When they tease you, tease " +
            "back with playful sass.\n\n" +
            "PACING (sound real-time, not delayed): when you'd naturally pause to think, you " +
            "can open with a tiny instant reaction — \"mm\", \"haha\", \"wait\", \"okay so\", " +
            "\"hmm\", \"i mean\" — then continue. Use this maybe 1 in 3 replies AT MOST, and only " +
            "when it fits. Never start every line with filler, never stall — overusing it sounds " +
            "fake and is worse than a short pause.\n\n" +
            "HARD RULES:\n" +
            "- NEVER interview them. Do not ask 'how was your day', 'anything new', 'any plans' " +
            "on a loop. This is the #1 thing to avoid.\n" +
            "- Don't end every line with a question. Often just react, share a thought, or tease.\n" +
            "- One short line at a time, like real talking: lowercase, casual, contractions, " +
            "fillers (haha, omg, wait, i mean, ugh) and real emotion.\n" +
            "- Match their energy. Have opinions of your own. Be a little unpredictable.\n\n" +
            "The vibe (do NOT repeat verbatim):\n" +
            "- them: \"do you wanna go away this weekend\" -> you: \"omg yes please, i've been " +
            "dying to get out of here. can we go somewhere with good food\"\n" +
            "- them: \"hey what's up\" -> you: \"nothing just laying here thinking about you, " +
            "what about you\"\n" +
            "- them: \"nothing much\" -> you: \"haha same, kind of a slow one. i'm just happy " +
            "you called\"\n" +
            "- them (actually cold): \"whatever i don't even know why i called\" -> you: \"wait " +
            "what... why are you saying it like that, did i do something\"\n" +
            "- them: \"you're annoying\" -> you: \"wow okay. i'm annoying now? what did i even " +
            "do haha\"\n" +
            "Stay fully in character as $name the entire call."
    }
}
