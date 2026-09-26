package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting
import kotlin.math.abs

class RangeSetting(
    name: String,
    override val default: ClosedFloatingPointRange<Double>,
    min: Number,
    max: Number,
    increment: Number = 1,
    desc: String,
    private val unit: String = ""
) : Setting<ClosedFloatingPointRange<Double>>(name, desc), Saving {

    private val bounds = NumberBounds(min, max, increment)

    override var value: ClosedFloatingPointRange<Double> = default
        set(value) {
            val start = bounds.snap(value.start)
            val end = bounds.snap(value.endInclusive)
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

    val lowerPercentage: Float get() = bounds.percentageOf(lower)
    val upperPercentage: Float get() = bounds.percentageOf(upper)

    val display: String
        get() = "${formatNumber(lower, unit)} - ${formatNumber(upper, unit)}"

    fun closerToUpper(percentage: Float): Boolean =
        abs(percentage - upperPercentage) < abs(percentage - lowerPercentage) ||
                (lowerPercentage == upperPercentage && percentage > upperPercentage)

    fun setLowerFromPercentage(percentage: Float) {
        lower = bounds.valueAt(percentage)
    }

    fun setUpperFromPercentage(percentage: Float) {
        upper = bounds.valueAt(percentage)
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
}
