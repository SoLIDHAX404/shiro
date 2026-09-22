package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.floor

object ClickGUI : Screen(Component.literal("Shiro Click GUI")) {

    var theme = Theme.DEFAULT

    private val panel = Panel("shiro")

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        panel.draw(graphics, panelX, panelY, mouseX.toFloat(), mouseY.toFloat())
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (panel.mouseClicked(event.x().toFloat(), event.y().toFloat(), event.button(), doubleClick)) return true
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (panel.mouseDragged(event.x().toFloat(), event.button(), deltaX.toFloat(), deltaY.toFloat())) return true
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        panel.mouseReleased(event.button())
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (panel.mouseScrolled(mouseX.toFloat(), mouseY.toFloat(), verticalAmount.toFloat())) return true
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (panel.charTyped(event)) return true
        return super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (panel.keyPressed(event)) return true
        return super.keyPressed(event)
    }

    override fun removed() {
        panel.unfocus()
        super.removed()
    }

    override fun extractBlurredBackground(graphics: GuiGraphicsExtractor) {}
    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}
    override fun isPauseScreen(): Boolean = false

    private val panelX get() = floor((width - Panel.WIDTH) / 2f)
    private val panelY get() = floor((height - Panel.HEIGHT) / 2f)
}
