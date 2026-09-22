package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.platform.InputConstants
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting

class KeybindSetting(
    name: String,
    override val default: InputConstants.Key,
    desc: String
) : Setting<InputConstants.Key>(name, desc), Saving {

    constructor(name: String, defaultKeyCode: Int, desc: String = "") : this(name, InputConstants.Type.KEYBOARD.getOrCreate(defaultKeyCode), desc)

    override var value: InputConstants.Key = default
    var onPress: (() -> Unit)? = null

    fun onPress(block: () -> Unit): KeybindSetting {
        onPress = block
        return this
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value.name)

    override fun read(element: JsonElement, gson: Gson) {
        element.asString?.let { value = InputConstants.getKey(it) }
    }
}
