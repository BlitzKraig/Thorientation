package com.blitzkraig.thorientation.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.blitzkraig.thorientation.R
import com.blitzkraig.thorientation.engine.RotationEngine
import com.blitzkraig.thorientation.model.ScreenOrientation
import com.blitzkraig.thorientation.ui.QuickRotationDialogHelper

class BottomScreenTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val displayManager = getSystemService(DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        val bottomDisplay = displayManager.displays.firstOrNull { it.displayId != android.view.Display.DEFAULT_DISPLAY }
        val displayId = bottomDisplay?.displayId ?: 1

        QuickRotationDialogHelper.show(
            service = this,
            displayId = displayId,
            displayName = getString(R.string.tile_bottom_screen),
            onComplete = { updateTileState() }
        )
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val displayManager = getSystemService(DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        val bottomDisplay = displayManager.displays.firstOrNull { it.displayId != android.view.Display.DEFAULT_DISPLAY }
        val displayId = bottomDisplay?.displayId ?: 1
        val engine = RotationEngine.get(this)
        val isMaster = engine.isMasterEnabled()

        val currentOrientation = engine.getSavedOrientation(displayId)
            ?: (bottomDisplay?.rotation?.let { ScreenOrientation.fromSurfaceRotation(it) })
            ?: ScreenOrientation.PORTRAIT

        tile.label = getString(R.string.tile_bottom_screen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isMaster) getString(currentOrientation.shortLabelRes) else "Off"
        }
        tile.icon = Icon.createWithResource(this, currentOrientation.iconRes)
        tile.state = if (isMaster) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
