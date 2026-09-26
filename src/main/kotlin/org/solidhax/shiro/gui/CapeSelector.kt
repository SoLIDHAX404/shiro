package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.states.impl.image.data.CascadeImageFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.cosmetics.Cape
import org.solidhax.shiro.cosmetics.CapeSetting
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.animation.Animations
import org.solidhax.shiro.utils.ui.centeredText
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.ui.outlinedRectangle

class CapeSelector(private val setting: CapeSetting) {

    private val hoverAnimations = Animations<Cape>()
    private val selectAnimations = Animations<Cape>(SELECT_DURATION)

    private var x = 0f
    private var y = 0f
    private var width = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float) {
        this.x = x
        this.y = y
        this.width = width

        Cape.entries.forEach { cape ->
            val swatchX = swatchX(cape)
            val selected = setting.value == cape
            val hover = hoverAnimations[cape].animate(!selected && isAreaHovered(mouseX, mouseY, swatchX, y, swatchWidth, HEIGHT))

            graphics.outlinedRectangle(swatchX, y, swatchWidth, HEIGHT, theme.controlHover(hover), lerpColor(theme.divider, theme.accent, selectAnimations[cape].animate(selected)), Radius.LARGE)

            val texture = cape.texture
            if (texture == null) {
                graphics.centeredText(cape.displayName, swatchX + swatchWidth / 2f, y + (HEIGHT - TEXT_SIZE) / 2f, if (selected) theme.text else theme.textMuted, TEXT_SIZE)
            } else {
                graphics.image(
                    texture, swatchX + (swatchWidth - IMAGE_WIDTH) / 2f, y + INSET, IMAGE_WIDTH, IMAGE_HEIGHT,
                    CAPE_U0, CAPE_V0, CAPE_U1, CAPE_V1,
                    radius = Radius.SMALL,
                    filter = CascadeImageFilter.NEAREST
                )
            }
        }
    }

    fun mouseClicked(mouseX: Float, mouseY: Float): Boolean {
        setting.value = Cape.entries.firstOrNull { isAreaHovered(mouseX, mouseY, swatchX(it), y, swatchWidth, HEIGHT) } ?: return false
        return true
    }

    private fun swatchX(cape: Cape): Float = x + cape.ordinal * (swatchWidth + SPACING)

    private val swatchWidth get() = (width - SPACING * (Cape.entries.size - 1)) / Cape.entries.size

    companion object {
        const val HEIGHT = 32f

        private const val SPACING = 5f
        private const val INSET = 4f
        private const val TEXT_SIZE = 7f
        private const val SELECT_DURATION = 150L
        private const val IMAGE_HEIGHT = HEIGHT - INSET * 2f
        private const val IMAGE_WIDTH = IMAGE_HEIGHT * 10f / 16f

        private const val CAPE_U0 = 1f / 64f
        private const val CAPE_U1 = 11f / 64f
        private const val CAPE_V0 = 1f / 32f
        private const val CAPE_V1 = 17f / 32f
    }
}
