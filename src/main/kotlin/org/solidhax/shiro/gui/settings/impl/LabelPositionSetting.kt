package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.utils.ui.LabelPosition

/**
 * Stores which side of an entity's bounds a label sits on. It has no row in the settings list; it is set by dragging the
 * label in the module's [PreviewSetting].
 */
class LabelPositionSetting(
    name: String,
    override val default: LabelPosition = LabelPosition.TOP,
    desc: String = ""
) : Setting<LabelPosition>(name, desc), Saving {

    override var value: LabelPosition = default

    init {
        hidden = true
    }

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value.name)

    override fun read(element: JsonElement, gson: Gson) {
        val name = element.takeIf { it.isJsonPrimitive }?.asString ?: return
        value = LabelPosition.entries.find { it.name == name } ?: return
    }
}
