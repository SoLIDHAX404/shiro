package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.icon
import org.solidhax.shiro.utils.ui.text

object SearchBar {

    const val WIDTH = 140f
    const val HEIGHT = 18f

    private const val PLACEHOLDER = "Search..."
    private const val INSET = 6f
    private const val ICON_SIZE = 9f
    private const val GAP = 5f

    private val ICON = Identifier.fromNamespaceAndPath("shiro", "search.svg")

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float) {
        graphics.roundedRectangle(x, y, WIDTH, HEIGHT, theme.card, Radius.LARGE)
        graphics.hollowRectangle(x, y, WIDTH, HEIGHT, 1f, theme.divider, Radius.LARGE)
        graphics.icon(ICON, x + INSET, y + (HEIGHT - ICON_SIZE) / 2f, ICON_SIZE, theme.textMuted)
        graphics.text(PLACEHOLDER, x + INSET + ICON_SIZE + GAP, y + (HEIGHT - TEXT_SIZE) / 2f, theme.textMuted)
    }
}
