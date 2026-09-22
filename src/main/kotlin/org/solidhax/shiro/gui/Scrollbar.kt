package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.isAreaHovered

class Scrollbar {

    var offset = 0f
        private set

    private var x = 0f
    private var y = 0f
    private var viewHeight = 0f
    private var contentHeight = 0f
    private var dragging = false

    fun draw(graphics: GuiGraphicsExtractor, right: Float, y: Float, viewHeight: Float, contentHeight: Float, mouseX: Float, mouseY: Float) {
        this.x = right - WIDTH
        this.y = y
        this.viewHeight = viewHeight
        this.contentHeight = contentHeight
        offset = offset.coerceIn(0f, maxOffset)

        if (maxOffset <= 0f) return

        val active = dragging || isAreaHovered(mouseX, mouseY, x - GRAB, y, WIDTH + GRAB * 2f, viewHeight)
        graphics.roundedRectangle(x, thumbY, WIDTH, thumbHeight, if (active) theme.scrollbarHovered else theme.scrollbar, CORNERS)
    }

    fun scroll(amount: Float): Boolean {
        if (maxOffset <= 0f) return false
        offset = (offset - amount * STEP).coerceIn(0f, maxOffset)
        return true
    }

    fun mouseClicked(mouseX: Float, mouseY: Float): Boolean {
        if (maxOffset <= 0f || !isAreaHovered(mouseX, mouseY, x - GRAB, y, WIDTH + GRAB * 2f, viewHeight)) return false
        dragging = true
        if (mouseY < thumbY || mouseY > thumbY + thumbHeight) centerThumbOn(mouseY)
        return true
    }

    fun mouseDragged(deltaY: Float): Boolean {
        if (!dragging) return false
        val travel = viewHeight - thumbHeight
        if (travel > 0f) offset = (offset + deltaY * maxOffset / travel).coerceIn(0f, maxOffset)
        return true
    }

    fun mouseReleased() {
        dragging = false
    }

    fun reset() {
        offset = 0f
        dragging = false
    }

    private fun centerThumbOn(mouseY: Float) {
        val travel = viewHeight - thumbHeight
        if (travel <= 0f) return
        offset = ((mouseY - y - thumbHeight / 2f) / travel * maxOffset).coerceIn(0f, maxOffset)
    }

    private val maxOffset get() = (contentHeight - viewHeight).coerceAtLeast(0f)

    private val thumbHeight get() = (viewHeight * viewHeight / contentHeight).coerceIn(MIN_THUMB, viewHeight)

    private val thumbY get() = y + (viewHeight - thumbHeight) * (offset / maxOffset)

    companion object {
        const val WIDTH = 3f

        private const val GRAB = 3f
        private const val STEP = 16f
        private const val MIN_THUMB = 20f

        private val CORNERS = CascadeGeometricRadius(WIDTH / 2f)
    }
}
