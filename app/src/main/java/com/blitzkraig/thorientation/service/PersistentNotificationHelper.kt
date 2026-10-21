package com.blitzkraig.thorientation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.blitzkraig.thorientation.R
import com.blitzkraig.thorientation.engine.RotationEngine
import com.blitzkraig.thorientation.model.DualPreset
import com.blitzkraig.thorientation.model.ScreenOrientation
import com.blitzkraig.thorientation.receiver.NotificationActionReceiver
import com.blitzkraig.thorientation.ui.MainActivity

object PersistentNotificationHelper {

    private const val PREFS_NAME = "thorientation_prefs"
    const val KEY_NOTIFICATION_ENABLED = "persistent_notif_enabled"
    const val KEY_NOTIFICATION_MODE = "persistent_notif_mode"

    const val MODE_PRESETS = "presets"
    const val MODE_TOP_SCREEN = "top_screen"
    const val MODE_BOTTOM_SCREEN = "bottom_screen"

    const val CHANNEL_ID = "thorientation_quick_controls"
    const val NOTIFICATION_ID = 1001

    private val COLOR_ACTIVE_BG = R.drawable.bg_notif_button_active
    private val COLOR_INACTIVE_BG = R.drawable.bg_notif_button
    private val COLOR_ACTIVE_TEXT = Color.parseColor("#111215")
    private val COLOR_INACTIVE_TEXT = Color.parseColor("#D8D9E0")

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
        startOrUpdate(context)
    }

    fun getMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NOTIFICATION_MODE, MODE_PRESETS) ?: MODE_PRESETS
    }

    fun setMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTIFICATION_MODE, mode).apply()
        update(context)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context): Notification {
        ensureChannel(context)

        val engine = RotationEngine.get(context)
        val displays = engine.getDisplays()
        val topDisplay = displays.firstOrNull { it.isPrimary } ?: displays.firstOrNull()
        val bottomDisplay = displays.firstOrNull { !it.isPrimary }
        val activePreset = engine.getActivePreset()
        val mode = getMode(context)

        val topOrientation = topDisplay?.currentRotation ?: ScreenOrientation.LANDSCAPE
        val bottomOrientation = bottomDisplay?.currentRotation ?: ScreenOrientation.LANDSCAPE

        // Content intent to open MainActivity
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            100,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Collapsed View
        val remoteCollapsed = RemoteViews(context.packageName, R.layout.notification_collapsed)
        val topDisplayId = topDisplay?.displayId ?: 0
        val bottomDisplayId = bottomDisplay?.displayId ?: 1

        val colStatusText = when (mode) {
            MODE_TOP_SCREEN -> "Top: ${context.getString(topOrientation.shortLabelRes)}"
            MODE_BOTTOM_SCREEN -> "Bottom: ${context.getString(bottomOrientation.shortLabelRes)}"
            else -> "Preset: ${context.getString(activePreset.titleRes)}"
        }

        // Wire 4 buttons for collapsed view based on mode: Left, Landscape, Right, Auto
        when (mode) {
            MODE_TOP_SCREEN -> {
                setupOrientationButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_1, tvId = R.id.notif_col_tv_1, ivId = R.id.notif_col_iv_1,
                    displayId = topDisplayId, orientation = ScreenOrientation.PORTRAIT,
                    isActive = topOrientation == ScreenOrientation.PORTRAIT, requestCode = 101
                )
                setupOrientationButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_2, tvId = R.id.notif_col_tv_2, ivId = R.id.notif_col_iv_2,
                    displayId = topDisplayId, orientation = ScreenOrientation.LANDSCAPE,
                    isActive = topOrientation == ScreenOrientation.LANDSCAPE, requestCode = 102
                )
                setupOrientationButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_3, tvId = R.id.notif_col_tv_3, ivId = R.id.notif_col_iv_3,
                    displayId = topDisplayId, orientation = ScreenOrientation.REVERSE_PORTRAIT,
                    isActive = topOrientation == ScreenOrientation.REVERSE_PORTRAIT, requestCode = 103
                )
                setupOrientationButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_4, tvId = R.id.notif_col_tv_4, ivId = R.id.notif_col_iv_4,
                    displayId = topDisplayId, orientation = ScreenOrientation.AUTO,
                    isActive = topOrientation == ScreenOrientation.AUTO, requestCode = 104
                )
            }
            MODE_BOTTOM_SCREEN -> {
                setupOrientationButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_1, tvId = R.id.notif_col_tv_1, ivId = R.id.notif_col_iv_1,
                    displayId = bottomDisplayId, orientation = ScreenOrientation.PORTRAIT,
                    isActive = bottomOrientation == ScreenOrientation.PORTRAIT, requestCode = 101
                )
                setupOrientationButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_2, tvId = R.id.notif_col_tv_2, ivId = R.id.notif_col_iv_2,
                    displayId = bottomDisplayId, orientation = ScreenOrientation.LANDSCAPE,
                    isActive = bottomOrientation == ScreenOrientation.LANDSCAPE, requestCode = 102
                )
                setupOrientationButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_3, tvId = R.id.notif_col_tv_3, ivId = R.id.notif_col_iv_3,
                    displayId = bottomDisplayId, orientation = ScreenOrientation.REVERSE_PORTRAIT,
                    isActive = bottomOrientation == ScreenOrientation.REVERSE_PORTRAIT, requestCode = 103
                )
                remoteCollapsed.setViewVisibility(R.id.notif_col_btn_4, View.VISIBLE)
                setupOrientationButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_4, tvId = R.id.notif_col_tv_4, ivId = R.id.notif_col_iv_4,
                    displayId = bottomDisplayId, orientation = ScreenOrientation.AUTO,
                    isActive = bottomOrientation == ScreenOrientation.AUTO, requestCode = 104
                )
            }
            else -> {
                // Dual Presets: Left (Portrait L), Landscape, Right (Portrait R), Auto
                setupPresetButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_1, tvId = R.id.notif_col_tv_1, ivId = R.id.notif_col_iv_1,
                    preset = DualPreset.PORTRAIT, isActive = activePreset == DualPreset.PORTRAIT, requestCode = 101
                )
                setupPresetButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_2, tvId = R.id.notif_col_tv_2, ivId = R.id.notif_col_iv_2,
                    preset = DualPreset.LANDSCAPE, isActive = activePreset == DualPreset.LANDSCAPE, requestCode = 102
                )
                setupPresetButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_3, tvId = R.id.notif_col_tv_3, ivId = R.id.notif_col_iv_3,
                    preset = DualPreset.PORTRAIT_REVERSE, isActive = activePreset == DualPreset.PORTRAIT_REVERSE, requestCode = 103
                )
                setupPresetButton(
                    context, remoteCollapsed,
                    btnId = R.id.notif_col_btn_4, tvId = R.id.notif_col_tv_4, ivId = R.id.notif_col_iv_4,
                    preset = DualPreset.AUTO, isActive = activePreset == DualPreset.AUTO, requestCode = 104
                )
            }
        }

        // Expanded View
        val remoteExpanded = RemoteViews(context.packageName, R.layout.notification_expanded)
        val expStatusText = buildString {
            append("Preset: ").append(context.getString(activePreset.titleRes))
            append(" • Top: ").append(context.getString(topOrientation.shortLabelRes))
            if (bottomDisplay != null) {
                append(" • Btm: ").append(context.getString(bottomOrientation.shortLabelRes))
            }
        }
        remoteExpanded.setTextViewText(R.id.notif_exp_header_status, expStatusText)

        // Section 1: Presets
        setupPresetButton(
            context, remoteExpanded,
            btnId = R.id.notif_exp_preset_landscape, tvId = R.id.notif_exp_preset_tv_landscape, ivId = R.id.notif_exp_preset_iv_landscape,
            preset = DualPreset.LANDSCAPE, isActive = activePreset == DualPreset.LANDSCAPE, requestCode = 201
        )
        setupPresetButton(
            context, remoteExpanded,
            btnId = R.id.notif_exp_preset_portrait, tvId = R.id.notif_exp_preset_tv_portrait, ivId = R.id.notif_exp_preset_iv_portrait,
            preset = DualPreset.PORTRAIT, isActive = activePreset == DualPreset.PORTRAIT, requestCode = 202
        )
        setupPresetButton(
            context, remoteExpanded,
            btnId = R.id.notif_exp_preset_portrait_reverse, tvId = R.id.notif_exp_preset_tv_portrait_reverse, ivId = R.id.notif_exp_preset_iv_portrait_reverse,
            preset = DualPreset.PORTRAIT_REVERSE, isActive = activePreset == DualPreset.PORTRAIT_REVERSE, requestCode = 203
        )
        setupPresetButton(
            context, remoteExpanded,
            btnId = R.id.notif_exp_preset_auto, tvId = R.id.notif_exp_preset_tv_auto, ivId = R.id.notif_exp_preset_iv_auto,
            preset = DualPreset.AUTO, isActive = activePreset == DualPreset.AUTO, requestCode = 204
        )

        // Section 2: Top Screen
        setupOrientationButton(
            context, remoteExpanded,
            btnId = R.id.notif_exp_top_landscape, tvId = R.id.notif_exp_top_tv_landscape, ivId = R.id.notif_exp_top_iv_landscape,
            displayId = topDisplayId, orientation = ScreenOrientation.LANDSCAPE,
            isActive = topOrientation == ScreenOrientation.LANDSCAPE, requestCode = 301
        )
        setupOrientationButton(
            context, remoteExpanded,
            btnId = R.id.notif_exp_top_portrait, tvId = R.id.notif_exp_top_tv_portrait, ivId = R.id.notif_exp_top_iv_portrait,
            displayId = topDisplayId, orientation = ScreenOrientation.PORTRAIT,
            isActive = topOrientation == ScreenOrientation.PORTRAIT, requestCode = 302
        )
        setupOrientationButton(
            context, remoteExpanded,
            btnId = R.id.notif_exp_top_portrait_reverse, tvId = R.id.notif_exp_top_tv_portrait_reverse, ivId = R.id.notif_exp_top_iv_portrait_reverse,
            displayId = topDisplayId, orientation = ScreenOrientation.REVERSE_PORTRAIT,
            isActive = topOrientation == ScreenOrientation.REVERSE_PORTRAIT, requestCode = 303
        )
        setupOrientationButton(
            context, remoteExpanded,
            btnId = R.id.notif_exp_top_auto, tvId = R.id.notif_exp_top_tv_auto, ivId = R.id.notif_exp_top_iv_auto,
            displayId = topDisplayId, orientation = ScreenOrientation.AUTO,
            isActive = topOrientation == ScreenOrientation.AUTO, requestCode = 304
        )

        // Section 3: Bottom Screen
        if (bottomDisplay != null) {
            remoteExpanded.setViewVisibility(R.id.notif_exp_bottom_section, View.VISIBLE)
            setupOrientationButton(
                context, remoteExpanded,
                btnId = R.id.notif_exp_bottom_landscape, tvId = R.id.notif_exp_bottom_tv_landscape, ivId = R.id.notif_exp_bottom_iv_landscape,
                displayId = bottomDisplayId, orientation = ScreenOrientation.LANDSCAPE,
                isActive = bottomOrientation == ScreenOrientation.LANDSCAPE, requestCode = 401
            )
            setupOrientationButton(
                context, remoteExpanded,
                btnId = R.id.notif_exp_bottom_portrait, tvId = R.id.notif_exp_bottom_tv_portrait, ivId = R.id.notif_exp_bottom_iv_portrait,
                displayId = bottomDisplayId, orientation = ScreenOrientation.PORTRAIT,
                isActive = bottomOrientation == ScreenOrientation.PORTRAIT, requestCode = 402
            )
            setupOrientationButton(
                context, remoteExpanded,
                btnId = R.id.notif_exp_bottom_portrait_reverse, tvId = R.id.notif_exp_bottom_tv_portrait_reverse, ivId = R.id.notif_exp_bottom_iv_portrait_reverse,
                displayId = bottomDisplayId, orientation = ScreenOrientation.REVERSE_PORTRAIT,
                isActive = bottomOrientation == ScreenOrientation.REVERSE_PORTRAIT, requestCode = 403
            )
            remoteExpanded.setViewVisibility(R.id.notif_exp_bottom_auto, View.VISIBLE)
            setupOrientationButton(
                context, remoteExpanded,
                btnId = R.id.notif_exp_bottom_auto, tvId = R.id.notif_exp_bottom_tv_auto, ivId = R.id.notif_exp_bottom_iv_auto,
                displayId = bottomDisplayId, orientation = ScreenOrientation.AUTO,
                isActive = bottomOrientation == ScreenOrientation.AUTO, requestCode = 404
            )
        } else {
            remoteExpanded.setViewVisibility(R.id.notif_exp_bottom_section, View.GONE)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dual_screen)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(colStatusText)
            .setSubText(colStatusText)
            .setCustomContentView(remoteCollapsed)
            .setCustomBigContentView(remoteExpanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun setupPresetButton(
        context: Context,
        remoteViews: RemoteViews,
        btnId: Int,
        tvId: Int,
        ivId: Int,
        preset: DualPreset,
        isActive: Boolean,
        requestCode: Int
    ) {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SET_PRESET
            putExtra(NotificationActionReceiver.EXTRA_PRESET, preset.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(btnId, pendingIntent)

        if (isActive) {
            remoteViews.setInt(btnId, "setBackgroundResource", COLOR_ACTIVE_BG)
            remoteViews.setTextColor(tvId, COLOR_ACTIVE_TEXT)
            remoteViews.setInt(ivId, "setColorFilter", COLOR_ACTIVE_TEXT)
        } else {
            remoteViews.setInt(btnId, "setBackgroundResource", COLOR_INACTIVE_BG)
            remoteViews.setTextColor(tvId, COLOR_INACTIVE_TEXT)
            remoteViews.setInt(ivId, "setColorFilter", COLOR_INACTIVE_TEXT)
        }
    }

    private fun setupOrientationButton(
        context: Context,
        remoteViews: RemoteViews,
        btnId: Int,
        tvId: Int,
        ivId: Int,
        displayId: Int,
        orientation: ScreenOrientation,
        isActive: Boolean,
        requestCode: Int
    ) {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SET_ORIENTATION
            putExtra(NotificationActionReceiver.EXTRA_DISPLAY_ID, displayId)
            putExtra(NotificationActionReceiver.EXTRA_ORIENTATION, orientation.ordinal)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(btnId, pendingIntent)

        if (isActive) {
            remoteViews.setInt(btnId, "setBackgroundResource", COLOR_ACTIVE_BG)
            remoteViews.setTextColor(tvId, COLOR_ACTIVE_TEXT)
            remoteViews.setInt(ivId, "setColorFilter", COLOR_ACTIVE_TEXT)
        } else {
            remoteViews.setInt(btnId, "setBackgroundResource", COLOR_INACTIVE_BG)
            remoteViews.setTextColor(tvId, COLOR_INACTIVE_TEXT)
            remoteViews.setInt(ivId, "setColorFilter", COLOR_INACTIVE_TEXT)
        }
    }

    fun startOrUpdate(context: Context) {
        val intent = Intent(context, ScreenOverlayService::class.java).apply {
            action = ScreenOverlayService.ACTION_UPDATE_NOTIFICATION
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Throwable) {
            // If already foreground, update directly
            update(context)
        }
    }

    fun update(context: Context) {
        if (isEnabled(context)) {
            try {
                val notification = buildNotification(context)
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: Throwable) {}
        }
    }
}
