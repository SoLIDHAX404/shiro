package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.utils.ui.BoxAnchor

/**
 * Stores where a label sits around an entity's bounds. It has no row in the settings list; it is set by dragging the
 * label in the module's [PreviewSetting].
 */
class AnchorSetting(
    name: String,
    override val default: BoxAnchor = BoxAnchor.ABOVE,
    desc: String = ""
) : Setting<BoxAnchor>(name, desc), Saving {

    override var value: BoxAnchor = default

    init {
        hidden = true
    }

    override fun write(gson: Gson): JsonElement = JsonObject().apply {
        addProperty("x", value.x)
        addProperty("y", value.y)
    }

    override fun read(element: JsonElement, gson: Gson) {
        val obj = element.asJsonObject
        value = BoxAnchor(obj.get("x").asFloat.coerceIn(0f, 1f), obj.get("y").asFloat.coerceIn(0f, 1f))
    }
}
