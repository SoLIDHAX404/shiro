package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.shiroId
import org.solidhax.shiro.utils.truncate
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.accentEdge
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.icon
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.ui.outlinedRectangle
import org.solidhax.shiro.utils.ui.text

class ModuleButton(val module: Module) {

    private val hoverAnimation = Animation()
    private val toggleAnimation = Animation(TOGGLE_DURATION, if (module.enabled) 1f else 0f)

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, hovered: Boolean) {
        val hover = hoverAnimation.animate(hovered)
        val toggle = toggleAnimation.animate(module.enabled)
        val color = theme.textHover(hover)

        graphics.outlinedRectangle(x, y, width, HEIGHT, lerpColor(theme.entryHovered, theme.entrySelected, hover), theme.divider, Radius.LARGE)
        if (toggle > 0f) graphics.accentEdge(x, y + HEIGHT * (1f - toggle) / 2f, width, HEIGHT * toggle, Radius.LARGE, theme.accent, left = true)

        graphics.text(module.name, x + INSET, y + NAME_Y, color, NAME_SIZE)
        graphics.text(module.description.truncate(width - INSET - CHEVRON_AREA, DESCRIPTION_SIZE), x + INSET, y + DESCRIPTION_Y, theme.textMuted, DESCRIPTION_SIZE)
        graphics.icon(CHEVRON, x + width - INSET - CHEVRON_SIZE, y + (HEIGHT - CHEVRON_SIZE) / 2f, CHEVRON_SIZE, color)
    }

    companion object {
        const val HEIGHT = 32f

        private const val INSET = 12f
        private const val NAME_SIZE = 9f
        private const val NAME_Y = 7f
        private const val DESCRIPTION_SIZE = 7f
        private const val DESCRIPTION_Y = 18f
        private const val CHEVRON_SIZE = 8f
        private const val CHEVRON_AREA = 30f
        private const val TOGGLE_DURATION = 200L

        val CHEVRON: Identifier = shiroId("chevron.svg")
    }
}
