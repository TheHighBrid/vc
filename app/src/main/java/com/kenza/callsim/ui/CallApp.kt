package com.kenza.callsim.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kenza.callsim.call.CallPhase
import com.kenza.callsim.call.CallViewModel
import com.kenza.callsim.ui.screens.HomeScreen
import com.kenza.callsim.ui.screens.InCallScreen
import com.kenza.callsim.ui.screens.IncomingCallScreen

@Composable
fun CallApp(
    viewModel: CallViewModel,
    onNeedMicPermission: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The ViewModel asks for the mic by flagging the error channel.
    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage == CallViewModel.NEED_MIC) onNeedMicPermission()
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
}
