package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Category
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.lerpColor

class Sidebar {

    var selected: Category = Category.categories.values.first()
        private set

    var profileOpen = false
        private set

    private val profileBadge = ProfileBadge(ENTRY_WIDTH)
    private val indicatorY = Animation(SLIDE_DURATION)
    private val indicatorHeight = Animation(SLIDE_DURATION)
    private val hoverAnimations = HashMap<Category, Animation>()
    private var initialized = false

    private var x = 0f
    private var y = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, mouseX: Float, mouseY: Float) {
        this.x = x
        this.y = y

        graphics.rectangle(x + WIDTH, y, 1f, HEIGHT, theme.divider)

        updateIndicator()
        graphics.selectionHighlight(x + PADDING, y + indicatorY.value, ENTRY_WIDTH, indicatorHeight.value)

        Category.categories.values.forEachIndexed { index, category ->
            val entryX = x + PADDING
            val entryY = entryY(index)
            val isSelected = !profileOpen && category == selected
            val hovered = !isSelected && isAreaHovered(mouseX, mouseY, entryX, entryY, ENTRY_WIDTH, ENTRY_HEIGHT)
            val hover = hoverAnimation(category).animate(hovered)

            if (hover > 0f) {
                graphics.roundedRectangle(entryX, entryY, ENTRY_WIDTH, ENTRY_HEIGHT, lerpColor(0, theme.entryHovered, hover), ENTRY_CORNERS)
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
        profileBadge.draw(graphics, x + PADDING, badgeY, badgeHovered)
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

    private fun updateIndicator() {
        val targetY: Float
        val targetHeight: Float
        if (profileOpen) {
            targetY = badgeY - y
            targetHeight = ProfileBadge.HEIGHT
        } else {
            targetY = entryY(Category.categories.values.indexOf(selected)) - y
            targetHeight = ENTRY_HEIGHT
        }

        if (initialized) {
            indicatorY.animateTo(targetY)
            indicatorHeight.animateTo(targetHeight)
        } else {
            indicatorY.set(targetY)
            indicatorHeight.set(targetHeight)
            initialized = true
        }
    }

    private fun hoverAnimation(category: Category): Animation =
        hoverAnimations.getOrPut(category) { Animation(HOVER_DURATION) }

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

        private const val SLIDE_DURATION = 200L
        private const val HOVER_DURATION = 120L

        private val ENTRY_CORNERS = CascadeGeometricRadius(4f)

        private val font get() = CascadeFonts.sans

        fun GuiGraphicsExtractor.selectionHighlight(x: Float, y: Float, width: Float, height: Float) {
            roundedRectangle(x, y, width, height, theme.entrySelected, ENTRY_CORNERS)
            accentEdge(x, y, width, height, ENTRY_CORNERS, left = false)
        }
    }
}
