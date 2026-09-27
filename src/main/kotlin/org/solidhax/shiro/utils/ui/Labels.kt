package org.solidhax.shiro.utils.ui

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
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

data class LabelStyle(
    val textSize: Float = TEXT_SIZE,
    val shadow: Boolean = false,
    val background: Background = Background.ROUNDED,
    val backgroundColor: Int = DEFAULT_BACKGROUND,
    val padding: Float = DEFAULT_PADDING,
) {
    enum class Background { NONE, ROUNDED, SQUARE }

    companion object {
        const val DEFAULT_BACKGROUND = 0x33000000
        const val DEFAULT_PADDING = 4f
    }
}

class Label(val position: LabelPosition, val segments: LabelSegments, val style: LabelStyle = LabelStyle())

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
 * Where [label] goes around [target].
 */
fun labelBounds(target: Bounds, label: Label): Bounds {
    val style = label.style
    val width = label.segments.fold(0f) { total, (text, _) -> total + textWidth(text, style.textSize) } + style.padding * 2f
    return label.position.place(target.expand(LABEL_GAP), width, style.textSize + style.padding)
}

fun placeLabels(target: Bounds, labels: List<Label>): List<Bounds> {
    val desired = labels.map { labelBounds(target, it) }
    val placed = arrayOfNulls<Bounds>(labels.size)

    for (side in COLUMN_SIDES) {
        val indices = labels.indices.filter { labels[it].position.side == side }
        stackColumn(indices.map { desired[it] }).forEachIndexed { index, bounds -> placed[indices[index]] = bounds }
    }

    for (index in labels.indices) {
        if (placed[index] != null) continue
        val side = labels[index].position.side
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
fun nearestLabelPosition(target: Bounds, label: Label, x: Float, y: Float, others: List<Pair<LabelPosition, Bounds>> = emptyList()): LabelPosition {
    val size = labelBounds(target, Label(LabelPosition.TOP, label.segments, label.style))
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

fun GuiGraphicsExtractor.label(bounds: Bounds, label: Label) {
    val style = label.style
    when (style.background) {
        LabelStyle.Background.ROUNDED -> roundedRectangle(bounds.left, bounds.top, bounds.width, bounds.height, style.backgroundColor, Radius.MEDIUM)
        LabelStyle.Background.SQUARE -> rectangle(bounds.left, bounds.top, bounds.width, bounds.height, style.backgroundColor)
        LabelStyle.Background.NONE -> {}
    }
    withTextStyle(TextStyle(style.textSize, style.shadow)) {
        var textX = bounds.left + style.padding
        for ((text, color) in label.segments) {
            text(text, textX, bounds.top + style.padding / 2f, color)
            textX += textWidth(text)
        }
    }
}

fun GuiGraphicsExtractor.labels(target: Bounds, labels: List<Label>) {
    val shown = labels.filter { it.segments.isNotEmpty() }
    placeLabels(target, shown).forEachIndexed { index, bounds -> label(bounds, shown[index]) }
}

/**
 * Draws a label centered on the screen position of [pos].
 */
fun GuiGraphicsExtractor.worldLabel(pos: Vec3, segments: LabelSegments) {
    if (segments.isEmpty()) return
    val point = worldToScreen(pos) ?: return
    labels(Bounds(point.x, point.y, point.x, point.y), listOf(Label(LabelPosition.CENTER, segments)))
}

fun GuiGraphicsExtractor.entityLabels(entity: Entity, partialTick: Float, labels: List<Label>) {
    if (labels.all { it.segments.isEmpty() }) return
    labels(screenBounds(ModelBounds.of(entity, partialTick)) ?: return, labels)
}

fun GuiGraphicsExtractor.entityLabel(entity: Entity, partialTick: Float, label: Label) =
    entityLabels(entity, partialTick, listOf(label))
