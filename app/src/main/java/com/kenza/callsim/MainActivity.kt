package com.kenza.callsim

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kenza.callsim.call.CallViewModel
import com.kenza.callsim.schedule.IncomingCallNotifier
import com.kenza.callsim.ui.CallApp
import com.kenza.callsim.ui.theme.KenzaCallTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    private val requestMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onMicPermissionResult(granted)
        }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        volumeControlStream = android.media.AudioManager.STREAM_VOICE_CALL

        // Ask for notification permission up front so scheduled calls can ring.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            KenzaCallTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    CallApp(
                        viewModel = viewModel,
                        onNeedMicPermission = { requestMic.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }
            }
        }

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * When launched by a fired schedule (via the full-screen intent), wake and
     * unlock the screen so the ringing incoming-call UI appears over the lock
     * screen, then drive the ViewModel into its INCOMING state.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_INCOMING_CALL, false) != true) return

        showWhenLockedAndTurnScreenOn()
        IncomingCallNotifier.cancel(this)
        viewModel.onScheduledIncomingCall()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
                ?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.tearDown()
    }

    companion object {
        const val EXTRA_INCOMING_CALL = "extra_incoming_call"
    }
}
