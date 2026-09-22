package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.gui.ClickGUI.theme

const val ACCENT_WIDTH = 2f

fun GuiGraphicsExtractor.accentEdge(x: Float, y: Float, width: Float, height: Float, radius: CascadeGeometricRadius, left: Boolean) {
    val edgeX = if (left) x else x + width - ACCENT_WIDTH
    scissor(edgeX, y, ACCENT_WIDTH, height) {
        roundedRectangle(x, y, width, height, theme.accent, radius)
    }
}
