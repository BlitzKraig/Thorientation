package com.blitzkraig.thorientation.ui

import android.app.AlertDialog
import android.app.Dialog
import android.service.quicksettings.TileService
import com.blitzkraig.thorientation.engine.RotationEngine
import com.blitzkraig.thorientation.model.ScreenOrientation

object QuickRotationDialogHelper {

    private var activeDialog: Dialog? = null

    fun show(
        service: TileService,
        displayId: Int,
        displayName: String,
        onComplete: () -> Unit = {}
    ) {
        if (service.isLocked) {
            service.unlockAndRun {
                showInternal(service, displayId, displayName, onComplete)
            }
        } else {
            showInternal(service, displayId, displayName, onComplete)
        }
    }

    private fun showInternal(
        service: TileService,
        displayId: Int,
        displayName: String,
        onComplete: () -> Unit
    ) {
        try {
            activeDialog?.dismiss()
        } catch (_: Throwable) {}

        val engine = RotationEngine.get(service)
        val currentRotation = engine.getSavedOrientation(displayId)
            ?: (if (displayId == 0) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT)

        val options = listOf(
            ScreenOrientation.PORTRAIT,
            ScreenOrientation.LANDSCAPE,
            ScreenOrientation.REVERSE_PORTRAIT,
            ScreenOrientation.AUTO
        )

        val itemLabels = options.map { service.getString(it.labelRes) }.toTypedArray()
        val checkedIndex = options.indexOf(currentRotation)

        val builder = AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(displayName)
            .setSingleChoiceItems(itemLabels, checkedIndex) { dialog, which ->
                val selected = options[which]
                engine.setOrientation(displayId, selected)
                onComplete()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(true)

        dialog.setOnDismissListener {
            if (activeDialog == dialog) {
                activeDialog = null
            }
        }

        activeDialog = dialog
        service.showDialog(dialog)
    }

    fun dismiss() {
        try {
            activeDialog?.dismiss()
        } catch (_: Throwable) {}
        activeDialog = null
    }
}
