package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.graphics.states.impl.image.data.CascadeImageFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.displayName
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.skinTexture

class ProfileBadge(private val width: Float) {

    private val hoverAnimation = Animation(HOVER_DURATION)

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, hovered: Boolean) {
        val hover = hoverAnimation.animate(hovered)
        if (hover > 0f) graphics.roundedRectangle(x, y, width, HEIGHT, lerpColor(0, theme.entryHovered, hover), CORNERS)

        val faceX = x + INSET
        val faceY = y + (HEIGHT - FACE_SIZE) / 2f
        drawFace(graphics, skinTexture(), faceX, faceY)

        val textX = faceX + FACE_SIZE + INSET
        font.extract(
            graphics, displayName(x + width - INSET - textX, TEXT_SIZE),
            textX,
            y + (HEIGHT - TEXT_SIZE) / 2f,
            theme.text,
            shadow = false,
            size = TEXT_SIZE
        )
    }

    private fun drawFace(graphics: GuiGraphicsExtractor, skin: Identifier, x: Float, y: Float) {
        graphics.image(skin, x, y, FACE_SIZE, FACE_SIZE, FACE_U0, FACE_V0, FACE_U1, FACE_V1, WHITE, FACE_CORNERS, CascadeImageFilter.NEAREST)
        graphics.image(skin, x, y, FACE_SIZE, FACE_SIZE, HAT_U0, FACE_V0, HAT_U1, FACE_V1, WHITE, FACE_CORNERS, CascadeImageFilter.NEAREST)
    }

    companion object {
        const val HEIGHT = 28f

        private const val INSET = 6f
        private const val FACE_SIZE = 16f
        private const val TEXT_SIZE = 8f

        private const val FACE_U0 = 8f / 64f
        private const val FACE_U1 = 16f / 64f
        private const val HAT_U0 = 40f / 64f
        private const val HAT_U1 = 48f / 64f
        private const val FACE_V0 = 8f / 64f
        private const val FACE_V1 = 16f / 64f

        private const val HOVER_DURATION = 120L

        private val CORNERS = CascadeGeometricRadius(4f)
        private val FACE_CORNERS = CascadeGeometricRadius(3f)
        private val WHITE = CascadeGeometricColor.WHITE

        private val font get() = CascadeFonts.sans
    }
}
