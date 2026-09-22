package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.truncate
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.animation.Easing
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.ui.svgTexture

class ModuleButton(val module: Module, private val width: Float) {

    private val hoverAnimation = Animation(HOVER_DURATION)
    private val toggleAnimation = Animation(TOGGLE_DURATION, Easing.EASE_OUT, if (module.enabled) 1f else 0f)

    private val description by lazy { module.description.truncate(width - TEXT_INSET - CHEVRON_AREA, DESCRIPTION_SIZE) }

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, hovered: Boolean) {
        val hover = hoverAnimation.animate(hovered)
        val toggle = toggleAnimation.animate(module.enabled)

        graphics.roundedRectangle(x, y, width, HEIGHT, lerpColor(theme.entryHovered, theme.entrySelected, hover), CORNERS)
        graphics.hollowRectangle(x, y, width, HEIGHT, 1f, theme.divider, CORNERS)
        if (toggle > 0f) {
            val stripeHeight = HEIGHT * toggle
            graphics.accentEdge(x, y + (HEIGHT - stripeHeight) / 2f, width, stripeHeight, CORNERS, left = true)
        }

        font.extract(graphics, module.name, x + TEXT_INSET, y + NAME_Y, lerpColor(theme.textMuted, theme.text, hover), shadow = false, size = NAME_SIZE)
        font.extract(graphics, description, x + TEXT_INSET, y + DESCRIPTION_Y, theme.textMuted, shadow = false, size = DESCRIPTION_SIZE)

        graphics.image(
            svgTexture(CHEVRON, CHEVRON_SIZE.toInt()),
            x + width - TEXT_INSET - CHEVRON_SIZE, y + (HEIGHT - CHEVRON_SIZE) / 2f,
            CHEVRON_SIZE, CHEVRON_SIZE,
            color = lerpColor(theme.textMuted, theme.text, hover)
        )
    }

    companion object {
        const val HEIGHT = 32f

        private const val TEXT_INSET = 12f
        private const val NAME_SIZE = 9f
        private const val NAME_Y = 7f
        private const val DESCRIPTION_SIZE = 7f
        private const val DESCRIPTION_Y = 18f
        private const val CHEVRON_SIZE = 8f
        private const val CHEVRON_AREA = 30f
        private const val HOVER_DURATION = 120L
        private const val TOGGLE_DURATION = 200L

        val CHEVRON: Identifier = Identifier.fromNamespaceAndPath("shiro", "chevron.svg")
        private val CORNERS = CascadeGeometricRadius(4f)

        private val font get() = CascadeFonts.sans
    }
}
