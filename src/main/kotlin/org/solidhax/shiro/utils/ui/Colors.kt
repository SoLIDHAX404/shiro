package org.solidhax.shiro.utils.ui

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor

const val WHITE = 0xFFFFFFFF.toInt()
const val BLACK = 0xFF000000.toInt()
const val TRANSPARENT = 0x00000000

fun withAlpha(color: Int, alpha: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (color and 0xFFFFFF)

fun lerpColor(from: Int, to: Int, progress: Float): Int {
    val amount = progress.coerceIn(0f, 1f)
    var result = 0
    for (shift in 0..24 step 8) {
        val start = (from shr shift) and 0xFF
        val end = (to shr shift) and 0xFF
        result = result or (((start + (end - start) * amount).toInt() and 0xFF) shl shift)
    }
    return result
}

fun lerpColor(from: CascadeGeometricColor, to: CascadeGeometricColor, progress: Float): CascadeGeometricColor =
    CascadeGeometricColor(lerpColor(from.tl, to.tl, progress))

fun fade(color: Int, alpha: Float): Int = lerpColor(withAlpha(color, 0), color, alpha)
