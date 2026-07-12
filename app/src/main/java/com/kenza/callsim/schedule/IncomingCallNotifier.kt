package com.kenza.callsim.schedule

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
    const val NOTIF_ID = 4711

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
            // The foreground service plays the ringtone itself, so the channel
            // stays silent to avoid a double ring.
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    /** The ringing-call notification that carries the full-screen intent. */
    fun build(context: Context, contactName: String): Notification {
        ensureChannel(context)

        val fullScreen = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_INCOMING_CALL, true)
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getActivity(context, 1001, fullScreen, piFlags)

        return NotificationCompat.Builder(context, CHANNEL_ID)
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
    }

    fun cancel(context: Context) =
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
}
