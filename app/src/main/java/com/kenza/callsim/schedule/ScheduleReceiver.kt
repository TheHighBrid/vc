package com.kenza.callsim.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kenza.callsim.MainActivity

/**
 * Receives fired schedule alarms (turns them into a ringing incoming call) and
 * re-arms all schedules after a device reboot.
 */
class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_FIRE -> {
                val id = intent.getStringExtra(EXTRA_ID) ?: return
                // 1) Ring + vibrate + wake immediately, and post the full-screen
                //    intent notification (used on locked screens / Android 14+).
                IncomingCallService.start(app)
                // 2) Also try to launch the call screen directly. This is the most
                //    reliable path over the lock screen and works whenever the
                //    "appear on top" permission is granted, even if the full-screen
                //    intent is downgraded to a banner.
                runCatching {
                    app.startActivity(
                        Intent(app, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(MainActivity.EXTRA_INCOMING_CALL, true)
                        }
                    )
                }.onFailure { Log.w("ScheduleReceiver", "direct launch blocked: ${it.message}") }
                CallScheduler(app).onFired(id)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" ->
                CallScheduler(app).rescheduleAll()
        }
    }

    companion object {
        const val ACTION_FIRE = "com.kenza.callsim.SCHEDULE_FIRE"
        const val EXTRA_ID = "id"
    }
}
