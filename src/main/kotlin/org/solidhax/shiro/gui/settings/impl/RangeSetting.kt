package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting
import kotlin.math.abs
import kotlin.math.round

class RangeSetting(
    name: String,
    override val default: ClosedFloatingPointRange<Double>,
    min: Number,
    max: Number,
    increment: Number = 1,
    desc: String,
    private val unit: String = ""
) : Setting<ClosedFloatingPointRange<Double>>(name, desc), Saving {

    val incrementDouble = increment.toDouble()
    val minDouble = min.toDouble()
    val maxDouble = max.toDouble()

    override var value: ClosedFloatingPointRange<Double> = default
        set(value) {
            val start = snap(value.start)
            val end = snap(value.endInclusive)
            field = minOf(start, end)..maxOf(start, end)
        }

    init {
        value = default
    }

    var lower: Double
        get() = value.start
        set(lower) {
            value = lower.coerceAtMost(upper)..upper
        }

    var upper: Double
        get() = value.endInclusive
        set(upper) {
            value = lower..upper.coerceAtLeast(lower)
        }

    val lowerPercentage: Float get() = percentageOf(lower)
    val upperPercentage: Float get() = percentageOf(upper)

    val display: String
        get() = "${formatNumber(lower, unit)} - ${formatNumber(upper, unit)}"

    fun closerToUpper(percentage: Float): Boolean =
        abs(percentage - upperPercentage) < abs(percentage - lowerPercentage) ||
                (lowerPercentage == upperPercentage && percentage > upperPercentage)

    fun setLowerFromPercentage(percentage: Float) {
        lower = valueAt(percentage)
    }

    fun setUpperFromPercentage(percentage: Float) {
        upper = valueAt(percentage)
    }

    override fun write(gson: Gson): JsonElement = JsonArray().apply {
        add(lower)
        add(upper)
    }

    override fun read(element: JsonElement, gson: Gson) {
        val array = element.asJsonArray ?: return
        if (array.size() != 2) return
        value = array[0].asDouble..array[1].asDouble
    }

    private fun valueAt(percentage: Float): Double =
        minDouble + percentage.coerceIn(0f, 1f) * (maxDouble - minDouble)

    private fun percentageOf(value: Double): Float =
        ((value - minDouble) / (maxDouble - minDouble)).toFloat()

    private fun snap(value: Double): Double =
        (round(value / incrementDouble) * incrementDouble).coerceIn(minDouble, maxDouble)
}
