package com.blitzkraig.thorientation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.blitzkraig.thorientation.R
import com.blitzkraig.thorientation.model.ScreenOrientation
import com.blitzkraig.thorientation.receiver.BootReceiver

class ScreenOverlayService : Service() {

    private lateinit var displayManager: DisplayManager
    // Map of displayId -> overlay View
    private val activeOverlays = mutableMapOf<Int, Pair<WindowManager, View>>()
    private val autoTrackingDisplays = mutableSetOf<Int>()
    private var isTopAuto: Boolean = false
    private var lastSensorOrientation: ScreenOrientation = ScreenOrientation.LANDSCAPE
    private var standbyOverlayView: Pair<WindowManager, View>? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val orientationListener by lazy {
        object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(angle: Int) {
                if (angle == ORIENTATION_UNKNOWN) return
                val newOrientation = calculateOrientationFromAngle(angle, lastSensorOrientation)
                if (newOrientation != lastSensorOrientation) {
                    lastSensorOrientation = newOrientation
                    Log.i(TAG, "Hardware sensor rotated to: ${newOrientation.name} (angle: $angle°)")

                    if (isTopAuto) {
                        applyDisplayOverlay(Display.DEFAULT_DISPLAY, newOrientation)
                    }
                    for (displayId in autoTrackingDisplays) {
                        applyDisplayOverlay(displayId, newOrientation)
                    }
                }
            }
        }
    }

    private fun calculateOrientationFromAngle(angle: Int, current: ScreenOrientation): ScreenOrientation {
        if (angle == OrientationEventListener.ORIENTATION_UNKNOWN) return current
        return when {
            // Landscape (either standard 270° or reverse 90°)
            angle in 60..120 || angle in 240..300 -> ScreenOrientation.LANDSCAPE
            // Reverse Portrait (upside-down ~180°)
            angle in 150..210 -> ScreenOrientation.REVERSE_PORTRAIT
            // Portrait (~0° / 360°)
            angle >= 330 || angle <= 30 -> ScreenOrientation.PORTRAIT
            // Deadband / transition zones: stay in current orientation to prevent jitter
            else -> current
        }
    }

    private fun updateSensorListenerState() {
        if (isTopAuto || autoTrackingDisplays.isNotEmpty()) {
            if (orientationListener.canDetectOrientation()) {
                orientationListener.enable()
            }
        } else {
            orientationListener.disable()
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                isTopAuto = false
            }
            autoTrackingDisplays.remove(displayId)
            updateSensorListenerState()
            removeOverlay(displayId)
            checkStopSelfIfIdle()
        }
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                handleTopDisplayChanged()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, mainHandler)
        val topDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (topDisplay != null) {
            lastSensorOrientation = ScreenOrientation.fromSurfaceRotation(topDisplay.rotation)
        }
        startForegroundServiceNotification()
        if (BootReceiver.isStartAtBootEnabled(this)) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_MASTER_ENABLED, true)) {
                restoreSavedOrientations()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isMaster = prefs.getBoolean(KEY_MASTER_ENABLED, true)

        if (intent == null) {
            Log.i(TAG, "Service restarted by system with null intent")
            if (BootReceiver.isStartAtBootEnabled(this) && isMaster) {
                restoreSavedOrientations()
                checkStopSelfIfIdle()
                return START_STICKY
            } else {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val action = intent.action ?: run {
            if (BootReceiver.isStartAtBootEnabled(this) && isMaster) {
                restoreSavedOrientations()
                checkStopSelfIfIdle()
                return START_STICKY
            } else {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        when (action) {
            ACTION_SET_MASTER_ENABLED -> {
                val enabled = intent.getBooleanExtra(EXTRA_MASTER_ENABLED, true)
                prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
                if (enabled) {
                    restoreSavedOrientations()
                } else {
                    clearAllOverlaysInternal()
                }
                checkStopSelfIfIdle()
            }
            ACTION_SET_OVERLAY_ORIENTATION -> {
                val displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, -1)
                val orientationOrdinal = intent.getIntExtra(EXTRA_ORIENTATION, -1)
                if (displayId >= 0 && orientationOrdinal >= 0) {
                    val orientation = ScreenOrientation.values()[orientationOrdinal]
                    applyOverlayOrientation(displayId, orientation)
                }
            }
            ACTION_CLEAR_OVERLAY -> {
                val displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, -1)
                if (displayId >= 0) {
                    removeOverlay(displayId)
                }
                checkStopSelfIfIdle()
            }
            ACTION_SET_STANDBY -> {
                val enable = intent.getBooleanExtra(EXTRA_STANDBY_ENABLE, false)
                val displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, 1)
                setBottomScreenStandby(displayId, enable)
                checkStopSelfIfIdle()
            }
            ACTION_UPDATE_NOTIFICATION -> {
                if (intent.hasExtra(PersistentNotificationHelper.KEY_NOTIFICATION_ENABLED)) {
                    val enabled = intent.getBooleanExtra(PersistentNotificationHelper.KEY_NOTIFICATION_ENABLED, false)
                    prefs.edit().putBoolean(PersistentNotificationHelper.KEY_NOTIFICATION_ENABLED, enabled).apply()
                }
                startForegroundServiceNotification()
                checkStopSelfIfIdle()
            }
        }

        return if (BootReceiver.isStartAtBootEnabled(this)) START_STICKY else START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: app task removed from recents, keeping overlay service active")
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (activeOverlays.isEmpty() && BootReceiver.isStartAtBootEnabled(this) && prefs.getBoolean(KEY_MASTER_ENABLED, true)) {
            restoreSavedOrientations()
        }
    }

    private fun restoreSavedOrientations() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_MASTER_ENABLED, true)) {
            Log.d(TAG, "Master switch is disabled; skipping restoreSavedOrientations")
            return
        }
        val displays = displayManager.displays
        for (display in displays) {
            val key = "$KEY_SAVED_ROTATION_PREFIX${display.displayId}"
            val ordinal = prefs.getInt(key, -1)
            if (ordinal in ScreenOrientation.values().indices) {
                val orientation = ScreenOrientation.values()[ordinal]
                val sanitized = if (orientation == ScreenOrientation.REVERSE_LANDSCAPE) ScreenOrientation.LANDSCAPE else orientation
                Log.d(TAG, "Restoring overlay orientation on display ${display.displayId} to ${sanitized.name}")
                applyOverlayOrientation(display.displayId, sanitized)
            }
        }
        val standby = prefs.getBoolean(KEY_BOTTOM_STANDBY, false)
        if (standby) {
            val bottomDisplay = displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
            if (bottomDisplay != null) {
                setBottomScreenStandby(bottomDisplay.displayId, true)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleTopDisplayChanged()
    }

    private fun handleTopDisplayChanged() {
        if (autoTrackingDisplays.isEmpty()) return

        val syncWithTop = {
            val currentTopOrientation = if (isTopAuto) {
                lastSensorOrientation
            } else {
                val topDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                if (topDisplay != null) ScreenOrientation.fromSurfaceRotation(topDisplay.rotation) else ScreenOrientation.PORTRAIT
            }
            for (displayId in autoTrackingDisplays) {
                Log.d(TAG, "Top display changed, syncing bottom display $displayId to ${currentTopOrientation.name}")
                applyDisplayOverlay(displayId, currentTopOrientation)
            }
        }

        syncWithTop()
        mainHandler.postDelayed(syncWithTop, 150L)
    }

    private fun applyOverlayOrientation(displayId: Int, orientation: ScreenOrientation) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("$KEY_SAVED_ROTATION_PREFIX$displayId", orientation.ordinal).apply()

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot apply overlay: overlay permission not granted")
            return
        }

        if (displayId == Display.DEFAULT_DISPLAY) {
            // Primary (top) display
            if (orientation == ScreenOrientation.AUTO) {
                isTopAuto = true
                if (Settings.System.canWrite(this)) {
                    try {
                        Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Could not set accelerometer_rotation", e)
                    }
                }
                applyDisplayOverlay(displayId, lastSensorOrientation)
            } else {
                isTopAuto = false
                if (Settings.System.canWrite(this)) {
                    try {
                        Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Could not set accelerometer_rotation", e)
                    }
                }
                applyDisplayOverlay(displayId, orientation)
            }
            updateSensorListenerState()
            handleTopDisplayChanged()
            return
        }

        // Secondary display (bottom screen)
        if (orientation == ScreenOrientation.AUTO) {
            autoTrackingDisplays.add(displayId)
            val targetOrientation = if (isTopAuto) {
                lastSensorOrientation
            } else {
                val topDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                topDisplay?.rotation?.let { ScreenOrientation.fromSurfaceRotation(it) } ?: ScreenOrientation.LANDSCAPE
            }
            Log.d(TAG, "Secondary display $displayId set to AUTO; applying orientation: ${targetOrientation.name}")
            applyDisplayOverlay(displayId, targetOrientation)
        } else {
            autoTrackingDisplays.remove(displayId)
            applyDisplayOverlay(displayId, orientation)
        }
        updateSensorListenerState()
    }

    private fun applyDisplayOverlay(displayId: Int, orientation: ScreenOrientation) {
        val targetDisplay = displayManager.getDisplay(displayId) ?: return

        try {
            // For the default display (0), use the service's primary WindowManager and context.
            // For secondary displays, use the display context.
            val (windowManager, viewContext) = if (displayId == Display.DEFAULT_DISPLAY) {
                Pair(getSystemService(Context.WINDOW_SERVICE) as WindowManager, applicationContext)
            } else {
                val displayContext = createDisplayContext(targetDisplay)
                Pair(displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager, displayContext)
            }

            val params = WindowManager.LayoutParams(
                0, 0, // 0x0 size invisible overlay
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                screenOrientation = orientation.toActivityOrientation()
            }

            val existing = activeOverlays[displayId]
            if (existing != null) {
                // Update the existing view layout instantly without tearing down
                existing.first.updateViewLayout(existing.second, params)
                Log.d(TAG, "Updated overlay orientation ${orientation.name} on display $displayId")
            } else {
                val view = View(viewContext)
                windowManager.addView(view, params)
                activeOverlays[displayId] = Pair(windowManager, view)
                Log.d(TAG, "Added overlay orientation ${orientation.name} on display $displayId")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply orientation overlay for display $displayId", e)
        }
    }

    private fun removeOverlay(displayId: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_SAVED_ROTATION_PREFIX$displayId").apply()

        if (displayId == Display.DEFAULT_DISPLAY) {
            isTopAuto = false
        }
        autoTrackingDisplays.remove(displayId)
        updateSensorListenerState()
        activeOverlays.remove(displayId)?.let { (wm, view) ->
            try {
                wm.removeView(view)
            } catch (e: Throwable) {
                Log.e(TAG, "Error removing overlay for display $displayId", e)
            }
        }
    }

    private fun setBottomScreenStandby(displayId: Int, enable: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BOTTOM_STANDBY, enable).apply()

        if (!Settings.canDrawOverlays(this)) return

        standbyOverlayView?.let { (wm, view) ->
            try {
                wm.removeView(view)
            } catch (e: Throwable) {
                Log.e(TAG, "Error removing standby overlay", e)
            }
            standbyOverlayView = null
        }

        if (!enable) return

        val targetDisplay = displayManager.getDisplay(displayId) ?: return

        try {
            val displayContext = createDisplayContext(targetDisplay)
            val windowManager = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE
            ).apply {
                gravity = Gravity.FILL
            }

            val blackView = View(displayContext).apply {
                setBackgroundColor(Color.BLACK)
            }

            windowManager.addView(blackView, params)
            standbyOverlayView = Pair(windowManager, blackView)
            Log.d(TAG, "Enabled standby blackout on display $displayId")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to enable standby on display $displayId", e)
        }
    }

    private fun isIdle(): Boolean {
        return activeOverlays.isEmpty() &&
                standbyOverlayView == null &&
                autoTrackingDisplays.isEmpty() &&
                !isTopAuto
    }

    private fun checkStopSelfIfIdle() {
        if (!PersistentNotificationHelper.isEnabled(this) && isIdle()) {
            Log.d(TAG, "No active overlays or persistent notification; stopping service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startForegroundServiceNotification() {
        if (PersistentNotificationHelper.isEnabled(this)) {
            val notification = PersistentNotificationHelper.buildNotification(this)
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startSilentForegroundNotification()
        }
    }

    private fun startSilentForegroundNotification() {
        val channelId = "screen_manager_overlay"
        val channelName = "Thorientation Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Screen orientation and standby controller active")
            .setSmallIcon(R.drawable.ic_dual_screen)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun clearAllOverlaysInternal() {
        orientationListener.disable()
        activeOverlays.forEach { (_, pair) ->
            try { pair.first.removeView(pair.second) } catch (e: Throwable) {
                Log.w(TAG, "Error removing overlay view", e)
            }
        }
        activeOverlays.clear()
        autoTrackingDisplays.clear()
        isTopAuto = false
        standbyOverlayView?.let { (wm, view) ->
            try { wm.removeView(view) } catch (e: Throwable) {
                Log.w(TAG, "Error removing standby overlay view", e)
            }
        }
        standbyOverlayView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        displayManager.unregisterDisplayListener(displayListener)
        mainHandler.removeCallbacksAndMessages(null)
        clearAllOverlaysInternal()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ScreenOverlayService"
        const val NOTIFICATION_ID = 1001

        private const val PREFS_NAME = "thorientation_prefs"
        const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_SAVED_ROTATION_PREFIX = "saved_rot_disp_"
        private const val KEY_BOTTOM_STANDBY = "bottom_standby"

        const val ACTION_SET_MASTER_ENABLED = "com.blitzkraig.thorientation.action.SET_MASTER_ENABLED"
        const val ACTION_SET_OVERLAY_ORIENTATION = "com.blitzkraig.thorientation.action.SET_OVERLAY_ORIENTATION"
        const val ACTION_CLEAR_OVERLAY = "com.blitzkraig.thorientation.action.CLEAR_OVERLAY"
        const val ACTION_SET_STANDBY = "com.blitzkraig.thorientation.action.SET_STANDBY"
        const val ACTION_UPDATE_NOTIFICATION = "com.blitzkraig.thorientation.action.UPDATE_NOTIFICATION"

        const val EXTRA_MASTER_ENABLED = "extra_master_enabled"
        const val EXTRA_DISPLAY_ID = "extra_display_id"
        const val EXTRA_ORIENTATION = "extra_orientation"
        const val EXTRA_STANDBY_ENABLE = "extra_standby_enable"
    }
}
