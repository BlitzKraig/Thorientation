package com.blitzkraig.thorientation.model

data class DisplayInfoModel(
    val displayId: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val refreshRate: Float,
    val densityDpi: Int,
    val currentRotation: ScreenOrientation,
    val isPrimary: Boolean
) {
    val resolutionFormatted: String
        get() = "${width} × ${height} (${densityDpi} dpi, ${refreshRate.toInt()}Hz)"
}

enum class PrivilegeStatus {
    READY,
    NEEDS_PERMISSION,
    NOT_AVAILABLE
}
