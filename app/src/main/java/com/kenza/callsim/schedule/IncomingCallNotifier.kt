package com.kenza.callsim.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kenza.callsim.MainActivity
import com.kenza.callsim.R

/**
 * Posts the high-priority, full-screen-intent notification that turns a fired
 * alarm into a ringing incoming-call screen — even over the lock screen with
 * the display off. The full-screen intent launches [MainActivity], which shows
 * the incoming-call UI and dismisses the keyguard.
 */
object IncomingCallNotifier {

    const val CHANNEL_ID = "incoming_calls"
    private const val NOTIF_ID = 4711
    private const val TAG = "IncomingCallNotifier"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Incoming calls", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled calls from Kenza"
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    fun show(context: Context, contactName: String) {
        ensureChannel(context)

        val fullScreen = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_INCOMING_CALL, true)
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getActivity(context, 1001, fullScreen, piFlags)

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(contactName)
            .setContentText("Incoming call…")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted (Android 13+) — nothing we can do here.
            Log.w(TAG, "notify blocked: ${e.message}")
        }
    }

    fun cancel(context: Context) =
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
}
