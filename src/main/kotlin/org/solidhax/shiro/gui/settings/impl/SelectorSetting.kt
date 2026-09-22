package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting

class SelectorSetting(
    name: String,
    default: String,
    val options: List<String>,
    desc: String
) : Setting<Int>(name, desc), Saving {

    override val default: Int = optionIndex(default)

    override var value: Int
        get() = index
        set(value) {
            index = value
        }

    private var index: Int = optionIndex(default)
        set(value) {
            field = if (value > options.size - 1) 0 else if (value < 0) options.size - 1 else value
        }

    var selected: String
        get() = options[index]
        set(value) {
            index = optionIndex(value)
        }

    private fun optionIndex(string: String): Int =
        options.map { it.lowercase() }.indexOf(string.lowercase()).coerceIn(0, options.size - 1)

    override fun write(gson: Gson): JsonElement = JsonPrimitive(selected)

    override fun read(element: JsonElement, gson: Gson) {
        element.asString?.let { selected = it }
    }
}
