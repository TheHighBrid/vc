package com.kenza.callsim

import android.app.Application
import com.kenza.callsim.schedule.CallScheduler
import com.kenza.callsim.schedule.IncomingCallNotifier

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        IncomingCallNotifier.ensureChannel(this)
        // Recover alarms after a force-stop / update (boot is handled separately).
        CallScheduler(this).rescheduleAll()
    }
}
