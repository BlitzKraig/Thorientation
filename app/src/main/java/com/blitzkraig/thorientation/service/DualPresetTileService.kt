package com.blitzkraig.thorientation.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.blitzkraig.thorientation.R
import com.blitzkraig.thorientation.engine.RotationEngine
import com.blitzkraig.thorientation.model.DualPreset

class DualPresetTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val engine = RotationEngine.get(this)
        val currentPreset = engine.getActivePreset()
        val nextPreset = currentPreset.next()

        engine.applyPreset(nextPreset)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val engine = RotationEngine.get(this)
        val activePreset = engine.getActivePreset()

        tile.label = getString(R.string.tile_dual_presets)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(activePreset.titleRes)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_dual_screen)
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }
}
