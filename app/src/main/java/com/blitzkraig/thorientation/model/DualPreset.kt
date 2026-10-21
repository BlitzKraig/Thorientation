package com.blitzkraig.thorientation.model

import com.blitzkraig.thorientation.R

enum class DualPreset(
    val titleRes: Int,
    val descRes: Int,
    val topOrientation: ScreenOrientation,
    val bottomOrientation: ScreenOrientation
) {
    LANDSCAPE(
        titleRes = R.string.preset_landscape,
        descRes = R.string.preset_landscape_desc,
        topOrientation = ScreenOrientation.LANDSCAPE,
        bottomOrientation = ScreenOrientation.LANDSCAPE
    ),
    PORTRAIT(
        titleRes = R.string.preset_portrait,
        descRes = R.string.preset_portrait_desc,
        topOrientation = ScreenOrientation.PORTRAIT,
        bottomOrientation = ScreenOrientation.PORTRAIT
    ),
    PORTRAIT_REVERSE(
        titleRes = R.string.preset_portrait_reverse,
        descRes = R.string.preset_portrait_reverse_desc,
        topOrientation = ScreenOrientation.REVERSE_PORTRAIT,
        bottomOrientation = ScreenOrientation.REVERSE_PORTRAIT
    ),
    AUTO(
        titleRes = R.string.preset_auto,
        descRes = R.string.preset_auto_desc,
        topOrientation = ScreenOrientation.AUTO,
        bottomOrientation = ScreenOrientation.AUTO
    );

    fun next(): DualPreset {
        val values = values()
        val nextIndex = (ordinal + 1) % values.size
        return values[nextIndex]
    }
}
