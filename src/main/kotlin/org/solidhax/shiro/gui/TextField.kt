package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.isAreaHovered

class TextField(getText: () -> String, setText: (String) -> Unit) {

    private val input = TextInput(getText, setText)

    private var x = 0f
    private var y = 0f
    private var width = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float) {
        this.x = x
        this.y = y
        this.width = width

        val active = input.focused || isHovered(mouseX, mouseY)
        graphics.roundedRectangle(x, y, width, HEIGHT, if (active) theme.controlHovered else theme.control, Radius.MEDIUM)
        graphics.hollowRectangle(x, y, width, HEIGHT, 1f, if (input.focused) theme.accent else theme.divider, Radius.MEDIUM)
        input.draw(graphics, x + PADDING, y, width - PADDING * 2f, HEIGHT)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        input.click(mouseX)
        return true
    }

    fun drag(mouseX: Float): Boolean = input.drag(mouseX)

    fun unfocus() = input.unfocus()

    fun charTyped(event: CharacterEvent): Boolean = input.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = input.keyPressed(event)

    private fun isHovered(mouseX: Float, mouseY: Float): Boolean = isAreaHovered(mouseX, mouseY, x, y, width, HEIGHT)

    companion object {
        const val HEIGHT = 15f

        private const val PADDING = 5f
    }
}
