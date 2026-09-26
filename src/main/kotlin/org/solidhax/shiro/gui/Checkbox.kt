package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.shiroId
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.ui.svgTexture

object Checkbox {

    const val SIZE = 12f

    private const val CHECK_SIZE = 8f
    private const val UNCHECKED_ALPHA = 0.35f
    private const val CHECK_MIN_SCALE = 0.75f

    private val CHECK = shiroId("check.svg")

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, hovered: Boolean, progress: Float) {
        graphics.roundedRectangle(x, y, SIZE, SIZE, lerpColor(if (hovered) theme.controlHovered else theme.control, theme.accent, progress), Radius.MEDIUM)
        if (progress < 1f) graphics.hollowRectangle(x, y, SIZE, SIZE, 1f, lerpColor(theme.divider, 0, progress), Radius.MEDIUM)

        val size = CHECK_SIZE * (CHECK_MIN_SCALE + (1f - CHECK_MIN_SCALE) * progress)
        val offset = (SIZE - size) / 2f
        graphics.image(
            svgTexture(CHECK, CHECK_SIZE.toInt()), x + offset, y + offset, size, size,
            color = lerpColor(theme.textMuted.alpha(UNCHECKED_ALPHA), theme.text, progress)
        )
    }
}
