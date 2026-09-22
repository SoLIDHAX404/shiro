package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.graphics.states.impl.image.data.CascadeImageFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.cosmetics.Cape
import org.solidhax.shiro.cosmetics.CosmeticsManager
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.lerpColor

class CapeSelector {

    private val hoverAnimations = HashMap<Cape, Animation>()
    private val selectAnimations = HashMap<Cape, Animation>()

    private var x = 0f
    private var y = 0f
    private var width = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float) {
        this.x = x
        this.y = y
        this.width = width

        Cape.entries.forEachIndexed { index, cape ->
            val swatchX = swatchX(index)
            val selected = CosmeticsManager.cape == cape
            val hover = hoverAnimation(cape).animate(!selected && isAreaHovered(mouseX, mouseY, swatchX, y, swatchWidth, HEIGHT))
            val select = selectAnimation(cape).animate(selected)

            graphics.roundedRectangle(swatchX, y, swatchWidth, HEIGHT, lerpColor(theme.control, theme.controlHovered, hover), CORNERS)
            graphics.hollowRectangle(swatchX, y, swatchWidth, HEIGHT, 1f, lerpColor(theme.divider, theme.accent, select), CORNERS)

            val texture = cape.texture
            if (texture == null) {
                val labelWidth = font.width(cape.displayName, TEXT_SIZE)
                font.extract(
                    graphics, cape.displayName,
                    swatchX + (swatchWidth - labelWidth) / 2f,
                    y + (HEIGHT - TEXT_SIZE) / 2f,
                    if (selected) theme.text else theme.textMuted,
                    shadow = false,
                    size = TEXT_SIZE
                )
            } else {
                val imageHeight = HEIGHT - INSET * 2f
                val imageWidth = imageHeight * CAPE_ASPECT
                graphics.image(
                    texture,
                    swatchX + (swatchWidth - imageWidth) / 2f, y + INSET, imageWidth, imageHeight,
                    CAPE_U0, CAPE_V0, CAPE_U1, CAPE_V1,
                    radius = IMAGE_CORNERS,
                    filter = CascadeImageFilter.NEAREST
                )
            }
        }
    }

    fun mouseClicked(mouseX: Float, mouseY: Float): Boolean {
        Cape.entries.forEachIndexed { index, cape ->
            if (isAreaHovered(mouseX, mouseY, swatchX(index), y, swatchWidth, HEIGHT)) {
                CosmeticsManager.cape = cape
                return true
            }
        }
        return false
    }

    private fun swatchX(index: Int): Float = x + index * (swatchWidth + SPACING)

    private val swatchWidth get() = (width - SPACING * (Cape.entries.size - 1)) / Cape.entries.size

    private fun hoverAnimation(cape: Cape): Animation = hoverAnimations.getOrPut(cape) { Animation(HOVER_DURATION) }

    private fun selectAnimation(cape: Cape): Animation = selectAnimations.getOrPut(cape) { Animation(SELECT_DURATION) }

    companion object {
        const val HEIGHT = 32f

        private const val SPACING = 5f
        private const val INSET = 4f
        private const val TEXT_SIZE = 7f
        private const val HOVER_DURATION = 120L
        private const val SELECT_DURATION = 150L

        private const val CAPE_U0 = 1f / 64f
        private const val CAPE_U1 = 11f / 64f
        private const val CAPE_V0 = 1f / 32f
        private const val CAPE_V1 = 17f / 32f
        private const val CAPE_ASPECT = 10f / 16f

        private val CORNERS = CascadeGeometricRadius(4f)
        private val IMAGE_CORNERS = CascadeGeometricRadius(2f)

        private val font get() = CascadeFonts.sans
    }
}
