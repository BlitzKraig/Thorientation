package com.blitzkraig.thorientation.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
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

        val engine = RotationEngine.get(this)
        if (!engine.isMasterEnabled()) {
            Log.i(TAG, "Master switch is disabled; skipping orientation restore in AccessibilityService")
            return
        }

        // Re-apply saved orientations to ensure top and bottom screens stay in sync
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

        fun disable(): Boolean {
            val service = instance ?: return false
            return try {
                service.disableSelf()
                instance = null
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to disableSelf()", e)
                false
            }
        }

        fun isEnabledInSettings(context: Context): Boolean {
            if (isRunning()) {
                return true
            }

            val expectedComponentName = ComponentName(context, OrientationAccessibilityService::class.java)
            val expectedServiceName = "${context.packageName}/${OrientationAccessibilityService::class.java.canonicalName}"
            val expectedShortName = "${context.packageName}/.service.OrientationAccessibilityService"

            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (enabledServices != null) {
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)
                while (colonSplitter.hasNext()) {
                    val componentNameStr = colonSplitter.next()
                    val cn = ComponentName.unflattenFromString(componentNameStr)
                    if (cn != null && cn == expectedComponentName) {
                        return true
                    }
                    if (componentNameStr.equals(expectedServiceName, ignoreCase = true) ||
                        componentNameStr.equals(expectedShortName, ignoreCase = true)
                    ) {
                        return true
                    }
                }
            }

            // Also check via AccessibilityManager as a secondary fallback
            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                val services = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (services != null) {
                    for (service in services) {
                        val si = service.resolveInfo?.serviceInfo
                        if (si != null &&
                            si.packageName.equals(context.packageName, ignoreCase = true) &&
                            si.name.equals(OrientationAccessibilityService::class.java.name, ignoreCase = true)
                        ) {
                            return true
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error checking AccessibilityManager", e)
            }

            return false
        }
    }
}
