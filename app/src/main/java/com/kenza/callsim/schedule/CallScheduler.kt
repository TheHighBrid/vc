package com.kenza.callsim.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Bridges [ScheduledCall] rules to the OS [AlarmManager]. Sets exact wake-up
 * alarms (falling back to inexact when the exact-alarm permission is missing),
 * reschedules recurring rules after each fire, and restores everything on boot.
 */
class CallScheduler(private val context: Context) {

    private val app = context.applicationContext
    private val store = ScheduleStore(app)
    private val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedules(): List<ScheduledCall> = store.all()

    fun add(call: ScheduledCall) {
        store.upsert(call)
        if (call.enabled) scheduleNext(call)
    }

    fun remove(id: String) {
        cancelAlarm(id)
        store.remove(id)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val updated = (store.get(id) ?: return).copy(enabled = enabled)
        store.upsert(updated)
        if (enabled) scheduleNext(updated) else cancelAlarm(id)
    }

    /** Re-arm every enabled rule (called on boot and app start). */
    fun rescheduleAll() = store.all().forEach { if (it.enabled) scheduleNext(it) }

    /** After an alarm fires: disable one-shots, roll recurring rules forward. */
    fun onFired(id: String) {
        val call = store.get(id) ?: return
        if (call.kind == ScheduleKind.ONE_SHOT) store.upsert(call.copy(enabled = false))
        else if (call.enabled) scheduleNext(call)
    }

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true

    private fun scheduleNext(call: ScheduledCall) {
        val at = call.nextTriggerAfter(System.currentTimeMillis()) ?: return
        val pi = alarmIntent(call.id)
        try {
            if (canScheduleExact()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            Log.w(TAG, "exact alarm denied, using inexact: ${e.message}")
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
        Log.i(TAG, "scheduled ${call.id} at $at")
    }

    private fun cancelAlarm(id: String) = am.cancel(alarmIntent(id))

    private fun alarmIntent(id: String): PendingIntent {
        val intent = Intent(app, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_FIRE
            putExtra(ScheduleReceiver.EXTRA_ID, id)
            // Distinct data per id so PendingIntents don't collapse into one.
            data = Uri.parse("kenzacall://schedule/$id")
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(app, id.hashCode(), intent, flags)
    }

    companion object {
        private const val TAG = "CallScheduler"

        /** Intent to send the user to the exact-alarm settings page (Android 12+). */
        fun exactAlarmSettingsIntent(): Intent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            else null
    }
}
