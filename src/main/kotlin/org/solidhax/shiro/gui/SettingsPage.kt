package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.Page.Companion.PADDING
import org.solidhax.shiro.utils.ui.icon
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth
import kotlin.math.PI

class SettingsPage private constructor(private val list: SettingList) : Page by list {

    constructor() : this(SettingList(topPadding = 0f))

    var module: Module? = null
        set(value) {
            if (value == field) return
            field = value
            list.settings = value?.settings?.values.orEmpty()
            list.reset()
        }

    private var backX = 0f
    private var backY = 0f
    private var backWidth = 0f

    override fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float) {
        val module = module ?: return
        backX = x + PADDING
        backY = y + PADDING
        backWidth = ICON_SIZE + GAP + textWidth(module.name, TITLE_SIZE)

        val color = if (isBackHovered(mouseX, mouseY)) theme.textMuted else theme.text
        graphics.icon(ModuleButton.CHEVRON, backX, backY + (TITLE_SIZE - ICON_SIZE) / 2f, ICON_SIZE, color, PI.toFloat())
        graphics.text(module.name, backX + ICON_SIZE + GAP, backY, color, TITLE_SIZE)

        val listY = backY + TITLE_SIZE + HEADER_GAP
        if (list.settings.none { it.isVisible }) graphics.text(EMPTY, backX, listY, theme.textMuted)
        else list.draw(graphics, x, listY, width, y + height - listY, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && isBackHovered(mouseX, mouseY)) {
            module = null
            return true
        }
        return list.mouseClicked(mouseX, mouseY, button, doubleClick)
    }

    private fun isBackHovered(mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, backX, backY - BACK_GRAB, backWidth, TITLE_SIZE + BACK_GRAB * 2f)

    companion object {
        private const val TITLE_SIZE = 10f
        private const val HEADER_GAP = 10f
        private const val ICON_SIZE = 9f
        private const val GAP = 4f
        private const val BACK_GRAB = 3f
        private const val EMPTY = "This module has no settings."
    }
}
