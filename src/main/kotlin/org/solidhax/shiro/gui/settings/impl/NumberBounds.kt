package org.solidhax.shiro.gui.settings.impl

import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

class NumberBounds(min: Number, max: Number, increment: Number) {

    val min = min.toDouble()
    val max = max.toDouble()
    val increment = increment.toDouble()

    fun snap(value: Double): Double = (round(value / increment) * increment).coerceIn(min, max)

    fun percentageOf(value: Double): Float = ((value - min) / (max - min)).toFloat()

    fun valueAt(percentage: Float): Double = min + percentage.coerceIn(0f, 1f) * (max - min)
}

fun formatNumber(value: Double, unit: String): String =
    if (value - floor(value) == 0.0) "${value.toLong()}$unit"
    else "${(value * 100.0).roundToInt() / 100.0}$unit"
