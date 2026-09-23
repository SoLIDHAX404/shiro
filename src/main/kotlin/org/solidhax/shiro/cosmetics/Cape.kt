package org.solidhax.shiro.cosmetics

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.minecraft.resources.Identifier
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting

enum class Cape(val displayName: String, file: String?) {
    NONE("None", null),
    MINECON13("Minecon 2013", "minecon13"),
    MINECON15("Minecon 2015", "minecon15"),
    MINECON16("Minecon 2016", "minecon16");

    val texture: Identifier? = file?.let { Identifier.fromNamespaceAndPath("shiro", "textures/cape/$it.png") }
}

class CapeSetting(name: String, desc: String = "") : Setting<Cape>(name, desc), Saving {

    override val default = Cape.NONE

    override var value = default

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value.name)

    override fun read(element: JsonElement, gson: Gson) {
        value = Cape.entries.firstOrNull { it.name == element.asString } ?: default
    }
}
