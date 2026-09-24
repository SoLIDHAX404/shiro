package org.solidhax.shiro.features.impl.misc

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import org.solidhax.shiro.features.DevModule
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.ActionSetting
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.KeybindSetting
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.gui.settings.impl.RangeSetting
import org.solidhax.shiro.gui.settings.impl.SelectorSetting
import org.solidhax.shiro.gui.settings.impl.StringSetting
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth

@DevModule
object TestModule : Module(
    name = "Test Module",
    description = "Empty module for testing the module system."
) {
    private val toggle by BooleanSetting("Toggle", true, desc = "A boolean setting.")
    private val number by NumberSetting("Number", 75, 0, 100, 1, desc = "A number setting.")
    private val range by RangeSetting("Range", 20.0..75.0, 0, 100, 1, desc = "A range setting.")
    private val mode by SelectorSetting("Mode", "First", listOf("First", "Second", "Third"), desc = "A selector setting.")
    private val text by StringSetting("Text", "Hello", desc = "A string setting.")
    private val keybind by KeybindSetting("Keybind", InputConstants.KEY_G, "A keybind setting.")
    private val advanced = +DropdownSetting("Advanced")
    private val hidden by BooleanSetting("Hidden", desc = "Only visible when Advanced is open.").withDependency(advanced)
    private val hiddenNumber by NumberSetting("Hidden Number", 3, 0, 10, 1, desc = "Only visible when Advanced is open.").withDependency(advanced)

    private val testHud by HUD("Test HUD", "An example HUD element.") { example ->
        val lines = listOf("Shiro", if (example) "FPS: 120" else "FPS: ${mc.fps}")
        val width = lines.maxOf { textWidth(it) } + HUD_PADDING * 2f
        val height = lines.size * TEXT_SIZE + (lines.size - 1) * HUD_LINE_GAP + HUD_PADDING * 2f

        roundedRectangle(0f, 0f, width, height, HUD_BACKGROUND, Radius.MEDIUM)
        lines.forEachIndexed { index, line -> text(line, HUD_PADDING, HUD_PADDING + index * (TEXT_SIZE + HUD_LINE_GAP), HUD_TEXT) }
        width to height
    }

    private const val HUD_PADDING = 4f
    private const val HUD_LINE_GAP = 2f
    private const val HUD_BACKGROUND = 0x80000000.toInt()
    private val HUD_TEXT = CascadeGeometricColor(0xFFE6E6E6.toInt())

    init {
        +ActionSetting("Action", desc = "An action setting.") { }
    }
}
