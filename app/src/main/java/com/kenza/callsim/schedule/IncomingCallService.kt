package com.kenza.callsim.schedule

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import com.kenza.callsim.call.RingtonePlayer
import com.kenza.callsim.config.ConfigRepository

/**
 * Foreground service that makes a fired schedule feel like a real incoming
 * call: it starts the ringtone + vibration immediately, wakes the screen, and
 * posts the full-screen-intent notification that launches the call UI over the
 * lock screen. Rings until answered/declined or a ~45s missed-call timeout.
 */
class IncomingCallService : Service() {

    private var ringtone: RingtonePlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = ConfigRepository(this).contactName
        val notif = IncomingCallNotifier.build(this, name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                IncomingCallNotifier.NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(IncomingCallNotifier.NOTIF_ID, notif)
        }

        wakeScreen()
        if (ringtone == null) ringtone = RingtonePlayer(this).also { it.start() }

        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, 45_000)
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "kenza:incoming-call"
        ).apply { acquire(5_000) }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        ringtone?.stop(); ringtone = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        IncomingCallNotifier.cancel(this)
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val i = Intent(context, IncomingCallService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, IncomingCallService::class.java))
        }
    }
}
