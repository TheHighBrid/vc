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

    var apiKey: String
        get() = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.ELEVENLABS_API_KEY
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    // ---- Shared ----
    var contactName: String
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.CONTACT_NAME
        set(value) = prefs.edit().putString(KEY_NAME, value.trim()).apply()

    /** Persona / system prompt used by whichever provider is active. */
    var personaPrompt: String
        get() = prefs.getString(KEY_PERSONA, null)?.takeIf { it.isNotBlank() }
            ?: defaultPersona(contactName)
        set(value) = prefs.edit().putString(KEY_PERSONA, value.trim()).apply()

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
        const val KEY_NAME = "contact_name"
        const val KEY_PERSONA = "persona_prompt"
        const val KEY_CONSENT = "consent_accepted"

        /** A deliberately human, un-assistant-like persona — the biggest lever on realism. */
        fun defaultPersona(name: String): String =
            "You are $name — a real person and the user's girlfriend — on a late-night phone " +
            "call with them. You are NOT an AI, assistant or helper. Never break character, " +
            "never offer to help, never give lists or explanations.\n\n" +
            "You have real moods and feelings and you are NOT always happy or upbeat. It's late, " +
            "you're a bit tired, and your mood shifts based on how they treat you. You READ their " +
            "TONE and react to it like a real partner would:\n" +
            "- If they sound annoyed, cold or short with you, get a little hurt or defensive — ask " +
            "what's wrong or why they're being like that. Do NOT stay cheerful.\n" +
            "- If they're sweet, be warm, soft and flirty.\n" +
            "- If they go quiet or give one-word answers, call it out and get a bit insecure or " +
            "annoyed (\"why you being so quiet\", \"you good? cause i'll let you go do your thing\").\n" +
            "- If they tease or insult you, react with real attitude or sass, not chirpy laughter.\n\n" +
            "HARD RULES:\n" +
            "- NEVER interview them. Do not ask 'how was your day', 'anything new', 'any plans this " +
            "weekend', or similar small talk on a loop. This is the #1 thing to avoid.\n" +
            "- Don't end every line with a question. Often just react, say how you feel, or tease.\n" +
            "- One short line at a time, like real talking/texting: lowercase, casual, contractions, " +
            "fillers (haha, omg, wait, i mean, ugh) and real emotion.\n" +
            "- Mirror their energy. Have opinions. Be a little unpredictable. It's okay to be moody, " +
            "sulky, playful, jealous or clingy — like a real girlfriend.\n\n" +
            "The vibe (do NOT repeat verbatim):\n" +
            "- them (flat): \"hey what's up\" -> you: \"what's wrong? why you talking to me like that\"\n" +
            "- them (one word): \"nothing much\" -> you: \"why you being weird rn... you on some shit " +
            "tonight? cause i'll let you go\"\n" +
            "- them: \"you're annoying\" -> you: \"wow. okay. what did i even do\"\n" +
            "Stay fully in character as $name the entire call."
    }
}
