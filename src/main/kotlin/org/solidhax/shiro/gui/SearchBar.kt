package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.svgTexture

class SearchBar {

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float) {
        graphics.roundedRectangle(x, y, WIDTH, HEIGHT, theme.card, CORNERS)
        graphics.hollowRectangle(x, y, WIDTH, HEIGHT, 1f, theme.divider, CORNERS)

        graphics.image(
            svgTexture(ICON, ICON_SIZE.toInt()),
            x + ICON_INSET, y + (HEIGHT - ICON_SIZE) / 2f,
            ICON_SIZE, ICON_SIZE,
            color = theme.textMuted
        )

        font.extract(
            graphics, PLACEHOLDER,
            x + ICON_INSET + ICON_SIZE + TEXT_GAP,
            y + (HEIGHT - TEXT_SIZE) / 2f,
            theme.textMuted,
            shadow = false,
            size = TEXT_SIZE
        )
    }

    companion object {
        const val WIDTH = 140f
        const val HEIGHT = 18f

        private const val PLACEHOLDER = "Search..."
        private const val TEXT_SIZE = 8f
        private const val TEXT_GAP = 5f

        private const val ICON_INSET = 6f
        private const val ICON_SIZE = 9f

        private val ICON = Identifier.fromNamespaceAndPath("shiro", "search.svg")
        private val CORNERS = CascadeGeometricRadius(4f)

        private val font get() = CascadeFonts.sans
    }
}
