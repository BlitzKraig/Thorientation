package com.blitzkraig.thorientation.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.blitzkraig.thorientation.engine.RotationEngine

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("BootReceiver", "Device booted / package updated, re-applying saved screen orientations")
            val engine = RotationEngine.get(context)
            val displays = engine.getDisplays()
            for (display in displays) {
                val saved = engine.getSavedOrientation(display.displayId)
                if (saved != null) {
                    engine.setOrientation(display.displayId, saved)
                }
            }
            if (com.blitzkraig.thorientation.service.PersistentNotificationHelper.isEnabled(context)) {
                com.blitzkraig.thorientation.service.PersistentNotificationHelper.startOrUpdate(context)
            }
        }
    }
}
