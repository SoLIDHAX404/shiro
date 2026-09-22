package org.solidhax.shiro.utils.ui.animation

class Animation(
    private val duration: Long,
    private val easing: Easing = Easing.EASE_OUT,
    initial: Float = 0f
) {

    private var from = initial
    private var to = initial
    private var startTime = AnimationManager.time - duration

    val value: Float
        get() {
            val elapsed = AnimationManager.time - startTime
            if (elapsed >= duration || duration <= 0L) return to
            return from + (to - from) * easing.apply(elapsed / duration.toFloat())
        }

    val animating: Boolean
        get() = AnimationManager.time - startTime < duration

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
