package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.utils.ui.LabelPosition

/**
 * Stores where a label sits around an entity's bounds. It has no row in the settings list; it is set by dragging the
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

    override fun write(gson: Gson): JsonElement = JsonObject().apply {
        addProperty("side", value.side.name)
        addProperty("along", value.along)
    }

    override fun read(element: JsonElement, gson: Gson) {
        // older configs saved only the side as a string, which now means the middle of that side
        val obj = element.takeIf { it.isJsonObject }?.asJsonObject
        val sideName = obj?.get("side")?.asString ?: element.takeIf { it.isJsonPrimitive }?.asString ?: return
        val side = LabelPosition.Side.entries.find { it.name == sideName } ?: return
        value = LabelPosition(side, obj?.get("along")?.asFloat?.coerceIn(0f, 1f) ?: 0.5f)
    }
}
