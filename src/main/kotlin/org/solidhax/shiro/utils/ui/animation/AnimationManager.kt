package org.solidhax.shiro.utils.ui.animation

import net.minecraft.util.Util

object AnimationManager {

    var time = Util.getMillis()
        private set

    var deltaSeconds = 0f
        private set

    fun update() {
        val now = Util.getMillis()
        deltaSeconds = ((now - time) / 1000f).coerceIn(0f, MAX_DELTA)
        time = now
    }

    fun animation(duration: Long, easing: Easing = Easing.EASE_OUT, initial: Float = 0f): Animation =
        Animation(duration, easing, initial)

    private const val MAX_DELTA = 0.1f
}
