package org.solidhax.shiro.utils.ui

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor

fun lerpColor(from: Int, to: Int, progress: Float): Int {
    val amount = progress.coerceIn(0f, 1f)
    if (amount <= 0f) return from
    if (amount >= 1f) return to

    var result = 0
    for (shift in intArrayOf(24, 16, 8, 0)) {
        val start = (from shr shift) and 0xFF
        val end = (to shr shift) and 0xFF
        result = result or (((start + (end - start) * amount).toInt() and 0xFF) shl shift)
    }
    return result
}

fun lerpColor(from: CascadeGeometricColor, to: CascadeGeometricColor, progress: Float): CascadeGeometricColor =
    CascadeGeometricColor(lerpColor(from.tl, to.tl, progress))

fun CascadeGeometricColor.fade(progress: Float): CascadeGeometricColor = alpha(progress.coerceIn(0f, 1f))
