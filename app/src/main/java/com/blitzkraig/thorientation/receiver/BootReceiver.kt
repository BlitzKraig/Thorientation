package com.blitzkraig.thorientation.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.blitzkraig.thorientation.engine.RotationEngine
import com.blitzkraig.thorientation.service.PersistentNotificationHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (!isStartAtBootEnabled(context)) {
                Log.i(TAG, "Device booted / package updated, but start-at-boot is disabled; skipping")
                return
            }

            Log.i(TAG, "Device booted / package updated, re-applying saved screen orientations")
            val engine = RotationEngine.get(context)
            val displays = engine.getDisplays()
            for (display in displays) {
                val saved = engine.getSavedOrientation(display.displayId)
                if (saved != null) {
                    engine.setOrientation(display.displayId, saved)
                }
            }
            if (PersistentNotificationHelper.isEnabled(context)) {
                PersistentNotificationHelper.startOrUpdate(context)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "thorientation_prefs"
        const val KEY_START_AT_BOOT = "start_at_boot"

        fun isStartAtBootEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_START_AT_BOOT, true)
        }

        fun setStartAtBootEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_START_AT_BOOT, enabled).apply()

            try {
                val component = ComponentName(context, BootReceiver::class.java)
                val state = if (enabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                context.packageManager.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to update BootReceiver component state", e)
            }
        }
    }
}

