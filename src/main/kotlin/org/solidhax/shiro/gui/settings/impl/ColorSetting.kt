package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting
import java.awt.Color

class ColorSetting(
    name: String,
    override val default: Int,
    desc: String
) : Setting<Int>(name, desc), Saving {

    var hue = 0f
        private set

    var saturation = 0f
        private set

    var brightness = 1f
        private set

    var expanded = false

    override var value: Int
        get() = Color.HSBtoRGB(hue, saturation, brightness)
        set(value) {
            val components = Color.RGBtoHSB((value shr 16) and 0xFF, (value shr 8) and 0xFF, value and 0xFF, null)
            hue = components[0]
            saturation = components[1]
            brightness = components[2]
        }

    init {
        value = default
    }

    fun setHue(hue: Float) {
        this.hue = hue.coerceIn(0f, 1f)
    }

    fun setSaturationBrightness(saturation: Float, brightness: Float) {
        this.saturation = saturation.coerceIn(0f, 1f)
        this.brightness = brightness.coerceIn(0f, 1f)
    }

    val hueColor: Int get() = Color.HSBtoRGB(hue, 1f, 1f)

    override fun write(gson: Gson): JsonElement = JsonPrimitive(value)

    override fun read(element: JsonElement, gson: Gson) {
        value = element.asInt
    }
}
