package com.blitzkraig.thorientation

import android.app.Application
import com.blitzkraig.thorientation.engine.RotationEngine

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize RotationEngine on app launch
        RotationEngine.get(this)
        if (com.blitzkraig.thorientation.service.PersistentNotificationHelper.isEnabled(this)) {
            com.blitzkraig.thorientation.service.PersistentNotificationHelper.startOrUpdate(this)
        }
    }
}
