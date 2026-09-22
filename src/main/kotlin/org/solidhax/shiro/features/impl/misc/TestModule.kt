package org.solidhax.shiro.features.impl.misc

import com.mojang.blaze3d.platform.InputConstants
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

    init {
        +ActionSetting("Action", desc = "An action setting.") { }
    }
}
