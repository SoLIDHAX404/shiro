package org.solidhax.shiro.utils.ui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.font.rendering.impl.FontRenderer
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import foo.starred.cascade.wrappers.svg.impl.CascadeSVG
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import org.solidhax.shiro.Shiro.mc

const val TEXT_SIZE = 8f

private const val ACCENT_WIDTH = 2f

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

fun textWidth(text: String, size: Float = TEXT_SIZE): Float = font.width(text, size)

fun GuiGraphicsExtractor.icon(id: Identifier, x: Float, y: Float, size: Float, color: CascadeGeometricColor, rotation: Float = 0f) {
    val pose = if (rotation == 0f) null else Matrix3x2f().rotateAbout(rotation, x + size / 2f, y + size / 2f)
    image(svgTexture(id, size.toInt()), x, y, size, size, color = color, pose = pose)
}

fun GuiGraphicsExtractor.accentEdge(x: Float, y: Float, width: Float, height: Float, radius: CascadeGeometricRadius, color: Int, left: Boolean) =
    scissor(if (left) x else x + width - ACCENT_WIDTH, y, ACCENT_WIDTH, height) {
        roundedRectangle(x, y, width, height, color, radius)
    }

fun svgTexture(id: Identifier, size: Int): Identifier {
    val source = svgSources.getOrPut(id) {
        mc.resourceManager.getResourceOrThrow(id).openAsReader().use { it.readText() }.replace("currentColor", "#FFFFFF")
    }
    return CascadeSVG.load(source, id, size, size)
}

fun isAreaHovered(mouseX: Float, mouseY: Float, x: Float, y: Float, width: Float, height: Float): Boolean =
    mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height

fun lerpColor(from: Int, to: Int, progress: Float): Int {
    val amount = progress.coerceIn(0f, 1f)
    var result = 0
    for (shift in 0..24 step 8) {
        val start = (from shr shift) and 0xFF
        val end = (to shr shift) and 0xFF
        result = result or (((start + (end - start) * amount).toInt() and 0xFF) shl shift)
    }
    return result
}

fun lerpColor(from: CascadeGeometricColor, to: CascadeGeometricColor, progress: Float): CascadeGeometricColor =
    CascadeGeometricColor(lerpColor(from.tl, to.tl, progress))
