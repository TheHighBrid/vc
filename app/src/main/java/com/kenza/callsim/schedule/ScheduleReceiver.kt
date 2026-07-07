package com.kenza.callsim.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kenza.callsim.config.ConfigRepository

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
                val name = ConfigRepository(app).contactName
                IncomingCallNotifier.show(app, name)
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
