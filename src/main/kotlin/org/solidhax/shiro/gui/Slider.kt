package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.gui.ClickGUI.theme

object Slider {

    const val HEIGHT = 6f

    private val CORNERS = CascadeGeometricRadius(HEIGHT / 2f)

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, start: Float, end: Float, active: Boolean) {
        graphics.roundedRectangle(x, y, width, HEIGHT, if (active) theme.sliderTrackHovered else theme.sliderTrack, CORNERS)

        val startX = x + width * start.coerceIn(0f, 1f)
        val endX = x + width * end.coerceIn(0f, 1f)
        if (endX <= x) return

        val fillWidth = (endX - startX).coerceAtLeast(HEIGHT)
        graphics.roundedRectangle(startX.coerceAtMost(x + width - fillWidth), y, fillWidth, HEIGHT, theme.sliderFill, CORNERS)
    }

    fun percentage(mouseX: Float, x: Float, width: Float): Float = ((mouseX - x) / width).coerceIn(0f, 1f)
}
