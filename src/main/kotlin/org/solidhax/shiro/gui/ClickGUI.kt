package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object ClickGUI : Screen(Component.literal("Shiro Click GUI")) {

    var theme = Theme.DEFAULT

    private val panel = Panel("shiro")

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        panel.draw(graphics, (width - Panel.WIDTH) / 2f, (height - Panel.HEIGHT) / 2f)
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks)
    }

    override fun extractBlurredBackground(graphics: GuiGraphicsExtractor) {}
    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}
    override fun isPauseScreen(): Boolean = false
}
