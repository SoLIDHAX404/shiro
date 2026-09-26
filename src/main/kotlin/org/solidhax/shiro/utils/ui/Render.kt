package org.solidhax.shiro.utils.ui

import foo.starred.cascade.graphics.extensions.blur.blur
import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.extensions.shadow.dropShadow
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.font.rendering.impl.FontRenderer
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.wrappers.svg.impl.CascadeSVG
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme

const val TEXT_SIZE = 8f

private const val ACCENT_WIDTH = 2f
private const val PANEL_BLUR_RADIUS = 30f
private val PANEL_SHADOW_OFFSET = CascadeGeometricOffset(0f, 2f)

val font: FontRenderer get() = CascadeFonts.sans

object Radius {
    val LARGE = CascadeGeometricRadius(4f)
    val MEDIUM = CascadeGeometricRadius(3f)
    val SMALL = CascadeGeometricRadius(2f)
    val TOP = CascadeGeometricRadius(4f, 4f, 0f, 0f)
    val BOTTOM = CascadeGeometricRadius(0f, 0f, 4f, 4f)
}

private val svgSources = HashMap<Identifier, String>()

fun GuiGraphicsExtractor.text(text: String, x: Float, y: Float, color: CascadeGeometricColor, size: Float = TEXT_SIZE) =
    font.extract(this, text, x, y, color, shadow = false, size = size)

fun GuiGraphicsExtractor.centeredText(text: String, centerX: Float, y: Float, color: CascadeGeometricColor, size: Float = TEXT_SIZE) =
    text(text, centerX - textWidth(text, size) / 2f, y, color, size)

fun textWidth(text: String, size: Float = TEXT_SIZE): Float = font.width(text, size)

fun GuiGraphicsExtractor.icon(id: Identifier, x: Float, y: Float, size: Float, color: CascadeGeometricColor, rotation: Float = 0f) {
    // rotate on top of the current pose; an explicit pose replaces it, which would drop outer transforms like the ClickGUI open/close scale
    val pose = if (rotation == 0f) null else Matrix3x2f(pose()).rotateAbout(rotation, x + size / 2f, y + size / 2f)
    image(svgTexture(id, size.toInt()), x, y, size, size, color = color, pose = pose)
}

fun GuiGraphicsExtractor.outlinedRectangle(x: Float, y: Float, width: Float, height: Float, fill: Int, outline: Int, radius: CascadeGeometricRadius) {
    roundedRectangle(x, y, width, height, fill, radius)
    hollowRectangle(x, y, width, height, 1f, outline, radius)
}

fun GuiGraphicsExtractor.accentEdge(x: Float, y: Float, width: Float, height: Float, radius: CascadeGeometricRadius, color: Int, left: Boolean) =
    scissor(if (left) x else x + width - ACCENT_WIDTH, y, ACCENT_WIDTH, height) {
        roundedRectangle(x, y, width, height, color, radius)
    }

inline fun GuiGraphicsExtractor.glassPanel(x: Float, y: Float, width: Float, height: Float, shadow: Boolean = true, content: () -> Unit = {}) {
    glassPanelBackground(x, y, width, height, shadow)
    content()
    hollowRectangle(x, y, width, height, 1f, theme.border, Radius.LARGE)
}

fun GuiGraphicsExtractor.glassPanelBackground(x: Float, y: Float, width: Float, height: Float, shadow: Boolean) {
    if (shadow) dropShadow(x, y, width, height, PANEL_SHADOW_OFFSET, 10f, 0f, theme.shadow, Radius.LARGE)
    blur(x, y, width, height, theme.panelTint, Radius.LARGE, PANEL_BLUR_RADIUS)
}

fun svgTexture(id: Identifier, size: Int): Identifier {
    val source = svgSources.getOrPut(id) {
        mc.resourceManager.getResourceOrThrow(id).openAsReader().use { it.readText() }.replace("currentColor", "#FFFFFF")
    }
    return CascadeSVG.load(source, id, size, size)
}

fun isAreaHovered(mouseX: Float, mouseY: Float, x: Float, y: Float, width: Float, height: Float): Boolean =
    mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
