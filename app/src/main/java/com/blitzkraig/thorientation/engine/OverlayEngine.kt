package com.blitzkraig.thorientation.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.blitzkraig.thorientation.model.PrivilegeStatus
import com.blitzkraig.thorientation.model.ScreenOrientation
import com.blitzkraig.thorientation.service.ScreenOverlayService

object OverlayEngine {

    fun getStatus(context: Context): PrivilegeStatus {
        return if (Settings.canDrawOverlays(context)) {
            PrivilegeStatus.READY
        } else {
            PrivilegeStatus.NEEDS_PERMISSION
        }
    }

    fun isAvailable(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun setOrientation(context: Context, displayId: Int, orientation: ScreenOrientation): Boolean {
        if (!isAvailable(context)) return false

        val intent = Intent(context, ScreenOverlayService::class.java).apply {
            action = ScreenOverlayService.ACTION_SET_OVERLAY_ORIENTATION
            putExtra(ScreenOverlayService.EXTRA_DISPLAY_ID, displayId)
            putExtra(ScreenOverlayService.EXTRA_ORIENTATION, orientation.ordinal)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        return true
    }

    fun setStandby(context: Context, displayId: Int, enable: Boolean): Boolean {
        if (!isAvailable(context)) return false

        val intent = Intent(context, ScreenOverlayService::class.java).apply {
            action = ScreenOverlayService.ACTION_SET_STANDBY
            putExtra(ScreenOverlayService.EXTRA_DISPLAY_ID, displayId)
            putExtra(ScreenOverlayService.EXTRA_STANDBY_ENABLE, enable)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        return true
    }

    fun setMasterEnabled(context: Context, enabled: Boolean): Boolean {
        if (!isAvailable(context)) return false

        val intent = Intent(context, ScreenOverlayService::class.java).apply {
            action = ScreenOverlayService.ACTION_SET_MASTER_ENABLED
            putExtra(ScreenOverlayService.EXTRA_MASTER_ENABLED, enabled)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        return true
    }
}
