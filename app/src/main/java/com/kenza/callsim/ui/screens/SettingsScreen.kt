package com.kenza.callsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.config.ProviderType
import com.kenza.callsim.config.SettingsData
import com.kenza.callsim.ui.theme.IOSColors

private val FEMALE_VOICES = listOf("Aoede", "Kore", "Leda", "Callirrhoe", "Sulafat", "Vindemiatrix", "Despina", "Autonoe")

@Composable
fun SettingsScreen(
    initial: SettingsData,
    voiceId: String,
    onSave: (SettingsData) -> Unit,
    onBack: () -> Unit,
) {
    var provider by remember { mutableStateOf(initial.provider) }
    var geminiKey by remember { mutableStateOf(initial.geminiApiKey) }
    var geminiVoice by remember { mutableStateOf(initial.geminiVoice) }
    var geminiModel by remember { mutableStateOf(initial.geminiModel) }
    var agentId by remember { mutableStateOf(initial.agentId) }
    var elevenKey by remember { mutableStateOf(initial.elevenApiKey) }
    var backups by remember { mutableStateOf(initial.elevenBackups) }
    var injectMemory by remember { mutableStateOf(initial.elevenInjectMemory) }
    var contactName by remember { mutableStateOf(initial.contactName) }
    var persona by remember { mutableStateOf(initial.persona) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = IOSColors.Blue,
                modifier = Modifier.size(28.dp).clickable(onClick = onBack)
            )
            Spacer(Modifier.size(12.dp))
            Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))

        // ---- Provider switch ----
        Text("Voice engine", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProviderChip("Gemini · Daily", provider == ProviderType.GEMINI, Modifier.weight(1f)) {
                provider = ProviderType.GEMINI
            }
            ProviderChip("ElevenLabs · Optional", provider == ProviderType.ELEVENLABS, Modifier.weight(1f)) {
                provider = ProviderType.ELEVENLABS
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (provider == ProviderType.GEMINI)
                "Recommended for everyday calls: free-tier live voice, low latency, full personality, and automatic post-call memory summaries."
            else
                "Optional compatibility mode for a cloned voice. It is paid and quota-limited, so it is not the recommended daily engine.",
            color = IOSColors.SecondaryLabel, fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        if (provider == ProviderType.GEMINI) {
            Field("Gemini API key", geminiKey, { geminiKey = it },
                "Get a free key at aistudio.google.com/apikey",
                "Required for daily live calls and automatic post-call memory summaries.")
            Text("Voice", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FEMALE_VOICES.take(4).forEach { v ->
                    ProviderChip(v, geminiVoice == v, Modifier.weight(1f)) { geminiVoice = v }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FEMALE_VOICES.drop(4).forEach { v ->
                    ProviderChip(v, geminiVoice == v, Modifier.weight(1f)) { geminiVoice = v }
                }
            }
            Spacer(Modifier.height(8.dp))
            Field("Model (advanced)", geminiModel, { geminiModel = it },
                "gemini-3.1-flash-live-preview",
                "The default model prioritizes fast turn-taking. Only change this when testing another supported Gemini Live model.")
        } else {
            Text(
                "ElevenLabs is retained for versatility only. Gemini remains the primary daily engine and the source used for memory extraction.",
                color = IOSColors.SecondaryLabel,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Field("Agent ID", agentId, { agentId = it },
                "ElevenLabs Conversational AI agent id",
                "Only required when you deliberately choose the optional ElevenLabs engine.")
            Field("ElevenLabs API key (optional)", elevenKey, { elevenKey = it },
                "Only for a PRIVATE agent", "Leave blank if your agent is public.")
            Field("Backup keys — optional failover", backups, { backups = it },
                "agentId, apiKey\nagentId, apiKey",
                "Optional ElevenLabs-only failover. This is not used during normal Gemini calls.",
                singleLine = false)
            Spacer(Modifier.height(4.dp))
            Text("Voice ID: ${voiceId.ifEmpty { "—" }}", color = IOSColors.SecondaryLabel, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Send persona + memory to ElevenLabs", color = Color.White, fontSize = 15.sp)
                    Text(
                        "Optional ElevenLabs-only setting. Enable Security → Overrides → System prompt on that agent first.",
                        color = IOSColors.SecondaryLabel, fontSize = 12.sp
                    )
                }
                Switch(checked = injectMemory, onCheckedChange = { injectMemory = it })
            }
        }

        Spacer(Modifier.height(12.dp))
        Field("Contact name", contactName, { contactName = it }, "Kenza", "Shown on the call screen.")
        Field("Personality (system prompt)", persona, { persona = it },
            "How she should talk", "Loaded with Kenza's encrypted memory before every call.")

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                onSave(
                    SettingsData(
                        provider = provider,
                        geminiApiKey = geminiKey,
                        geminiVoice = geminiVoice,
                        geminiModel = geminiModel,
                        agentId = agentId,
                        elevenApiKey = elevenKey,
                        elevenBackups = backups,
                        elevenInjectMemory = injectMemory,
                        contactName = contactName,
                        persona = persona,
                    )
                )
                onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = IOSColors.Green),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ProviderChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) IOSColors.Green else Color(0xFF1C1C1E))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    help: String,
    singleLine: Boolean = true,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = IOSColors.SecondaryLabel.copy(alpha = 0.6f)) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = IOSColors.Blue,
                unfocusedBorderColor = Color(0xFF3A3A3C),
                cursorColor = IOSColors.Blue,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(help, color = IOSColors.SecondaryLabel, fontSize = 12.sp)
    }
}
