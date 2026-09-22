package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.font.CascadeFonts
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Category
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.isAreaHovered

class ModuleList(private val onOpenSettings: (Module) -> Unit) {

    private val buttons = HashMap<Category, List<ModuleButton>>()
    private val scrollbar = Scrollbar()
    private var category: Category? = null

    private var x = 0f
    private var y = 0f
    private var width = 0f
    private var height = 0f

    fun draw(graphics: GuiGraphicsExtractor, category: Category, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float) {
        if (category != this.category) {
            this.category = category
            scrollbar.reset()
        }
        this.x = x
        this.y = y
        this.width = width
        this.height = height

        val buttons = buttonsFor(category)
        if (buttons.isEmpty()) {
            val emptyWidth = font.width(EMPTY, TEXT_SIZE)
            font.extract(graphics, EMPTY, x + (width - emptyWidth) / 2f, y + (height - TEXT_SIZE) / 2f, theme.textMuted, shadow = false, size = TEXT_SIZE)
            return
        }

        val listHovered = isAreaHovered(mouseX, mouseY, x, y, width, height)
        graphics.scissor(x, y, width, height) {
            buttons.forEachIndexed { index, button ->
                val buttonY = buttonY(index)
                if (buttonY + ModuleButton.HEIGHT < y || buttonY > y + height) return@forEachIndexed
                val hovered = listHovered && isAreaHovered(mouseX, mouseY, x + PADDING, buttonY, buttonWidth, ModuleButton.HEIGHT)
                button.draw(graphics, x + PADDING, buttonY, hovered)
            }
        }

        scrollbar.draw(graphics, x + width - SCROLLBAR_INSET, y, height, contentHeight(buttons.size), mouseX, mouseY)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        val category = category ?: return false
        if (scrollbar.mouseClicked(mouseX, mouseY)) return true
        if (!isAreaHovered(mouseX, mouseY, x, y, width, height)) return false

        buttonsFor(category).forEachIndexed { index, moduleButton ->
            if (!isAreaHovered(mouseX, mouseY, x + PADDING, buttonY(index), buttonWidth, ModuleButton.HEIGHT)) return@forEachIndexed
            when (button) {
                InputConstants.MOUSE_BUTTON_LEFT -> moduleButton.module.toggle()
                InputConstants.MOUSE_BUTTON_RIGHT -> onOpenSettings(moduleButton.module)
                else -> return false
            }
            return true
        }
        return false
    }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (!isAreaHovered(mouseX, mouseY, x, y, width, height)) return false
        return scrollbar.scroll(amount)
    }

    fun mouseDragged(deltaY: Float): Boolean = scrollbar.mouseDragged(deltaY)

    fun mouseReleased() {
        scrollbar.mouseReleased()
    }

    private fun contentHeight(count: Int): Float =
        if (count == 0) 0f else PADDING * 2f + count * ModuleButton.HEIGHT + (count - 1) * SPACING

    private fun buttonsFor(category: Category): List<ModuleButton> =
        buttons.getOrPut(category) {
            ModuleManager.modulesByCategory[category].orEmpty().map { ModuleButton(it, buttonWidth) }
        }

    private fun buttonY(index: Int): Float = y + PADDING + index * (ModuleButton.HEIGHT + SPACING) - scrollbar.offset

    private val buttonWidth get() = Panel.CONTENT_WIDTH - PADDING * 2f - SCROLLBAR_SPACE

    companion object {
        private const val PADDING = 10f
        private const val SPACING = 6f
        private const val SCROLLBAR_INSET = 4f
        private const val SCROLLBAR_SPACE = 6f
        private const val TEXT_SIZE = 8f
        private const val EMPTY = "No modules in this category yet."

        private val font get() = CascadeFonts.sans
    }
}
