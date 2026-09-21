package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object ClickGUI : Screen(Component.literal("Shiro Click GUI")) {

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)

        val panelWidth = 200f
        val panelHeight = 120f

        graphics.roundedRectangle(
            (width - panelWidth) / 2f,
            (height - panelHeight) / 2f,
            panelWidth,
            panelHeight,
            0xE01E1E2E.toInt(),
            CascadeGeometricRadius(10f)
        )
    }

}
