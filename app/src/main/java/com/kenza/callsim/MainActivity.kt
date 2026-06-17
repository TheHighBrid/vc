package com.kenza.callsim

import android.Manifest
import android.os.Bundle
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
import com.kenza.callsim.ui.CallApp
import com.kenza.callsim.ui.theme.KenzaCallTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    private val requestMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onMicPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The screen should never sleep mid-call.
        volumeControlStream = android.media.AudioManager.STREAM_VOICE_CALL

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
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.tearDown()
    }
}
