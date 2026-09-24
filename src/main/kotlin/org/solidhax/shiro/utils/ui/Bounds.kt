package org.solidhax.shiro.utils.ui

import org.joml.Vector2f
import kotlin.math.abs

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
 * Where a label sits around some [Bounds]: just outside one [side], [along] that side from 0 (its top/left end) to 1 (its
 * bottom/right end). The ends reach past the corners, so a label can go anywhere around the bounds.
 */
data class LabelPosition(val side: Side, val along: Float) {

    enum class Side { TOP, BOTTOM, LEFT, RIGHT }

    fun place(bounds: Bounds, width: Float, height: Float): Bounds {
        val track = track(bounds, width, height)
        return when (side) {
            Side.TOP -> Bounds.centered(lerp(track.left, track.right, along), track.top, width, height)
            Side.BOTTOM -> Bounds.centered(lerp(track.left, track.right, along), track.bottom, width, height)
            Side.LEFT -> Bounds.centered(track.left, lerp(track.top, track.bottom, along), width, height)
            Side.RIGHT -> Bounds.centered(track.right, lerp(track.top, track.bottom, along), width, height)
        }
    }

    companion object {
        val TOP = LabelPosition(Side.TOP, 0.5f)

        /**
         * The position whose label is centered closest to ([x], [y]), pulled to the middle of its side when within [snap].
         */
        fun nearest(bounds: Bounds, width: Float, height: Float, x: Float, y: Float, snap: Float = 0f): LabelPosition {
            val track = track(bounds, width, height)
            val trackX = x.coerceIn(track.left, track.right)
            val trackY = y.coerceIn(track.top, track.bottom)
            val side = listOf(
                Side.TOP to trackY - track.top,
                Side.BOTTOM to track.bottom - trackY,
                Side.LEFT to trackX - track.left,
                Side.RIGHT to track.right - trackX,
            ).minBy { it.second }.first

            val horizontal = side == Side.TOP || side == Side.BOTTOM
            val start = if (horizontal) track.left else track.top
            val end = if (horizontal) track.right else track.bottom
            val value = if (horizontal) trackX else trackY
            if (abs(value - (start + end) / 2f) <= snap) return LabelPosition(side, 0.5f)
            return LabelPosition(side, ((value - start) / (end - start)).coerceIn(0f, 1f))
        }

        // the path a label's center follows: the bounds grown by half the label, so the label always just touches them
        private fun track(bounds: Bounds, width: Float, height: Float): Bounds =
            Bounds(bounds.left - width / 2f, bounds.top - height / 2f, bounds.right + width / 2f, bounds.bottom + height / 2f)

        private fun lerp(start: Float, end: Float, amount: Float): Float = start + (end - start) * amount
    }
}
