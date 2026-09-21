package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.blur.blur
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.extensions.shadow.dropShadow
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.gui.ClickGUI.theme

class Panel(private val title: String) {

    private val sidebar = Sidebar()
    private val titleWidth by lazy { font.width(title, TITLE_SIZE) }

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, mouseX: Float, mouseY: Float) {
        graphics.dropShadow(x, y, WIDTH, HEIGHT, CascadeGeometricOffset(0f, 2f), 10f, 0f, theme.shadow, ALL_CORNERS)
        graphics.blur(x, y, WIDTH, HEIGHT, theme.panelTint, ALL_CORNERS, 30f)

        drawHeader(graphics, x, y)
        sidebar.draw(graphics, x, y + HEADER_HEIGHT + 1f, mouseX, mouseY)

        graphics.hollowRectangle(x, y, WIDTH, HEIGHT, 1f, theme.border, ALL_CORNERS)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        return sidebar.mouseClicked(mouseX, mouseY, button)
    }

    private fun drawHeader(graphics: GuiGraphicsExtractor, x: Float, y: Float) {
        graphics.roundedRectangle(x, y, WIDTH, HEADER_HEIGHT, theme.headerTint, TOP_CORNERS)
        graphics.rectangle(x, y + HEADER_HEIGHT, WIDTH, 1f, theme.divider)

        font.extract(
            graphics, title,
            x + (WIDTH - titleWidth) / 2f,
            y + (HEADER_HEIGHT - TITLE_SIZE) / 2f,
            theme.text,
            shadow = false,
            size = TITLE_SIZE
        )
    }

    companion object {
        const val WIDTH = 465f
        const val HEADER_HEIGHT = 30f
        const val BODY_HEIGHT = 300f
        const val HEIGHT = HEADER_HEIGHT + BODY_HEIGHT

        private const val RADIUS = 4f
        private const val TITLE_SIZE = 10f

        private val ALL_CORNERS = CascadeGeometricRadius(RADIUS)
        private val TOP_CORNERS = CascadeGeometricRadius(RADIUS, RADIUS, 0f, 0f)

        private val font get() = CascadeFonts.sans
    }
}
