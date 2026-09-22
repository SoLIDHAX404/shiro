package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.font.CascadeFonts
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.svgTexture

class SettingsPage(private val onBack: () -> Unit) {

    private val scrollbar = Scrollbar()
    private val cards = SettingCards()

    private var x = 0f
    private var y = 0f
    private var width = 0f
    private var height = 0f
    private var backWidth = 0f
    private var module: Module? = null

    fun draw(graphics: GuiGraphicsExtractor, module: Module, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float) {
        if (module != this.module) {
            this.module = module
            scrollbar.reset()
        }
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        backWidth = BACK_ICON_SIZE + BACK_GAP + font.width(module.name, TITLE_SIZE)

        drawHeader(graphics, module, mouseX, mouseY)

        val settings = visibleSettings(module)
        if (settings.isEmpty()) {
            font.extract(graphics, EMPTY, cardX, listY, theme.textMuted, shadow = false, size = TEXT_SIZE)
            return
        }

        graphics.scissor(x, listY, width, listHeight) {
            cards.forEachCard(settings, listY - scrollbar.offset, SPACING) { setting, cardY, corners ->
                if (cardY + cards.height(setting) >= listY && cardY <= listY + listHeight) {
                    cards.draw(graphics, setting, cardX, cardY, cardWidth, mouseX, mouseY, corners)
                }
            }
        }

        scrollbar.draw(graphics, x + width - SCROLLBAR_INSET, y, height, contentHeight(settings), mouseX, mouseY)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            cards.unfocus()
            if (isBackHovered(mouseX, mouseY)) {
                onBack()
                return true
            }
            if (scrollbar.mouseClicked(mouseX, mouseY)) return true
        }

        val module = module ?: return false
        if (!isAreaHovered(mouseX, mouseY, x, listY, width, listHeight)) return false

        cards.forEachCard(visibleSettings(module), listY - scrollbar.offset, SPACING) { setting, cardY, _ ->
            if (cards.mouseClicked(setting, cardX, cardY, cardWidth, mouseX, mouseY, button)) return true
        }
        return false
    }

    fun mouseDragged(mouseX: Float, mouseY: Float, deltaY: Float): Boolean {
        if (scrollbar.mouseDragged(deltaY)) return true
        return cards.mouseDragged(mouseX, mouseY)
    }

    fun mouseReleased(button: Int) {
        cards.mouseReleased(button)
        if (button == InputConstants.MOUSE_BUTTON_LEFT) scrollbar.mouseReleased()
    }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (!isAreaHovered(mouseX, mouseY, x, y, width, height)) return false
        return scrollbar.scroll(amount)
    }

    fun charTyped(event: CharacterEvent): Boolean = cards.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = cards.keyPressed(event)

    fun unfocus() {
        cards.unfocus()
    }

    private fun drawHeader(graphics: GuiGraphicsExtractor, module: Module, mouseX: Float, mouseY: Float) {
        val color = if (isBackHovered(mouseX, mouseY)) theme.textMuted else theme.text
        val titleY = y + PADDING

        graphics.image(
            svgTexture(ModuleButton.CHEVRON, BACK_ICON_SIZE.toInt()),
            cardX, titleY + (TITLE_SIZE - BACK_ICON_SIZE) / 2f,
            BACK_ICON_SIZE, BACK_ICON_SIZE,
            u0 = 1f, u1 = 0f,
            color = color
        )
        font.extract(graphics, module.name, cardX + BACK_ICON_SIZE + BACK_GAP, titleY, color, shadow = false, size = TITLE_SIZE)
    }

    private fun visibleSettings(module: Module): List<Setting<*>> = module.settings.values.filter { it.isVisible }

    private fun contentHeight(settings: List<Setting<*>>): Float = (listY - y) + cards.totalHeight(settings, SPACING) + PADDING

    private fun isBackHovered(mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, cardX, y + PADDING - BACK_GRAB, backWidth, TITLE_SIZE + BACK_GRAB * 2f)

    private val listY get() = y + PADDING + TITLE_SIZE + HEADER_GAP
    private val listHeight get() = height - (listY - y)
    private val cardX get() = x + PADDING
    private val cardWidth get() = width - PADDING * 2f - SCROLLBAR_SPACE

    companion object {
        private const val PADDING = 10f
        private const val SPACING = 6f
        private const val TEXT_SIZE = 8f
        private const val TITLE_SIZE = 10f
        private const val HEADER_GAP = 10f
        private const val BACK_ICON_SIZE = 9f
        private const val BACK_GAP = 4f
        private const val BACK_GRAB = 3f
        private const val SCROLLBAR_INSET = 4f
        private const val SCROLLBAR_SPACE = 6f
        private const val EMPTY = "This module has no settings."

        private val font get() = CascadeFonts.sans
    }
}
