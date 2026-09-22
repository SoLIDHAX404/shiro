package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

@Suppress("UNCHECKED_CAST")
class NumberSetting<E>(
    name: String,
    override val default: E = 1.0 as E,
    min: Number,
    max: Number,
    increment: Number = 1,
    desc: String,
    private val unit: String = ""
) : Setting<E>(name, desc), Saving where E : Number, E : Comparable<E> {

    val incrementDouble = increment.toDouble()
    val minDouble = min.toDouble()
    val maxDouble = max.toDouble()

    override var value: E = default
        set(value) {
            field = convert(roundToIncrement(value).coerceIn(minDouble, maxDouble))
        }

    init {
        value = default
    }

    var valueDouble
        get() = value.toDouble()
        set(value) {
            this.value = value as E
        }

    val percentage: Float
        get() = ((valueDouble - minDouble) / (maxDouble - minDouble)).toFloat()

    val display: String
        get() = formatNumber(valueDouble, unit)

    fun setFromPercentage(percentage: Float) {
        valueDouble = minDouble + percentage.coerceIn(0f, 1f) * (maxDouble - minDouble)
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)

    override fun read(element: JsonElement, gson: Gson) {
        element.asNumber?.let { value = it as E }
    }

    private fun roundToIncrement(x: Number): Double =
        round((x.toDouble() / incrementDouble)) * incrementDouble

    private fun convert(value: Double): E = when (default) {
        is Int -> value.toInt()
        is Long -> value.toLong()
        is Float -> value.toFloat()
        is Short -> value.toInt().toShort()
        is Byte -> value.toInt().toByte()
        else -> value
    } as E
}

fun formatNumber(value: Double, unit: String): String =
    if (value - floor(value) == 0.0) "${value.toLong()}$unit"
    else "${(value * 100.0).roundToInt() / 100.0}$unit"
