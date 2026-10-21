package com.blitzkraig.thorientation.engine

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

object ThorSettingsEngine {
    private const val TAG = "ThorSettingsEngine"
    const val SETTING_FORCE_LANDSCAPE = "force_landscape"

    /**
     * Checks if Thor's native "Force Landscape" mode is enabled in Settings.System.
     */
    fun isForceLandscapeEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, SETTING_FORCE_LANDSCAPE, 0) == 1
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to read $SETTING_FORCE_LANDSCAPE setting", e)
            false
        }
    }

    /**
     * Attempts to disable Thor's "Force Landscape" mode directly via Settings.System.
     * Returns true if successfully updated.
     */
    fun disableForceLandscape(context: Context): Boolean {
        return try {
            if (!Settings.System.canWrite(context)) {
                Log.w(TAG, "Cannot disable $SETTING_FORCE_LANDSCAPE: WRITE_SETTINGS permission not granted")
                return false
            }
            val success = Settings.System.putInt(context.contentResolver, SETTING_FORCE_LANDSCAPE, 0)
            Log.i(TAG, "Set $SETTING_FORCE_LANDSCAPE to 0 result: $success")
            success
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to write $SETTING_FORCE_LANDSCAPE setting", e)
            false
        }
    }

    /**
     * Registers a ContentObserver for real-time detection of changes to Thor's Force Landscape toggle.
     */
    fun registerObserver(context: Context, onChange: () -> Unit): ContentObserver {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange()
            }
        }
        try {
            val uri = Settings.System.getUriFor(SETTING_FORCE_LANDSCAPE)
            context.contentResolver.registerContentObserver(uri, false, observer)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register observer for $SETTING_FORCE_LANDSCAPE", e)
        }
        return observer
    }

    /**
     * Unregisters a previously registered ContentObserver.
     */
    fun unregisterObserver(context: Context, observer: ContentObserver) {
        try {
            context.contentResolver.unregisterContentObserver(observer)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to unregister observer", e)
        }
    }
}
