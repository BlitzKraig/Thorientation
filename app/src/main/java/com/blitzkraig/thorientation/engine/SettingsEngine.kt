package com.blitzkraig.thorientation.engine

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.blitzkraig.thorientation.model.PrivilegeStatus
import com.blitzkraig.thorientation.model.ScreenOrientation

object SettingsEngine {
    private const val TAG = "SettingsEngine"

    fun getStatus(context: Context): PrivilegeStatus {
        return if (Settings.System.canWrite(context)) {
            PrivilegeStatus.READY
        } else {
            PrivilegeStatus.NEEDS_PERMISSION
        }
    }

    /**
     * Applies rotation to the Primary display (Display 0) via standard Android system settings.
     */
    fun setOrientation(context: Context, displayId: Int, orientation: ScreenOrientation): Boolean {
        if (displayId != 0) {
            // Android System Settings only controls Display 0 (Primary)
            return false
        }
        if (!Settings.System.canWrite(context)) {
            Log.w(TAG, "Cannot write system settings: permission not granted")
            return false
        }

        return try {
            val resolver = context.contentResolver
            if (orientation == ScreenOrientation.AUTO) {
                Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1)
            } else {
                Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                Settings.System.putInt(resolver, Settings.System.USER_ROTATION, orientation.androidValue)
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to write system rotation settings", e)
            false
        }
    }
}
