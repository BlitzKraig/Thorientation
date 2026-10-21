package com.blitzkraig.thorientation.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.blitzkraig.thorientation.engine.RotationEngine
import com.blitzkraig.thorientation.model.DualPreset
import com.blitzkraig.thorientation.model.ScreenOrientation
import com.blitzkraig.thorientation.service.PersistentNotificationHelper

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val engine = RotationEngine.get(context)

        when (action) {
            ACTION_SET_PRESET -> {
                val presetName = intent.getStringExtra(EXTRA_PRESET) ?: return
                try {
                    val preset = DualPreset.valueOf(presetName)
                    val ok = engine.applyPreset(preset)
                    if (ok) {
                        Toast.makeText(
                            context,
                            "Applied: ${context.getString(preset.titleRes)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (_: Throwable) {}
            }
            ACTION_SET_ORIENTATION -> {
                val displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, 0)
                val orientationOrdinal = intent.getIntExtra(EXTRA_ORIENTATION, -1)
                val values = ScreenOrientation.values()
                if (orientationOrdinal in values.indices) {
                    val orientation = values[orientationOrdinal]
                    val ok = engine.setOrientation(displayId, orientation)
                    if (ok) {
                        val screenName = if (displayId == 0) "Top Screen" else "Bottom Screen"
                        Toast.makeText(
                            context,
                            "$screenName: ${context.getString(orientation.shortLabelRes)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        PersistentNotificationHelper.update(context)
    }

    companion object {
        const val ACTION_SET_PRESET = "com.blitzkraig.thorientation.action.NOTIF_SET_PRESET"
        const val ACTION_SET_ORIENTATION = "com.blitzkraig.thorientation.action.NOTIF_SET_ORIENTATION"

        const val EXTRA_PRESET = "extra_preset"
        const val EXTRA_DISPLAY_ID = "extra_display_id"
        const val EXTRA_ORIENTATION = "extra_orientation"
    }
}
