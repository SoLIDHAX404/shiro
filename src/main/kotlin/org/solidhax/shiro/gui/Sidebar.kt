package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Category
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.isAreaHovered

class Sidebar {

    var selected: Category = Category.categories.values.first()
        private set

    var profileOpen = false
        private set

    private val profileBadge = ProfileBadge(ENTRY_WIDTH)

    private var x = 0f
    private var y = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, mouseX: Float, mouseY: Float) {
        this.x = x
        this.y = y

        graphics.rectangle(x + WIDTH, y, 1f, HEIGHT, theme.divider)

        Category.categories.values.forEachIndexed { index, category ->
            val entryX = x + PADDING
            val entryY = entryY(index)
            val isSelected = !profileOpen && category == selected

            if (isSelected) {
                graphics.selectionHighlight(entryX, entryY, ENTRY_WIDTH, ENTRY_HEIGHT)
            } else if (isAreaHovered(mouseX, mouseY, entryX, entryY, ENTRY_WIDTH, ENTRY_HEIGHT)) {
                graphics.roundedRectangle(entryX, entryY, ENTRY_WIDTH, ENTRY_HEIGHT, theme.entryHovered, ENTRY_CORNERS)
            }

            font.extract(
                graphics, category.name,
                entryX + TEXT_INSET,
                entryY + (ENTRY_HEIGHT - TEXT_SIZE) / 2f,
                if (isSelected) theme.text else theme.textMuted,
                shadow = false,
                size = TEXT_SIZE
            )
        }

        val badgeHovered = isAreaHovered(mouseX, mouseY, x + PADDING, badgeY, ENTRY_WIDTH, ProfileBadge.HEIGHT)
        profileBadge.draw(graphics, x + PADDING, badgeY, profileOpen, badgeHovered)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false

        if (isAreaHovered(mouseX, mouseY, x + PADDING, badgeY, ENTRY_WIDTH, ProfileBadge.HEIGHT)) {
            profileOpen = true
            return true
        }

        Category.categories.values.forEachIndexed { index, category ->
            if (isAreaHovered(mouseX, mouseY, x + PADDING, entryY(index), ENTRY_WIDTH, ENTRY_HEIGHT)) {
                selected = category
                profileOpen = false
                return true
            }
        }
        return false
    }

    private fun entryY(index: Int): Float = y + PADDING + index * (ENTRY_HEIGHT + SPACING)

    private val badgeY get() = y + HEIGHT - PADDING - ProfileBadge.HEIGHT

    companion object {
        const val WIDTH = 110f
        const val HEIGHT = Panel.BODY_HEIGHT - 1f

        private const val PADDING = 6f
        private const val SPACING = 3f
        private const val ENTRY_WIDTH = WIDTH - PADDING * 2f
        private const val ENTRY_HEIGHT = 22f
        private const val TEXT_INSET = 8f
        private const val TEXT_SIZE = 8f

        const val TEXT_X = PADDING + TEXT_INSET

        private val ENTRY_CORNERS = CascadeGeometricRadius(4f)

        private val font get() = CascadeFonts.sans

        fun GuiGraphicsExtractor.selectionHighlight(x: Float, y: Float, width: Float, height: Float) {
            roundedRectangle(x, y, width, height, theme.entrySelected, ENTRY_CORNERS)
            accentEdge(x, y, width, height, ENTRY_CORNERS, left = false)
        }
    }
}
