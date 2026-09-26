package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting

class HudSetting(
    name: String,
    hud: HudElement,
    val toggleable: Boolean = false,
    desc: String,
    val module: Module,
) : Setting<HudElement>(name, desc), Saving {

    constructor(
        name: String,
        x: Float,
        y: Float,
        scale: Float,
        toggleable: Boolean,
        desc: String,
        module: Module,
        render: GuiGraphicsExtractor.(example: Boolean) -> Pair<Float, Float>,
    ) : this(name, HudElement(name, x, y, scale, true, render), toggleable, desc, module)

    override val default: HudElement = hud
    override var value: HudElement = default

    val isEnabled: Boolean get() = module.enabled && value.enabled

    override fun reset() = value.reset()

    override fun write(gson: Gson): JsonElement = JsonObject().apply {
        addProperty("x", value.x)
        addProperty("y", value.y)
        addProperty("scale", value.scale)
        addProperty("enabled", value.enabled)
        add("style", JsonObject().apply {
            for (setting in value.settings) if (setting is Saving) add(setting.key, setting.write(gson))
        })
    }

    override fun read(element: JsonElement, gson: Gson) {
        if (element !is JsonObject) return
        value.x = element.get("x")?.asFloat ?: value.x
        value.y = element.get("y")?.asFloat ?: value.y
        value.scale = element.get("scale")?.asFloat ?: value.scale
        value.enabled = if (toggleable) element.get("enabled")?.asBoolean ?: value.enabled else true
        val style = element.get("style") as? JsonObject ?: return
        for (setting in value.settings) {
            if (setting is Saving) style.get(setting.key)?.let { setting.read(it, gson) }
        }
    }
}
