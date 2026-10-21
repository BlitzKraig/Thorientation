package com.blitzkraig.thorientation.model

import android.content.pm.ActivityInfo
import android.view.Surface
import com.blitzkraig.thorientation.R

enum class ScreenOrientation(
    val degrees: Int,
    val androidValue: Int, // 0=0°, 1=90°, 2=180°, 3=270°, -1=Auto
    val labelRes: Int,
    val shortLabelRes: Int,
    val iconRes: Int
) {
    PORTRAIT(
        degrees = 0,
        androidValue = 0,
        labelRes = R.string.orientation_portrait,
        shortLabelRes = R.string.rot_portrait,
        iconRes = R.drawable.ic_rotate_portrait
    ),
    LANDSCAPE(
        degrees = 90,
        androidValue = 1,
        labelRes = R.string.orientation_landscape,
        shortLabelRes = R.string.rot_landscape,
        iconRes = R.drawable.ic_rotate_landscape
    ),
    REVERSE_PORTRAIT(
        degrees = 180,
        androidValue = 2,
        labelRes = R.string.orientation_reverse_portrait,
        shortLabelRes = R.string.rot_rev_portrait,
        iconRes = R.drawable.ic_rotate_rev_portrait
    ),
    @Deprecated("Reverse Landscape is not supported on this device")
    REVERSE_LANDSCAPE(
        degrees = 270,
        androidValue = 3,
        labelRes = R.string.orientation_landscape,
        shortLabelRes = R.string.rot_landscape,
        iconRes = R.drawable.ic_rotate_landscape
    ),
    AUTO(
        degrees = -1,
        androidValue = -1,
        labelRes = R.string.orientation_auto,
        shortLabelRes = R.string.rot_auto,
        iconRes = R.drawable.ic_rotate_auto
    );

    fun toActivityOrientation(): Int = when (this) {
        PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        REVERSE_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        REVERSE_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    companion object {
        fun fromSurfaceRotation(rotation: Int): ScreenOrientation = when (rotation) {
            Surface.ROTATION_0 -> PORTRAIT
            Surface.ROTATION_90 -> LANDSCAPE
            Surface.ROTATION_180 -> REVERSE_PORTRAIT
            Surface.ROTATION_270 -> LANDSCAPE
            else -> PORTRAIT
        }

        fun fromAndroidValue(value: Int): ScreenOrientation = when (value) {
            0 -> PORTRAIT
            1 -> LANDSCAPE
            2 -> REVERSE_PORTRAIT
            3 -> LANDSCAPE
            else -> AUTO
        }
    }
}
