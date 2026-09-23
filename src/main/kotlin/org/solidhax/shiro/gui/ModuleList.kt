package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Category
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.Page.Companion.SPACING
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth

class ModuleList(private val category: () -> Category, private val search: SearchBar, private val onOpenSettings: (Module) -> Unit) : Page {

    private val scroll = ScrollArea()
    private val buttons = HashMap<Category, List<ModuleButton>>()
    private var current: Pair<Category, String>? = null

    private val currentButtons: List<ModuleButton>
        get() = buttons.getOrPut(category()) { ModuleManager.modulesByCategory[category()].orEmpty().map(::ModuleButton) }
            .filter { search.score(it.module) > 0 }
            .sortedByDescending { search.score(it.module) }

    override fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float) {
        if (category() to search.query != current) {
            current = category() to search.query
            scroll.reset()
        }

        val buttons = currentButtons
        if (buttons.isEmpty()) {
            val empty = if (search.searching) NO_RESULTS else EMPTY
            graphics.text(empty, x + (width - textWidth(empty)) / 2f, y + (height - TEXT_SIZE) / 2f, theme.textMuted)
            return
        }

        val hovered = scroll.isHovered(mouseX, mouseY)
        scroll.draw(graphics, x, y, width, height, buttons.size * (ModuleButton.HEIGHT + SPACING) - SPACING, mouseX, mouseY) {
            buttons.forEachIndexed { index, button ->
                val buttonY = buttonY(index)
                button.draw(graphics, scroll.contentX, buttonY, scroll.contentWidth, hovered && isButtonHovered(buttonY, mouseX, mouseY))
            }
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        if (scroll.mouseClicked(mouseX, mouseY)) return true
        if (!scroll.isHovered(mouseX, mouseY)) return false

        val module = currentButtons.withIndex().firstOrNull { isButtonHovered(buttonY(it.index), mouseX, mouseY) }?.value?.module ?: return false
        when (button) {
            InputConstants.MOUSE_BUTTON_LEFT -> module.toggle()
            InputConstants.MOUSE_BUTTON_RIGHT -> onOpenSettings(module)
            else -> return false
        }
        return true
    }

    override fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float): Boolean = scroll.mouseDragged(deltaY)

    override fun mouseReleased(button: Int) = scroll.mouseReleased()

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean = scroll.mouseScrolled(mouseX, mouseY, amount)

    private fun buttonY(index: Int): Float = scroll.contentY + index * (ModuleButton.HEIGHT + SPACING)

    private fun isButtonHovered(buttonY: Float, mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, scroll.contentX, buttonY, scroll.contentWidth, ModuleButton.HEIGHT)

    companion object {
        private const val EMPTY = "No modules in this category yet."
        private const val NO_RESULTS = "No modules match your search."
    }
}
