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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.call.CallUiState
import com.kenza.callsim.ui.components.RoundControl
import com.kenza.callsim.ui.theme.IOSColors

@Composable
fun IncomingCallScreen(
    state: CallUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2A2A2C), Color(0xFF0A0A0A)))
            )
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        Avatar(name = state.contactName)
        Spacer(Modifier.height(20.dp))
        Text(state.contactName, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("calling you…", color = IOSColors.SecondaryLabel, fontSize = 18.sp)

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 56.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CallAction(label = "Decline", color = IOSColors.Red, onClick = onDecline) {
                Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            CallAction(label = "Accept", color = IOSColors.Green, onClick = onAccept) {
                Icon(Icons.Filled.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun CallAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RoundControl(onClick = onClick, background = color, size = 76, content = icon)
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
fun Avatar(name: String, size: Int = 112) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF8E8E93), Color(0xFF48484A)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = (size / 2.2).sp,
            fontWeight = FontWeight.Medium
        )
    }
}
