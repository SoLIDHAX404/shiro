package org.solidhax.shiro.gui.settings.impl

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth

/**
 * A movable, scalable element drawn on the in-game HUD. Positions are in GUI-scaled coordinates.
 *
 * [render] draws the content at (0, 0) and returns its unscaled (width, height).
 * `example` is true inside the HUD editor, so elements can show placeholder data.
 */
class HudElement(
    val title: String,
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

    private val titleDropdown = DropdownSetting("Title")
    val drawTitle = BooleanSetting("Draw Title", false, desc = "Draws the element's name above it.").withDependency(titleDropdown)
    val titlePadding = NumberSetting("Padding", 3f, 0, 12, 1, desc = "Space between the title and the content below it.", unit = "px").withDependency(titleDropdown)

    private val backgroundDropdown = DropdownSetting("Background")
    val backgroundType = SelectorSetting("Type", BACKGROUND_NONE, listOf(BACKGROUND_NONE, BACKGROUND_ROUNDED, BACKGROUND_SQUARE), desc = "Shape drawn behind the element.").withDependency(backgroundDropdown)
    val backgroundColor = ColorSetting("Color", 0xFF000000.toInt(), desc = "Color of the background.").withDependency(backgroundDropdown)
    val backgroundOpacity = NumberSetting("Opacity", 50, 0, 100, 5, desc = "How see-through the background is.", unit = "%").withDependency(backgroundDropdown)
    val backgroundPadding = NumberSetting("Padding", 4f, 0, 12, 1, desc = "Space between the edge of the background and the content.", unit = "px").withDependency(backgroundDropdown)

    val settings: List<Setting<*>> = listOf(titleDropdown, drawTitle, titlePadding, backgroundDropdown, backgroundType, backgroundColor, backgroundOpacity, backgroundPadding)

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

        val showTitle = drawTitle.enabled
        val padding = if (backgroundType.selected != BACKGROUND_NONE) backgroundPadding.value else 0f
        val titleHeight = if (showTitle) TITLE_SIZE + titlePadding.value else 0f
        val visible = width > 0f && height > 0f

        graphics.pose().pushMatrix()
        graphics.pose().translate(drawX, drawY)
        graphics.pose().scale(scale, scale)
        if (visible) drawFrame(graphics, padding, showTitle)

        graphics.pose().pushMatrix()
        graphics.pose().translate(padding, padding + titleHeight)
        val (contentWidth, contentHeight) = graphics.render(example)
        graphics.pose().popMatrix()
        graphics.pose().popMatrix()

        if (contentWidth <= 0f || contentHeight <= 0f) {
            this.width = 0f
            this.height = 0f
            return
        }
        val titleWidth = if (showTitle) textWidth(title, TITLE_SIZE) else 0f
        this.width = maxOf(contentWidth, titleWidth) + padding * 2f
        this.height = contentHeight + titleHeight + padding * 2f
    }

    private fun drawFrame(graphics: GuiGraphicsExtractor, padding: Float, showTitle: Boolean) {
        val color = (backgroundOpacity.value * 255 / 100) shl 24 or (backgroundColor.value and 0xFFFFFF)
        when (backgroundType.selected) {
            BACKGROUND_ROUNDED -> graphics.roundedRectangle(0f, 0f, width, height, color, Radius.MEDIUM)
            BACKGROUND_SQUARE -> graphics.rectangle(0f, 0f, width, height, color)
        }
        if (showTitle) graphics.text(title, padding, padding, theme.text, TITLE_SIZE)
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
        settings.forEach(Setting<*>::reset)
    }

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 5f

        private const val TITLE_SIZE = 8f
        private const val BACKGROUND_NONE = "None"
        private const val BACKGROUND_ROUNDED = "Rounded"
        private const val BACKGROUND_SQUARE = "Square"
    }
}
