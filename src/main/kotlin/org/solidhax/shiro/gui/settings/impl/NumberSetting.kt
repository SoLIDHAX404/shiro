package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting

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

    private val bounds = NumberBounds(min, max, increment)

    override var value: E = default
        set(value) {
            field = convert(bounds.snap(value.toDouble()))
        }

    init {
        value = default
    }

    val percentage: Float
        get() = bounds.percentageOf(value.toDouble())

    val display: String
        get() = formatNumber(value.toDouble(), unit)

    fun setFromPercentage(percentage: Float) {
        value = bounds.valueAt(percentage) as E
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)

    override fun read(element: JsonElement, gson: Gson) {
        element.asNumber?.let { value = it as E }
    }

    private fun convert(value: Double): E = when (default) {
        is Int -> value.toInt()
        is Long -> value.toLong()
        is Float -> value.toFloat()
        is Short -> value.toInt().toShort()
        is Byte -> value.toInt().toByte()
        else -> value
    } as E
}
