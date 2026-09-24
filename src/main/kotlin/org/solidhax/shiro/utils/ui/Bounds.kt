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
 * Which side of some [Bounds] a label sits on, centered along that side and just outside it.
 */
enum class LabelPosition(private val x: Float, private val y: Float) {
    TOP(0.5f, 0f),
    BOTTOM(0.5f, 1f),
    LEFT(0f, 0.5f),
    RIGHT(1f, 0.5f);

    // x and y go from 0 (label fully before the left/top edge) to 1 (fully past the right/bottom edge)
    fun place(bounds: Bounds, width: Float, height: Float): Bounds = Bounds.centered(
        bounds.left - width / 2f + x * (bounds.width + width),
        bounds.top - height / 2f + y * (bounds.height + height),
        width, height
    )
}
