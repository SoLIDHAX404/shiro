package org.solidhax.shiro.features.impl.misc

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.HudElement
import org.solidhax.shiro.gui.settings.impl.SelectorSetting
import org.solidhax.shiro.utils.ui.ACCENT_WIDTH
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth

// not named ArrayList so it doesn't shadow kotlin.collections.ArrayList wherever it's imported
object ArrayListModule : Module(
    name = "Array List",
    description = "Shows all enabled modules on screen."
) {
    private val sort by SelectorSetting("Sort", "Length", listOf("Length", "Alphabetical"), desc = "How the modules are ordered.")
    private val align by SelectorSetting("Align", "Auto", listOf("Auto", "Left", "Right"), desc = "Which side the text lines up on. Auto follows the side of the screen the list is on.")
    private val textColor by ColorSetting("Text Color", 0xFF9A86F2.toInt(), desc = "Color of the module names.")
    private val background by BooleanSetting("Background", true, desc = "Draws a dark background behind each module.")
    private val accentBar by BooleanSetting("Accent Bar", true, desc = "Draws an accent edge in the text color on the aligned side.")
    private val showSelf by BooleanSetting("Show Self", false, desc = "Lists the Array List module itself.")

    private val hud: HudElement by HUD("Array List", "Shows all enabled modules on screen.", toggleable = false) { example ->
        val names = enabledModules().ifEmpty { if (example) EXAMPLE_NAMES else emptyList() }
        if (names.isEmpty()) return@HUD 0f to 0f

        val rightAligned = when (align) {
            ALIGN_LEFT -> false
            ALIGN_RIGHT -> true
            else -> hud.x + hud.scaledWidth / 2f > mc.window.guiScaledWidth / 2f
        }
        val bar = if (accentBar) ACCENT_WIDTH else 0f
        val width = names.maxOf { textWidth(it) } + PADDING * 2f + bar

        names.forEachIndexed { index, name ->
            val y = index * (LINE_HEIGHT + LINE_GAP)
            val lineWidth = textWidth(name) + PADDING * 2f + bar
            line(name, if (rightAligned) width - lineWidth else 0f, y, lineWidth, bar, rightAligned)
        }
        width to names.size * LINE_HEIGHT + (names.size - 1) * LINE_GAP
    }

    // the accent is its own shape instead of a scissored slice of the background: scissor snaps to whole
    // GUI pixels, which leaves a gap at the edge once the text widths or the HUD scale are fractional
    private fun GuiGraphicsExtractor.line(name: String, x: Float, y: Float, width: Float, bar: Float, rightAligned: Boolean) {
        val bodyX = if (rightAligned) x else x + bar
        if (background) roundedRectangle(bodyX, y, width - bar, LINE_HEIGHT, BACKGROUND, radius(bar == 0f || rightAligned, bar == 0f || !rightAligned))
        if (bar > 0f) roundedRectangle(if (rightAligned) x + width - bar else x, y, bar, LINE_HEIGHT, textColor, radius(!rightAligned, rightAligned))
        text(name, bodyX + PADDING, y + (LINE_HEIGHT - TEXT_SIZE) / 2f, CascadeGeometricColor(textColor))
    }

    private fun radius(left: Boolean, right: Boolean): CascadeGeometricRadius {
        val l = if (left) RADIUS else 0f
        val r = if (right) RADIUS else 0f
        return CascadeGeometricRadius(l, r, l, r)
    }

    private fun enabledModules(): List<String> {
        val names = ModuleManager.modules.values.filter { it.enabled && (showSelf || it != this) }.map(Module::name)
        return if (sort == SORT_LENGTH) names.sortedByDescending { textWidth(it) } else names.sortedBy(String::lowercase)
    }

    private const val SORT_LENGTH = 0
    private const val ALIGN_LEFT = 1
    private const val ALIGN_RIGHT = 2

    private const val PADDING = 3f
    private const val LINE_HEIGHT = TEXT_SIZE + PADDING * 2f
    private const val LINE_GAP = 2f
    private const val RADIUS = 3f
    private const val BACKGROUND = 0x80000000.toInt()

    private val EXAMPLE_NAMES = listOf("Corpse ESP", "Test Module")
}
