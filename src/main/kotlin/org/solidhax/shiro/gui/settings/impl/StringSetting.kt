package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting

class StringSetting(
    name: String,
    override val default: String = "",
    val length: Int = 32,
    desc: String
) : Setting<String>(name, desc), Saving {

    override var value: String = default
        set(value) {
            field = if (value.length <= length) value else return
        }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)

    override fun read(element: JsonElement, gson: Gson) {
        element.asString?.let { value = it }
    }
}
