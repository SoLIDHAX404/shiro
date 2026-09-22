package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.svgTexture

object Checkbox {

    const val SIZE = 12f

    private const val CHECK_SIZE = 8f
    private const val UNCHECKED_ALPHA = 0.35f

    private val CHECK: Identifier = Identifier.fromNamespaceAndPath("shiro", "check.svg")
    private val CORNERS = CascadeGeometricRadius(3f)

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, checked: Boolean, hovered: Boolean) {
        if (checked) {
            graphics.roundedRectangle(x, y, SIZE, SIZE, theme.accent, CORNERS)
        } else {
            graphics.roundedRectangle(x, y, SIZE, SIZE, if (hovered) theme.controlHovered else theme.control, CORNERS)
            graphics.hollowRectangle(x, y, SIZE, SIZE, 1f, theme.divider, CORNERS)
        }

        val checkColor: CascadeGeometricColor = if (checked) theme.text else theme.textMuted.alpha(UNCHECKED_ALPHA)
        val offset = (SIZE - CHECK_SIZE) / 2f
        graphics.image(svgTexture(CHECK, CHECK_SIZE.toInt()), x + offset, y + offset, CHECK_SIZE, CHECK_SIZE, color = checkColor)
    }
}
