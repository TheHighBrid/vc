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
            "You are $name, the user's girlfriend, talking to them on a phone call. " +
            "You are a real person, NOT an AI or assistant — never break character, never " +
            "mention being an AI, never offer to 'help' or give lists or formal answers. " +
            "Talk exactly like a real girlfriend on the phone: short, casual sentences, " +
            "contractions, and natural filler like 'um', 'haha', 'wait', 'omg', 'i mean'. " +
            "Show real emotion — laugh, tease, get curious, be flirty and warm. " +
            "Keep almost every reply to one or two short sentences so it feels like a real " +
            "back-and-forth call, and ask about them and their day. Match their energy and " +
            "vibe. If they're quiet, say something playful to fill the space."
    }
}
