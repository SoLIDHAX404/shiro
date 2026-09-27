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
import kotlin.math.abs

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
    val desired = labels.map { (position, segments) -> labelBounds(target, position, segments) }
    val placed = arrayOfNulls<Bounds>(labels.size)

    for (side in COLUMN_SIDES) {
        val indices = labels.indices.filter { labels[it].first.side == side }
        stackColumn(indices.map { desired[it] }).forEachIndexed { index, bounds -> placed[indices[index]] = bounds }
    }

    for (index in labels.indices) {
        if (placed[index] != null) continue
        val side = labels[index].first.side
        var bounds = desired[index]
        var pushes = 0
        while (pushes++ < labels.size) {
            val blocker = placed.firstOrNull { it != null && it.overlaps(bounds) } ?: break
            bounds = if (side == LabelPosition.Side.TOP) bounds.offset(0f, blocker.top - LABEL_GAP - bounds.bottom)
            else bounds.offset(0f, blocker.bottom + LABEL_GAP - bounds.top)
        }
        placed[index] = bounds
    }
    return placed.requireNoNulls().toList()
}

private fun stackColumn(labels: List<Bounds>): List<Bounds> {
    val clusters = ArrayList<MutableList<Int>>()
    for (index in labels.indices.sortedBy { labels[it].centerY }) {
        clusters.add(mutableListOf(index))
        while (clusters.size > 1) {
            val last = clusters.last()
            val previous = clusters[clusters.size - 2]
            if (columnTop(labels, previous) + columnHeight(labels, previous) + LABEL_GAP <= columnTop(labels, last)) break
            previous.addAll(clusters.removeLast())
        }
    }

    val stacked = arrayOfNulls<Bounds>(labels.size)
    for (cluster in clusters) {
        var top = columnTop(labels, cluster)
        for (index in cluster) {
            stacked[index] = labels[index].offset(0f, top - labels[index].top)
            top += labels[index].height + LABEL_GAP
        }
    }
    return stacked.requireNoNulls().toList()
}

private fun columnHeight(labels: List<Bounds>, cluster: List<Int>): Float =
    cluster.sumOf { labels[it].height.toDouble() }.toFloat() + LABEL_GAP * (cluster.size - 1)

private fun columnTop(labels: List<Bounds>, cluster: List<Int>): Float =
    cluster.sumOf { labels[it].centerY.toDouble() }.toFloat() / cluster.size - columnHeight(labels, cluster) / 2f

private const val COLUMN_RANGE = 0.05f
private const val COLUMN_STEP = 0.001f

private val COLUMN_SIDES = listOf(LabelPosition.Side.CENTER, LabelPosition.Side.LEFT, LabelPosition.Side.RIGHT)

/**
 * The position around [target] whose label is centered closest to ([x], [y]), snapping to the middle of a side.
 */
fun nearestLabelPosition(target: Bounds, segments: LabelSegments, x: Float, y: Float, others: List<Pair<LabelPosition, Bounds>> = emptyList()): LabelPosition {
    val size = labelBounds(target, LabelPosition.TOP, segments)
    val position = LabelPosition.nearest(target.expand(LABEL_GAP), size.width, size.height, x, y, LABEL_SNAP)
    return if (position.side in COLUMN_SIDES) orderInColumn(position, y, others) else position
}

private fun orderInColumn(position: LabelPosition, y: Float, others: List<Pair<LabelPosition, Bounds>>): LabelPosition {
    val column = others
        .filter { (other, _) -> other.side == position.side && abs(other.along - position.along) <= COLUMN_RANGE }
        .sortedBy { (_, bounds) -> bounds.centerY }
    if (column.isEmpty()) return position

    val index = column.count { (_, bounds) -> bounds.centerY < y }
    val along = when (index) {
        0 -> column.first().first.along - COLUMN_STEP
        column.size -> column.last().first.along + COLUMN_STEP
        else -> (column[index - 1].first.along + column[index].first.along) / 2f
    }
    return LabelPosition(position.side, along.coerceIn(0f, 1f))
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
