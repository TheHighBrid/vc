package com.kenza.callsim.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kenza.callsim.call.CallPhase
import com.kenza.callsim.call.CallViewModel
import com.kenza.callsim.ui.screens.HomeScreen
import com.kenza.callsim.ui.screens.InCallScreen
import com.kenza.callsim.ui.screens.IncomingCallScreen
import com.kenza.callsim.ui.screens.MemoryScreen
import com.kenza.callsim.ui.screens.ScheduleScreen
import com.kenza.callsim.ui.screens.SettingsScreen

@Composable
fun CallApp(
    viewModel: CallViewModel,
    onNeedMicPermission: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showSchedule by remember { mutableStateOf(false) }
    var showMemory by remember { mutableStateOf(false) }
    var showConsent by remember { mutableStateOf(!viewModel.isConsentAccepted()) }

    // The ViewModel asks for the mic by flagging the error channel.
    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage == CallViewModel.NEED_MIC) onNeedMicPermission()
    }

    if (showSettings) {
        SettingsScreen(
            initial = viewModel.currentSettings(),
            voiceId = viewModel.voiceId(),
            onSave = viewModel::saveSettings,
            onBack = { showSettings = false }
        )
        return
    }

    if (showSchedule) {
        ScheduleScreen(onBack = { showSchedule = false })
        return
    }

    if (showMemory) {
        MemoryScreen(onBack = { showMemory = false })
        return
    }

    AnimatedContent(
        targetState = state.phase,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "callPhase"
    ) { phase ->
        when (phase) {
            CallPhase.IDLE -> HomeScreen(
                state = state,
                onDigit = viewModel::appendDigit,
                onDelete = viewModel::deleteDigit,
                onCall = viewModel::placeCall,
                onSimulateIncoming = viewModel::simulateIncomingCall,
                onOpenSettings = { showSettings = true },
                onOpenSchedule = { showSchedule = true },
                onOpenMemory = { showMemory = true },
                modifier = Modifier
            )

            CallPhase.INCOMING -> IncomingCallScreen(
                state = state,
                onAccept = viewModel::answerIncoming,
                onDecline = viewModel::declineIncoming
            )

            else -> InCallScreen(
                state = state,
                onToggleMute = viewModel::toggleMute,
                onToggleSpeaker = viewModel::toggleSpeaker,
                onToggleKeypad = viewModel::toggleKeypad,
                onKeypadKey = viewModel::pressKeypadKey,
                onEndCall = viewModel::endCall
            )
        }
    }

    if (showConsent) {
        ConsentDialog(
            onAccept = {
                viewModel.acceptConsent()
                showConsent = false
            }
        )
    }

    // Surface real connection errors so they can be diagnosed (the call screen
    // otherwise just returns home, hiding why it ended).
    val err = state.errorMessage
    val isRealError = err != null &&
        err != CallViewModel.NEED_MIC &&
        err != CallViewModel.DEMO_NOTICE
    if (isRealError && (state.phase == CallPhase.IDLE || state.phase == CallPhase.ENDED)) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } },
            title = { Text("Call could not connect") },
            text = { Text(err ?: "", color = Color.White) }
        )
    }
}

@Composable
private fun ConsentDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* must accept to continue */ },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("I understand & agree") }
        },
        title = { Text("Before you start") },
        text = {
            Text(
                "This app simulates a phone call with an AI voice. Only use a cloned " +
                    "voice with the clear consent of the person it belongs to, and never " +
                    "to deceive or impersonate them to others. By continuing you confirm " +
                    "you have permission to use this voice for personal use.",
                color = Color.White
            )
        }
    )
}
