package com.kenza.callsim.call

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Plays the incoming-call ringtone plus a repeating vibration pattern,
 * mimicking an iPhone ring. Drop a file at res/raw/ringtone.(mp3|ogg) to use a
 * custom tone; otherwise the system default ringtone is used.
 */
class RingtonePlayer(private val context: Context) {

    private var ringtone: Ringtone? = null

    fun start() {
        if (ringtone?.isPlaying == true) return

        val custom = resolveRawRingtone()
        val uri: Uri = custom ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
            // setLooping was added in API 28; on older devices we just play once
            // and rely on the vibration loop for the "ringing" feel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            runCatching { play() }
        }
        vibrate()
    }

    fun stop() {
        runCatching { ringtone?.stop() }
        ringtone = null
        vibrator()?.cancel()
    }

    private fun resolveRawRingtone(): Uri? {
        val id = context.resources.getIdentifier("ringtone", "raw", context.packageName)
        return if (id != 0) Uri.parse("android.resource://${context.packageName}/$id") else null
    }

    private fun vibrate() {
        val v = vibrator() ?: return
        // iPhone-like: buzz, pause, buzz, longer pause, repeat.
        val pattern = longArrayOf(0, 400, 200, 400, 1200)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, 0)
        }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}

/** Short DTMF beeps for keypad presses, both on the home dialer and in-call. */
class DtmfTones {
    private val gen = ToneGenerator(AudioManager.STREAM_DTMF, 70)

    fun play(key: Char) {
        val tone = when (key) {
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '0' -> ToneGenerator.TONE_DTMF_0
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> return
        }
        gen.startTone(tone, 150)
    }

    fun release() = gen.release()
}
