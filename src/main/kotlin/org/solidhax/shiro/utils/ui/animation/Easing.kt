package org.solidhax.shiro.utils.ui.animation

import kotlin.math.pow

enum class Easing(private val curve: (Float) -> Float) {
    LINEAR({ it }),
    EASE_OUT({ 1f - (1f - it).pow(3) }),
    EASE_IN_OUT({ if (it < 0.5f) 4f * it * it * it else 1f - (-2f * it + 2f).pow(3) / 2f }),
    EASE_OUT_BACK({ 1f + 2.70158f * (it - 1f).pow(3) + 1.70158f * (it - 1f).pow(2) });

    fun apply(progress: Float): Float = curve(progress.coerceIn(0f, 1f))
}
