package com.blitzkraig.thorientation

import android.app.Application
import com.blitzkraig.thorientation.engine.RotationEngine

import com.blitzkraig.thorientation.receiver.BootReceiver
import com.blitzkraig.thorientation.service.PersistentNotificationHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize RotationEngine on app launch
        RotationEngine.get(this)
        if (BootReceiver.isStartAtBootEnabled(this) && PersistentNotificationHelper.isEnabled(this)) {
            PersistentNotificationHelper.startOrUpdate(this)
        }
    }
}
