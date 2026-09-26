package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

abstract class OverlayScreen(title: String) : Screen(Component.literal(title)) {

    override fun extractBlurredBackground(graphics: GuiGraphicsExtractor) {}

    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}

    override fun isPauseScreen(): Boolean = false
}
