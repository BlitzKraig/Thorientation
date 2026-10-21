package com.blitzkraig.thorientation.engine

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.widget.Toast
import com.blitzkraig.thorientation.model.DisplayInfoModel
import com.blitzkraig.thorientation.model.DualPreset
import com.blitzkraig.thorientation.model.PrivilegeStatus
import com.blitzkraig.thorientation.model.ScreenOrientation
import com.blitzkraig.thorientation.service.BottomScreenTileService
import com.blitzkraig.thorientation.service.DualPresetTileService
import com.blitzkraig.thorientation.service.PersistentNotificationHelper
import com.blitzkraig.thorientation.service.TopScreenTileService

class RotationEngine(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    /**
     * Queries and inspects all connected displays dynamically.
     */
    fun getDisplays(): List<DisplayInfoModel> {
        val rawDisplays = displayManager.displays
        val result = mutableListOf<DisplayInfoModel>()

        for (display in rawDisplays) {
            val isPrimary = display.displayId == Display.DEFAULT_DISPLAY
            val defaultName = if (isPrimary) {
                "Top Screen (Primary)"
            } else {
                "Bottom Screen (${display.displayId})"
            }

            val size = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(size)

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)

            val currentRotation = getSavedOrientation(display.displayId)
                ?: ScreenOrientation.fromSurfaceRotation(display.rotation)

            result.add(
                DisplayInfoModel(
                    displayId = display.displayId,
                    name = defaultName,
                    width = size.x,
                    height = size.y,
                    refreshRate = display.refreshRate,
                    densityDpi = metrics.densityDpi,
                    currentRotation = currentRotation,
                    isPrimary = isPrimary
                )
            )
        }

        // Always sort with primary (Top Screen) first
        result.sortBy { if (it.isPrimary) 0 else 1 }
        return result
    }

    fun getSavedOrientation(displayId: Int): ScreenOrientation? {
        val key = "$KEY_SAVED_ROTATION_PREFIX$displayId"
        val ordinal = prefs.getInt(key, -1)
        val orientation = if (ordinal in ScreenOrientation.values().indices) {
            ScreenOrientation.values()[ordinal]
        } else {
            null
        }
        val sanitized = if (orientation == ScreenOrientation.REVERSE_LANDSCAPE) ScreenOrientation.LANDSCAPE else orientation
        return sanitized
    }

    private fun saveOrientation(displayId: Int, orientation: ScreenOrientation) {
        prefs.edit().putInt("$KEY_SAVED_ROTATION_PREFIX$displayId", orientation.ordinal).apply()
        // Notify Quick Settings tiles to update their labels and icons immediately
        requestTileUpdates()
        PersistentNotificationHelper.update(context)
    }

    private var lastForceLandscapeWarningTime = 0L

    private fun checkAndWarnForceLandscape() {
        if (ThorSettingsEngine.isForceLandscapeEnabled(context)) {
            val now = System.currentTimeMillis()
            if (now - lastForceLandscapeWarningTime > 2500L) {
                lastForceLandscapeWarningTime = now
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Notice: Thor's 'Force Landscape' is enabled and may override orientation!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Applies orientation to a specific display using overlay and system settings.
     */
    fun setOrientation(displayId: Int, orientation: ScreenOrientation): Boolean {
        Log.i(TAG, "Request to rotate display $displayId to ${orientation.name}")
        checkAndWarnForceLandscape()

        // Keep system settings in sync if WRITE_SETTINGS is granted (for primary display)
        if (displayId == 0 && SettingsEngine.getStatus(context) == PrivilegeStatus.READY) {
            SettingsEngine.setOrientation(context, displayId, orientation)
        }

        // Enforce orientation via ScreenOverlayService
        val success = OverlayEngine.setOrientation(context, displayId, orientation)

        if (success) {
            saveOrientation(displayId, orientation)
        }
        return success
    }

    /**
     * Applies a preset configuration simultaneously to both top and bottom screens.
     */
    fun applyPreset(preset: DualPreset): Boolean {
        val displays = getDisplays()
        val topDisplay = displays.firstOrNull { it.isPrimary } ?: displays.firstOrNull()
        val bottomDisplay = displays.firstOrNull { !it.isPrimary }

        var successTop = false
        // Apply top screen orientation first so secondary screens can validate AUTO if preset is AUTO
        if (topDisplay != null) {
            successTop = setOrientation(topDisplay.displayId, preset.topOrientation)
        }

        val successBottom = if (bottomDisplay != null) {
            setOrientation(bottomDisplay.displayId, preset.bottomOrientation)
        } else {
            true
        }

        prefs.edit().putString(KEY_ACTIVE_PRESET, preset.name).apply()
        requestTileUpdates()
        PersistentNotificationHelper.update(context)
        return successTop && successBottom
    }

    fun getActivePreset(): DualPreset {
        val name = prefs.getString(KEY_ACTIVE_PRESET, DualPreset.LANDSCAPE.name) ?: DualPreset.LANDSCAPE.name
        return try {
            DualPreset.valueOf(name)
        } catch (_: Throwable) {
            DualPreset.LANDSCAPE
        }
    }

    fun setBottomStandby(enable: Boolean): Boolean {
        val displays = getDisplays()
        val bottomDisplay = displays.firstOrNull { !it.isPrimary } ?: return false
        val ok = OverlayEngine.setStandby(context, bottomDisplay.displayId, enable)
        if (ok) {
            prefs.edit().putBoolean(KEY_BOTTOM_STANDBY, enable).apply()
        }
        return ok
    }

    fun isBottomStandbyEnabled(): Boolean {
        return prefs.getBoolean(KEY_BOTTOM_STANDBY, false)
    }

    fun requestTileUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(context, ComponentName(context, TopScreenTileService::class.java))
                TileService.requestListeningState(context, ComponentName(context, BottomScreenTileService::class.java))
                TileService.requestListeningState(context, ComponentName(context, DualPresetTileService::class.java))
            } catch (e: Throwable) {
                Log.w(TAG, "Could not request tile update", e)
            }
        }
    }

    companion object {
        private const val TAG = "RotationEngine"
        private const val PREFS_NAME = "thorientation_prefs"
        private const val KEY_SAVED_ROTATION_PREFIX = "saved_rot_disp_"
        private const val KEY_ACTIVE_PRESET = "active_preset"
        private const val KEY_BOTTOM_STANDBY = "bottom_standby"

        @Volatile
        private var instance: RotationEngine? = null

        fun get(context: Context): RotationEngine {
            return instance ?: synchronized(this) {
                instance ?: RotationEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
