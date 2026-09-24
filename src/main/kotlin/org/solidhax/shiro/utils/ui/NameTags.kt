package org.solidhax.shiro.utils.ui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme

private const val NAMETAG_PADDING = 4f

/**
 * Projects a world position onto the GUI-scaled screen, or null when it is behind the camera.
 */
fun worldToScreen(pos: Vec3): Vector2f? {
    val camera = mc.gameRenderer.mainCamera()
    val forward = camera.forwardVector()
    val relative = pos.subtract(camera.position())
    if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() <= 0.0) return null

    val ndc = mc.gameRenderer.projectPointToScreen(pos)
    return Vector2f(
        ((ndc.x + 1.0) / 2.0 * mc.window.guiScaledWidth).toFloat(),
        ((1.0 - ndc.y) / 2.0 * mc.window.guiScaledHeight).toFloat()
    )
}

/**
 * Draws a name tag card centered on [centerX] with its bottom edge at [bottomY], built from colored text segments.
 */
fun GuiGraphicsExtractor.nameTag(centerX: Float, bottomY: Float, segments: List<Pair<String, CascadeGeometricColor>>) {
    if (segments.isEmpty()) return
    val width = segments.fold(0f) { total, (text, _) -> total + textWidth(text) }
    val textY = bottomY - NAMETAG_PADDING / 2f - TEXT_SIZE
    var textX = centerX - width / 2f

    roundedRectangle(textX - NAMETAG_PADDING, textY - NAMETAG_PADDING / 2f, width + NAMETAG_PADDING * 2f, TEXT_SIZE + NAMETAG_PADDING, theme.card, Radius.MEDIUM)
    for ((text, color) in segments) {
        text(text, textX, textY, color)
        textX += textWidth(text)
    }
}
