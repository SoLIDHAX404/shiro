package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Category
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.accentEdge
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.animation.Animations
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.ui.text

class Sidebar {

    var selected: Category = categories.first()
        private set

    var profileOpen = false
        private set

    private val hoverAnimations = Animations<Any>()
    private val indicatorY = Animation(SLIDE_DURATION, PADDING)
    private val indicatorHeight = Animation(SLIDE_DURATION, ENTRY_HEIGHT)

    private var x = 0f
    private var y = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, mouseX: Float, mouseY: Float) {
        this.x = x + PADDING
        this.y = y

        graphics.rectangle(x + WIDTH, y, 1f, HEIGHT, theme.divider)

        val highlightY = y + indicatorY.animateTo(if (profileOpen) BADGE_OFFSET else entryOffset(categories.indexOf(selected)))
        val highlightHeight = indicatorHeight.animateTo(if (profileOpen) ProfileBadge.HEIGHT else ENTRY_HEIGHT)
        graphics.roundedRectangle(this.x, highlightY, ENTRY_WIDTH, highlightHeight, theme.entrySelected, Radius.LARGE)
        graphics.accentEdge(this.x, highlightY, ENTRY_WIDTH, highlightHeight, Radius.LARGE, theme.accent, left = false)

        categories.forEachIndexed { index, category ->
            val entryY = y + entryOffset(index)
            val active = !profileOpen && category == selected
            drawHover(graphics, category, entryY, ENTRY_HEIGHT, !active && isAreaHovered(mouseX, mouseY, this.x, entryY, ENTRY_WIDTH, ENTRY_HEIGHT))
            graphics.text(category.name, this.x + TEXT_INSET, entryY + (ENTRY_HEIGHT - TEXT_SIZE) / 2f, if (active) theme.text else theme.textMuted)
        }

        val badgeY = y + BADGE_OFFSET
        drawHover(graphics, ProfileBadge, badgeY, ProfileBadge.HEIGHT, !profileOpen && isAreaHovered(mouseX, mouseY, this.x, badgeY, ENTRY_WIDTH, ProfileBadge.HEIGHT))
        ProfileBadge.draw(graphics, this.x, badgeY, ENTRY_WIDTH)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false

        if (isAreaHovered(mouseX, mouseY, x, y + BADGE_OFFSET, ENTRY_WIDTH, ProfileBadge.HEIGHT)) {
            profileOpen = true
            return true
        }

        val index = categories.indices.firstOrNull { isAreaHovered(mouseX, mouseY, x, y + entryOffset(it), ENTRY_WIDTH, ENTRY_HEIGHT) } ?: return false
        selected = categories[index]
        profileOpen = false
        return true
    }

    private fun drawHover(graphics: GuiGraphicsExtractor, key: Any, entryY: Float, height: Float, hovered: Boolean) {
        val hover = hoverAnimations[key].animate(hovered)
        if (hover > 0f) graphics.roundedRectangle(x, entryY, ENTRY_WIDTH, height, lerpColor(0, theme.entryHovered, hover), Radius.LARGE)
    }

    private fun entryOffset(index: Int): Float = PADDING + index * (ENTRY_HEIGHT + SPACING)

    companion object {
        const val WIDTH = 110f
        const val HEIGHT = Panel.BODY_HEIGHT - 1f

        private const val PADDING = 6f
        private const val SPACING = 3f
        private const val ENTRY_WIDTH = WIDTH - PADDING * 2f
        private const val ENTRY_HEIGHT = 22f
        private const val TEXT_INSET = 8f
        private const val BADGE_OFFSET = HEIGHT - PADDING - ProfileBadge.HEIGHT
        private const val SLIDE_DURATION = 200L

        const val TEXT_X = PADDING + TEXT_INSET

        private val categories get() = Category.categories.values.toList()
    }
}
