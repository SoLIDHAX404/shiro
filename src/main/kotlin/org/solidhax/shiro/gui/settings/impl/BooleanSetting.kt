package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting

class BooleanSetting(
    name: String,
    override val default: Boolean = false,
    desc: String,
) : Setting<Boolean>(name, desc), Saving {

    override var value: Boolean = default
    var enabled: Boolean by this::value

    override fun write(gson: Gson): JsonElement = JsonPrimitive(enabled)

    override fun read(element: JsonElement, gson: Gson) {
        enabled = element.asBoolean
    }
}
