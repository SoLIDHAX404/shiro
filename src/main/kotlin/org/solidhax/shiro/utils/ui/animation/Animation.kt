package org.solidhax.shiro.utils.ui.animation

import net.minecraft.util.Util
import kotlin.math.pow

const val HOVER_DURATION = 120L

object AnimationManager {

    var time = Util.getMillis()
        private set

    /**
     * Refreshes [time]. Safe to call more than once per frame, since both the HUD and an open screen call it.
     */
    fun update() {
        time = Util.getMillis()
    }
}

class Animation(private val duration: Long = HOVER_DURATION, initial: Float = 0f) {

    private var from = initial
    private var to = initial
    private var startTime = AnimationManager.time - duration

    val value: Float
        get() {
            val progress = (AnimationManager.time - startTime) / duration.toFloat()
            if (progress >= 1f) return to
            return from + (to - from) * (1f - (1f - progress).pow(3))
        }

    fun animateTo(target: Float): Float {
        if (target != to) {
            from = value
            to = target
            startTime = AnimationManager.time
        }
        return value
    }

    fun animate(state: Boolean): Float = animateTo(if (state) 1f else 0f)

    fun set(target: Float) {
        from = target
        to = target
        startTime = AnimationManager.time - duration
    }

    fun restart() {
        from = 0f
        to = 1f
        startTime = AnimationManager.time
    }
}

class Animations<K>(private val duration: Long = HOVER_DURATION, private val initial: Float = 0f) {

    private val animations = HashMap<K, Animation>()

    operator fun get(key: K): Animation = animations.getOrPut(key) { Animation(duration, initial) }
}
