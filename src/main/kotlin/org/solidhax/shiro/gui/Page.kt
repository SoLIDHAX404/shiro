package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent

interface Page {

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float)

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean = false

    fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float): Boolean = false

    fun mouseReleased(button: Int) {}

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean = false

    fun charTyped(event: CharacterEvent): Boolean = false

    fun keyPressed(event: KeyEvent): Boolean = false

    fun unfocus() {}

    companion object {
        const val PADDING = 10f
        const val SPACING = 6f
    }
}
