package com.blitzkraig.thorientation.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.blitzkraig.thorientation.engine.RotationEngine

class OrientationAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "OrientationAccessibilityService connected by system; enforcing orientation overlays")
        instance = this

        if (!com.blitzkraig.thorientation.receiver.BootReceiver.isStartAtBootEnabled(this)) {
            Log.i(TAG, "Start at boot is disabled; skipping orientation restore in AccessibilityService")
            return
        }

        // Re-apply saved orientations to ensure top and bottom screens stay in sync
        val engine = RotationEngine.get(this)
        val displays = engine.getDisplays()
        for (display in displays) {
            val saved = engine.getSavedOrientation(display.displayId)
            if (saved != null) {
                engine.setOrientation(display.displayId, saved)
            }
        }

        if (PersistentNotificationHelper.isEnabled(this)) {
            PersistentNotificationHelper.startOrUpdate(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No events monitored, 0 CPU / battery overhead
    }

    override fun onInterrupt() {
        Log.w(TAG, "OrientationAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "OrientationA11yService"
        var instance: OrientationAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun isEnabledInSettings(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${OrientationAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }
}
