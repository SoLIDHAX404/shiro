package org.solidhax.shiro.utils.ui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.render.ModelBounds
import org.solidhax.shiro.utils.render.corners

typealias LabelSegments = List<Pair<String, CascadeGeometricColor>>

private const val LABEL_PADDING = 4f
private const val LABEL_GAP = 2f
private const val LABEL_SNAP = 4f

const val PREVIEW_DISTANCE = 12

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
fun screenBounds(box: AABB): Bounds? =
    Bounds.of(box.corners.map { worldToScreen(it) ?: return null })

fun labelSegments(title: String?, titleColor: Int, distance: Int?): LabelSegments = buildList {
    if (title != null) add(title to CascadeGeometricColor(titleColor))
    if (distance != null) add((if (isEmpty()) "" else " ") + "${distance}m" to theme.textMuted)
}

/**
 * Where a label with [segments] goes when placed at [position] around [target].
 */
fun labelBounds(target: Bounds, position: LabelPosition, segments: LabelSegments): Bounds {
    val width = segments.fold(0f) { total, (text, _) -> total + textWidth(text) } + LABEL_PADDING * 2f
    return position.place(target.expand(LABEL_GAP), width, TEXT_SIZE + LABEL_PADDING)
}

fun placeLabels(target: Bounds, labels: List<Pair<LabelPosition, LabelSegments>>): List<Bounds> {
    val placed = ArrayList<Bounds>(labels.size)
    for ((position, segments) in labels) {
        var bounds = labelBounds(target, position, segments)
        repeat(placed.size) {
            val blocker = placed.firstOrNull { it.overlaps(bounds) } ?: return@repeat
            bounds = pushOut(bounds, blocker, position.side)
        }
        placed.add(bounds)
    }
    return placed
}

private fun pushOut(bounds: Bounds, blocker: Bounds, side: LabelPosition.Side): Bounds = when (side) {
    LabelPosition.Side.TOP -> bounds.offset(0f, blocker.top - LABEL_GAP - bounds.bottom)
    LabelPosition.Side.BOTTOM, LabelPosition.Side.CENTER -> bounds.offset(0f, blocker.bottom + LABEL_GAP - bounds.top)
    LabelPosition.Side.LEFT -> bounds.offset(blocker.left - LABEL_GAP - bounds.right, 0f)
    LabelPosition.Side.RIGHT -> bounds.offset(blocker.right + LABEL_GAP - bounds.left, 0f)
}

/**
 * The position around [target] whose label is centered closest to ([x], [y]), snapping to the middle of a side.
 */
fun nearestLabelPosition(target: Bounds, segments: LabelSegments, x: Float, y: Float): LabelPosition {
    val size = labelBounds(target, LabelPosition.TOP, segments)
    return LabelPosition.nearest(target.expand(LABEL_GAP), size.width, size.height, x, y, LABEL_SNAP)
}

fun GuiGraphicsExtractor.label(bounds: Bounds, segments: LabelSegments) {
    roundedRectangle(bounds.left, bounds.top, bounds.width, bounds.height, theme.card, Radius.MEDIUM)
    var textX = bounds.left + LABEL_PADDING
    for ((text, color) in segments) {
        text(text, textX, bounds.top + LABEL_PADDING / 2f, color)
        textX += textWidth(text)
    }
}

fun GuiGraphicsExtractor.labels(target: Bounds, labels: List<Pair<LabelPosition, LabelSegments>>) {
    val shown = labels.filter { it.second.isNotEmpty() }
    placeLabels(target, shown).forEachIndexed { index, bounds -> label(bounds, shown[index].second) }
}

/**
 * Draws a label centered on the screen position of [pos].
 */
fun GuiGraphicsExtractor.worldLabel(pos: Vec3, segments: LabelSegments) {
    if (segments.isEmpty()) return
    val point = worldToScreen(pos) ?: return
    labels(Bounds(point.x, point.y, point.x, point.y), listOf(LabelPosition.CENTER to segments))
}

fun GuiGraphicsExtractor.entityLabels(entity: Entity, partialTick: Float, labels: List<Pair<LabelPosition, LabelSegments>>) {
    if (labels.all { it.second.isEmpty() }) return
    labels(screenBounds(ModelBounds.of(entity, partialTick)) ?: return, labels)
}

fun GuiGraphicsExtractor.entityLabel(entity: Entity, partialTick: Float, position: LabelPosition, segments: LabelSegments) =
    entityLabels(entity, partialTick, listOf(position to segments))
