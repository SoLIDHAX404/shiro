package org.solidhax.shiro.utils.ui

import org.joml.Vector2f

data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {

    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun expand(amount: Float): Bounds = Bounds(left - amount, top - amount, right + amount, bottom + amount)

    fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom

    companion object {
        fun of(points: Iterable<Vector2f>): Bounds {
            var left = Float.POSITIVE_INFINITY
            var top = Float.POSITIVE_INFINITY
            var right = Float.NEGATIVE_INFINITY
            var bottom = Float.NEGATIVE_INFINITY
            for (point in points) {
                left = minOf(left, point.x)
                top = minOf(top, point.y)
                right = maxOf(right, point.x)
                bottom = maxOf(bottom, point.y)
            }
            return Bounds(left, top, right, bottom)
        }

        fun centered(centerX: Float, centerY: Float, width: Float, height: Float): Bounds =
            Bounds(centerX - width / 2f, centerY - height / 2f, centerX + width / 2f, centerY + height / 2f)
    }
}

/**
 * Where a label sits around some [Bounds]. [x] and [y] go from 0 to 1: 0 puts the label fully before the left/top edge,
 * 1 fully past the right/bottom edge and 0.5 centers it on that axis, so the label never covers the bounds unless centered.
 */
data class BoxAnchor(val x: Float, val y: Float) {

    fun place(bounds: Bounds, width: Float, height: Float): Bounds = Bounds.centered(
        bounds.left - width / 2f + x * (bounds.width + width),
        bounds.top - height / 2f + y * (bounds.height + height),
        width, height
    )

    companion object {
        val ABOVE = BoxAnchor(0.5f, 0f)

        fun fromCenter(bounds: Bounds, width: Float, height: Float, centerX: Float, centerY: Float): BoxAnchor = BoxAnchor(
            ((centerX - bounds.left + width / 2f) / (bounds.width + width)).coerceIn(0f, 1f),
            ((centerY - bounds.top + height / 2f) / (bounds.height + height)).coerceIn(0f, 1f)
        )
    }
}
