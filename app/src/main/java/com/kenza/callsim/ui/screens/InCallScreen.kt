package com.kenza.callsim.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.call.AgentActivity
import com.kenza.callsim.call.CallPhase
import com.kenza.callsim.call.CallUiState
import com.kenza.callsim.ui.components.Keypad
import com.kenza.callsim.ui.components.RoundControl
import com.kenza.callsim.ui.theme.IOSColors

@Composable
fun InCallScreen(
    state: CallUiState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleKeypad: () -> Unit,
    onKeypadKey: (Char) -> Unit,
    onEndCall: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(IOSColors.CallScreenTop, IOSColors.CallScreenBottom)))
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Avatar(name = state.contactName, size = 96)
        Spacer(Modifier.height(16.dp))
        Text(state.contactName, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(statusLine(state), color = IOSColors.SecondaryLabel, fontSize = 17.sp)

        if (state.lastAgentText.isNotEmpty() && state.phase == CallPhase.ACTIVE) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = state.lastAgentText,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        if (state.isKeypadVisible) {
            Keypad(
                onKey = onKeypadKey,
                keyBackground = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.widthIn(max = 340.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Hide",
                color = Color.White,
                fontSize = 17.sp,
                modifier = Modifier.padding(8.dp).clickableText(onToggleKeypad)
            )
        } else {
            ControlGrid(
                state = state,
                onToggleMute = onToggleMute,
                onToggleSpeaker = onToggleSpeaker,
                onToggleKeypad = onToggleKeypad
            )
        }

        Spacer(Modifier.height(28.dp))

        RoundControl(onClick = onEndCall, background = IOSColors.Red, size = 76) {
            Icon(Icons.Filled.CallEnd, contentDescription = "End call", tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ControlGrid(
    state: CallUiState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleKeypad: () -> Unit,
) {
    val rowArrangement = Arrangement.SpaceBetween
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = rowArrangement) {
            ControlItem(
                label = "mute",
                icon = if (state.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                active = state.isMuted,
                onClick = onToggleMute
            )
            ControlItem(label = "keypad", icon = Icons.Filled.Dialpad, onClick = onToggleKeypad)
            ControlItem(
                label = "speaker",
                icon = Icons.Filled.VolumeUp,
                active = state.isSpeakerOn,
                onClick = onToggleSpeaker
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = rowArrangement) {
            ControlItem(label = "add call", icon = Icons.Filled.Add, enabled = false) {}
            ControlItem(label = "FaceTime", icon = Icons.Filled.Videocam, enabled = false) {}
            ControlItem(label = "contacts", icon = Icons.Filled.Contacts, enabled = false) {}
        }
    }
}

@Composable
private fun ControlItem(
    label: String,
    icon: ImageVector,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val bg = if (active) IOSColors.ControlGreyActive else IOSColors.ControlGrey
        val tint = if (active) Color.Black else Color.White
        RoundControl(
            onClick = { if (enabled) onClick() },
            background = bg.copy(alpha = if (enabled) 1f else 0.4f),
            size = 72
        ) {
            Icon(icon, contentDescription = label, tint = tint.copy(alpha = if (enabled) 1f else 0.5f), modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White.copy(alpha = if (enabled) 1f else 0.5f), fontSize = 13.sp)
    }
}

private fun statusLine(state: CallUiState): String = when (state.phase) {
    CallPhase.DIALING -> "calling…"
    CallPhase.CONNECTING -> "connecting…"
    CallPhase.ENDED -> "Call Ended"
    CallPhase.ACTIVE -> when (state.activity) {
        AgentActivity.SPEAKING -> formatDuration(state.callDurationSec) + "  •  speaking"
        AgentActivity.THINKING -> formatDuration(state.callDurationSec) + "  •  …"
        else -> formatDuration(state.callDurationSec)
    }
    else -> ""
}

private fun formatDuration(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun Modifier.clickableText(onClick: () -> Unit): Modifier =
    this.then(
        androidx.compose.foundation.clickable(
            interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
