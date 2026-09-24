package org.solidhax.shiro.gui.settings.impl

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.utils.ui.isAreaHovered

/**
 * A movable, scalable element drawn on the in-game HUD. Positions are in GUI-scaled coordinates.
 *
 * [render] draws the content at (0, 0) and returns its unscaled (width, height).
 * `example` is true inside the HUD editor, so elements can show placeholder data.
 */
class HudElement(
    x: Float,
    y: Float,
    scale: Float,
    var enabled: Boolean = true,
    val render: GuiGraphicsExtractor.(example: Boolean) -> Pair<Float, Float>,
) {
    private val defaultX = x
    private val defaultY = y
    private val defaultScale = scale

    var x: Float = x
    var y: Float = y

    var scale: Float = scale
        set(value) {
            field = value.coerceIn(MIN_SCALE, MAX_SCALE)
        }

    var width: Float = 0f
        private set
    var height: Float = 0f
        private set

    val scaledWidth: Float get() = width * scale
    val scaledHeight: Float get() = height * scale

    /**
     * Draws the element, shifted onto the screen if it would go off it (without changing the saved position).
     */
    fun draw(graphics: GuiGraphicsExtractor, example: Boolean) {
        val drawX = x.coerceIn(0f, (mc.window.guiScaledWidth - scaledWidth).coerceAtLeast(0f))
        val drawY = y.coerceIn(0f, (mc.window.guiScaledHeight - scaledHeight).coerceAtLeast(0f))

        graphics.pose().pushMatrix()
        graphics.pose().translate(drawX, drawY)
        graphics.pose().scale(scale, scale)
        val (width, height) = graphics.render(example)
        graphics.pose().popMatrix()

        this.width = width
        this.height = height
    }

    /**
     * Keeps the element fully on a screen of the given size.
     */
    fun clamp(screenWidth: Float, screenHeight: Float) {
        x = x.coerceIn(0f, (screenWidth - scaledWidth).coerceAtLeast(0f))
        y = y.coerceIn(0f, (screenHeight - scaledHeight).coerceAtLeast(0f))
    }

    fun isHovered(mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, x, y, scaledWidth, scaledHeight)

    fun reset() {
        x = defaultX
        y = defaultY
        scale = defaultScale
    }

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 5f
    }
}
