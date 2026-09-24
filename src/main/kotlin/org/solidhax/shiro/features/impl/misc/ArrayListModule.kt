package org.solidhax.shiro.features.impl.misc

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.HudElement
import org.solidhax.shiro.gui.settings.impl.SelectorSetting
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.animation.Animations
import org.solidhax.shiro.utils.ui.fade
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth

object ArrayListModule : Module(
    name = "Array List",
    description = "Shows all enabled modules on screen."
) {
    private val sort by SelectorSetting("Sort", "Length", listOf("Length", "Alphabetical"), desc = "How the modules are ordered.")
    private val align by SelectorSetting("Align", "Auto", listOf("Auto", "Left", "Right"), desc = "Which side the text lines up on. Auto follows the side of the screen the list is on.")
    private val textColor by ColorSetting("Text Color", 0xFF9A86F2.toInt(), desc = "Color of the module names.")
    private val background by BooleanSetting("Background", true, desc = "Draws a dark background behind each module.")
    private val showSelf by BooleanSetting("Show Self", false, desc = "Lists the Array List module itself.")

    private val slideAnimations = Animations<Module>(SLIDE_DURATION)

    private val hud: HudElement by HUD("Array List", "Shows all enabled modules on screen.", toggleable = false) { example ->
        val lines = visibleLines().ifEmpty { if (example) EXAMPLE_LINES else emptyList() }
        if (lines.isEmpty()) return@HUD 0f to 0f

        val rightAligned = when (align) {
            ALIGN_LEFT -> false
            ALIGN_RIGHT -> true
            else -> hud.x + hud.scaledWidth / 2f > mc.window.guiScaledWidth / 2f
        }
        val width = lines.maxOf { textWidth(it.name) } + PADDING * 2f

        var y = 0f
        for ((name, progress) in lines) {
            val lineWidth = textWidth(name) + PADDING * 2f
            val offset = (1f - progress) * lineWidth
            val lineX = if (rightAligned) width - lineWidth + offset else -offset
            line(name, lineX, y, lineWidth, progress)
            y += (LINE_HEIGHT + LINE_GAP) * progress
        }
        width to (y - LINE_GAP).coerceAtLeast(0f)
    }

    private fun GuiGraphicsExtractor.line(name: String, x: Float, y: Float, width: Float, progress: Float) {
        if (background) roundedRectangle(x, y, width, LINE_HEIGHT, fade(BACKGROUND, progress), Radius.MEDIUM)
        text(name, x + PADDING, y + (LINE_HEIGHT - TEXT_SIZE) / 2f, CascadeGeometricColor(fade(textColor, progress)))
    }

    private fun visibleLines(): List<Line> {
        val lines = ModuleManager.modules.values
            .filter { showSelf || it != this }
            .map { Line(it.name, slideAnimations[it].animate(it.enabled)) }
            .filter { it.progress > 0f }
        return if (sort == SORT_LENGTH) lines.sortedByDescending { textWidth(it.name) } else lines.sortedBy { it.name.lowercase() }
    }

    private data class Line(val name: String, val progress: Float)

    private const val SORT_LENGTH = 0
    private const val ALIGN_LEFT = 1
    private const val ALIGN_RIGHT = 2

    private const val PADDING = 3f
    private const val LINE_HEIGHT = TEXT_SIZE + PADDING * 2f
    private const val LINE_GAP = 2f
    private const val BACKGROUND = 0x80000000.toInt()
    private const val SLIDE_DURATION = 250L

    private val EXAMPLE_LINES = listOf(Line("Corpse ESP", 1f), Line("Test Module", 1f))
}
