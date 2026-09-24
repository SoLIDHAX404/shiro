package org.solidhax.shiro.utils.ui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme

typealias NameTagSegments = List<Pair<String, CascadeGeometricColor>>

private const val NAMETAG_PADDING = 4f
private const val NAMETAG_GAP = 2f

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
 * The on-screen rectangle covering [box], or null when any of its corners is behind the camera.
 */
fun screenBounds(box: AABB): Bounds? {
    val corners = ArrayList<Vector2f>(8)
    for (x in doubleArrayOf(box.minX, box.maxX)) for (y in doubleArrayOf(box.minY, box.maxY)) for (z in doubleArrayOf(box.minZ, box.maxZ)) {
        corners += worldToScreen(Vec3(x, y, z)) ?: return null
    }
    return Bounds.of(corners)
}

/**
 * Where a name tag with [segments] goes when placed at [anchor] around [target].
 */
fun nameTagBounds(target: Bounds, anchor: BoxAnchor, segments: NameTagSegments): Bounds {
    val width = segments.fold(0f) { total, (text, _) -> total + textWidth(text) } + NAMETAG_PADDING * 2f
    return anchor.place(target.expand(NAMETAG_GAP), width, TEXT_SIZE + NAMETAG_PADDING)
}

/**
 * The anchor that puts a name tag with [segments] centered on ([centerX], [centerY]) around [target], as close as it can.
 */
fun nameTagAnchor(target: Bounds, segments: NameTagSegments, centerX: Float, centerY: Float): BoxAnchor {
    val size = nameTagBounds(target, BoxAnchor.ABOVE, segments)
    return BoxAnchor.fromCenter(target.expand(NAMETAG_GAP), size.width, size.height, centerX, centerY)
}

fun GuiGraphicsExtractor.nameTag(tag: Bounds, segments: NameTagSegments) {
    roundedRectangle(tag.left, tag.top, tag.width, tag.height, theme.card, Radius.MEDIUM)
    var textX = tag.left + NAMETAG_PADDING
    for ((text, color) in segments) {
        text(text, textX, tag.top + NAMETAG_PADDING / 2f, color)
        textX += textWidth(text)
    }
}

fun GuiGraphicsExtractor.nameTag(target: Bounds, anchor: BoxAnchor, segments: NameTagSegments) {
    if (segments.isNotEmpty()) nameTag(nameTagBounds(target, anchor, segments), segments)
}
