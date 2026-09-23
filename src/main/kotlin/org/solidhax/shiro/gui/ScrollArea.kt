package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.Page.Companion.PADDING
import org.solidhax.shiro.utils.ui.isAreaHovered

class ScrollArea(private val topPadding: Float = PADDING) {

    private var offset = 0f
    private var x = 0f
    private var y = 0f
    private var width = 0f
    private var height = 0f
    private var contentHeight = 0f
    private var dragging = false

    val contentX get() = x + PADDING
    val contentY get() = y + topPadding - offset
    val contentWidth get() = width - PADDING * 2f - SCROLLBAR_SPACE

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, contentHeight: Float, mouseX: Float, mouseY: Float, content: () -> Unit) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        this.contentHeight = contentHeight
        offset = offset.coerceIn(0f, maxOffset)

        graphics.scissor(x, y, width, height, content)
        if (maxOffset <= 0f) return

        val color = if (dragging || isTrackHovered(mouseX, mouseY)) theme.scrollbarHovered else theme.scrollbar
        graphics.roundedRectangle(barX, thumbY, WIDTH, thumbHeight, color, CORNERS)
    }

    fun isHovered(mouseX: Float, mouseY: Float): Boolean = isAreaHovered(mouseX, mouseY, x, y, width, height)

    fun mouseClicked(mouseX: Float, mouseY: Float): Boolean {
        if (maxOffset <= 0f || !isTrackHovered(mouseX, mouseY)) return false
        dragging = true
        if (mouseY < thumbY || mouseY > thumbY + thumbHeight) scrollTo((mouseY - trackY - thumbHeight / 2f) / travel * maxOffset)
        return true
    }

    fun mouseDragged(deltaY: Float): Boolean {
        if (!dragging) return false
        scrollTo(offset + deltaY * maxOffset / travel)
        return true
    }

    fun mouseReleased() {
        dragging = false
    }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (maxOffset <= 0f || !isHovered(mouseX, mouseY)) return false
        scrollTo(offset - amount * STEP)
        return true
    }

    fun reset() {
        offset = 0f
        dragging = false
    }

    private fun scrollTo(target: Float) {
        if (target.isFinite()) offset = target.coerceIn(0f, maxOffset)
    }

    private fun isTrackHovered(mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, barX - GRAB, trackY, WIDTH + GRAB * 2f, trackHeight)

    private val barX get() = x + width - INSET - WIDTH
    private val trackY get() = y + topPadding
    private val trackHeight get() = height - topPadding - PADDING
    private val maxOffset get() = (contentHeight - trackHeight).coerceAtLeast(0f)
    private val thumbHeight get() = (trackHeight * trackHeight / contentHeight).coerceIn(MIN_THUMB, trackHeight)
    private val thumbY get() = trackY + travel * (offset / maxOffset)
    private val travel get() = trackHeight - thumbHeight

    companion object {
        private const val WIDTH = 3f
        private const val INSET = 4f
        private const val GRAB = 3f
        private const val STEP = 16f
        private const val MIN_THUMB = 20f
        private const val SCROLLBAR_SPACE = 6f

        private val CORNERS = CascadeGeometricRadius(WIDTH / 2f)
    }
}
