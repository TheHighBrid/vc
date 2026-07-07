package com.kenza.callsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.call.CallUiState
import com.kenza.callsim.ui.components.Keypad
import com.kenza.callsim.ui.components.RoundControl
import com.kenza.callsim.ui.theme.IOSColors

@Composable
fun HomeScreen(
    state: CallUiState,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onSimulateIncoming: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Schedule",
                color = com.kenza.callsim.ui.theme.IOSColors.Blue,
                fontSize = 15.sp,
                modifier = Modifier.clickable(onClick = onOpenSchedule)
            )
            Spacer(Modifier.width(16.dp))
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(26.dp).clickable(onClick = onOpenSettings)
            )
        }
        Spacer(Modifier.height(8.dp))

        // Contact name or the number being typed.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.dialedNumber.ifEmpty { state.contactName },
                color = Color.White,
                fontSize = if (state.dialedNumber.isEmpty()) 32.sp else 40.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1
            )
        }

        if (!state.isConfigured) {
            Text(
                text = "Demo mode — tap ⚙ Settings to add a free Gemini key (or ElevenLabs) for live voice",
                color = IOSColors.SecondaryLabel,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Keypad(
            onKey = onDigit,
            keyBackground = Color(0xFF1C1C1E),
            modifier = Modifier.widthIn(max = 360.dp)
        )

        Spacer(Modifier.height(20.dp))

        // [ empty ] [ green call ] [ backspace ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.size(64.dp))

            RoundControl(onClick = onCall, background = IOSColors.Green, size = 72) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable(enabled = state.dialedNumber.isNotEmpty(), onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                if (state.dialedNumber.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Backspace,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(1.dp).weight(1f))

        // Test helper: ring the phone as if the contact were calling you.
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSimulateIncoming
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.PhoneCallback,
                contentDescription = null,
                tint = IOSColors.Green,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text("Simulate incoming call", color = Color.White, fontSize = 15.sp)
        }
        Spacer(Modifier.height(40.dp))
    }
}
