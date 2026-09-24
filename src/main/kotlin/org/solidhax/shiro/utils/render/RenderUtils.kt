// Adapted from Odin (https://github.com/odtheking/Odin), Copyright (c) 2025, odtheking, BSD 3-Clause License.
package org.solidhax.shiro.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.StringDecomposer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.events.RenderEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.utils.ui.fade
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

internal data class LineData(val from: Vec3, val to: Vec3, val color1: Int, val color2: Int, val thickness: Float, val depth: Boolean)
internal data class BoxData(val aabb: AABB, val color: Int, val thickness: Float, val depth: Boolean)
internal data class BeaconData(val pos: BlockPos, val color: Int, val isScoping: Boolean, val gameTime: Long)
internal data class TextData(val text: String, val pos: Vec3, val scale: Float, val depth: Boolean, val cameraRotation: Quaternionf, val textWidth: Float)
internal data class TexturedQuadData(val texture: Identifier, val bl: Vec3, val tl: Vec3, val tr: Vec3, val br: Vec3, val nx: Float, val ny: Float, val nz: Float, val color: Int, val depth: Boolean)

class RenderConsumer {
    internal val lines = ObjectArrayList<LineData>()
    internal val filledBoxes = ObjectArrayList<BoxData>()
    internal val wireBoxes = ObjectArrayList<BoxData>()

    internal val beaconBeams = ObjectArrayList<BeaconData>()
    internal val texts = ObjectArrayList<TextData>()
    internal val texturedQuads = ObjectArrayList<TexturedQuadData>()

    fun clear() {
        lines.clear()
        filledBoxes.clear()
        wireBoxes.clear()
        beaconBeams.clear()
        texts.clear()
        texturedQuads.clear()
    }
}

object RenderBatchManager {
    val renderConsumer = RenderConsumer()

    init {
        on<RenderEvent.Last> {
            val poseStack = context.poseStack()
            val collector = context.submitNodeCollector()
            val camera = context.levelState().cameraRenderState.pos

            poseStack.pushPose()
            poseStack.translate(-camera.x, -camera.y, -camera.z)

            poseStack.submitQueuedLinesAndWireBoxes(renderConsumer.lines, renderConsumer.wireBoxes, collector)
            poseStack.submitQueuedFilledBoxes(renderConsumer.filledBoxes, collector)
            poseStack.submitQueuedTexturedQuads(renderConsumer.texturedQuads, collector)
            poseStack.popPose()

            poseStack.submitQueuedBeaconBeams(renderConsumer.beaconBeams, collector, camera)
            poseStack.submitQueuedTexts(renderConsumer.texts, collector, camera)
            renderConsumer.clear()
        }
    }
}

private fun Int.isFullyOpaque(): Boolean = ((this ushr 24) and 0xFF) == 0xFF

private fun resolveLineRenderType(depth: Boolean, fullyOpaque: Boolean) = when {
    depth && fullyOpaque -> RenderTypes.LINES
    depth -> RenderTypes.LINES_TRANSLUCENT
    fullyOpaque -> CustomRenderType.LINES_ESP
    else -> CustomRenderType.LINES_TRANSLUCENT_ESP
}

private fun LineData.renderType() = resolveLineRenderType(
    depth = depth,
    fullyOpaque = color1.isFullyOpaque() && color2.isFullyOpaque()
)

private fun BoxData.lineRenderType() = resolveLineRenderType(
    depth = depth,
    fullyOpaque = color.isFullyOpaque()
)

private fun BoxData.filledRenderType() = if (depth) RenderTypes.debugFilledBox() else CustomRenderType.QUADS_ESP

// submitted geometry is drawn later in the frame, so every submit gets its own copy of the queued data (groupBy makes new lists)
private fun PoseStack.submitQueuedTexturedQuads(quads: List<TexturedQuadData>, collector: SubmitNodeCollector) {
    for ((texture, group) in quads.groupBy { it.texture }) {
        collector.submitCustomGeometry(this, RenderTypes.entityCutout(texture)) { pose, buffer ->
            for (quad in group) {
                fun vertex(p: Vec3, u: Float, v: Float) {
                    buffer.addVertex(pose, p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
                        .setColor(quad.color)
                        .setUv(u, v)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setUv2(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT)
                        .setNormal(pose, quad.nx, quad.ny, quad.nz)
                }

                vertex(quad.bl, 0f, 1f)
                vertex(quad.tl, 0f, 0f)
                vertex(quad.tr, 1f, 0f)
                vertex(quad.br, 1f, 1f)
            }
        }
    }
}

private fun PoseStack.submitQueuedLinesAndWireBoxes(lines: List<LineData>, wireBoxes: List<BoxData>, collector: SubmitNodeCollector) {
    for ((renderType, group) in lines.groupBy { it.renderType() }) {
        collector.submitCustomGeometry(this, renderType) { pose, buffer ->
            for (line in group) {
                PrimitiveRenderer.renderVector(
                    pose, buffer,
                    Vector3f(line.from.x.toFloat(), line.from.y.toFloat(), line.from.z.toFloat()),
                    line.to.subtract(line.from),
                    line.color1, line.color2, line.thickness
                )
            }
        }
    }

    for ((renderType, group) in wireBoxes.groupBy { it.lineRenderType() }) {
        collector.submitCustomGeometry(this, renderType) { pose, buffer ->
            for (box in group) PrimitiveRenderer.renderLineBox(pose, buffer, box.aabb, box.color, box.thickness)
        }
    }
}

private fun PoseStack.submitQueuedFilledBoxes(boxes: List<BoxData>, collector: SubmitNodeCollector) {
    for ((renderType, group) in boxes.groupBy { it.filledRenderType() }) {
        collector.submitCustomGeometry(this, renderType) { pose, buffer ->
            for (box in group) {
                PrimitiveRenderer.addChainedFilledBoxVertices(
                    pose, buffer,
                    box.aabb.minX.toFloat(), box.aabb.minY.toFloat(), box.aabb.minZ.toFloat(),
                    box.aabb.maxX.toFloat(), box.aabb.maxY.toFloat(), box.aabb.maxZ.toFloat(),
                    box.color
                )
            }
        }
    }
}

private fun PoseStack.submitQueuedBeaconBeams(beacons: List<BeaconData>, collector: SubmitNodeCollector, camera: Vec3) {
    for (beacon in beacons) {
        pushPose()
        translate(beacon.pos.x - camera.x, beacon.pos.y - camera.y, beacon.pos.z - camera.z)

        val dx = camera.x - (beacon.pos.x + 0.5)
        val dz = camera.z - (beacon.pos.z + 0.5)
        val length = sqrt(dx * dx + dz * dz).toFloat()

        val scale = if (beacon.isScoping) 1.0f else maxOf(1.0f, length * 0.010416667f)

        BeaconRenderer.submitBeaconBeam(
            this,
            collector,
            BeaconRenderer.BEAM_LOCATION,
            1f,
            beacon.gameTime.toFloat(),
            0,
            319,
            beacon.color,
            0.2f * scale,
            0.25f * scale
        )
        popPose()
    }
}

private fun PoseStack.submitQueuedTexts(texts: List<TextData>, collector: SubmitNodeCollector, camera: Vec3) {
    for (textData in texts) {
        pushPose()
        val scaleFactor = textData.scale * 0.025f

        last().pose()
            .translate(textData.pos.toVector3f())
            .translate(-camera.x.toFloat(), -camera.y.toFloat(), -camera.z.toFloat())
            .rotate(textData.cameraRotation)
            .scale(scaleFactor, -scaleFactor, scaleFactor)

        // parse the § formatting codes like the String overloads of Font do
        val text = FormattedCharSequence { sink -> StringDecomposer.iterateFormatted(textData.text, Style.EMPTY, sink) }

        collector.submitText(
            this, -textData.textWidth / 2f, 0f, text, true,
            if (textData.depth) Font.DisplayMode.POLYGON_OFFSET else Font.DisplayMode.SEE_THROUGH,
            LightCoordsUtil.FULL_BRIGHT, -1, 0, 0
        )

        popPose()
    }
}

fun RenderEvent.Extract.drawTexturedQuad(
    texture: Identifier,
    pos: Vec3,
    width: Float,
    height: Float,
    yaw: Float = 0f,
    color: Int = -1,
    depth: Boolean = true
) {
    val yawRad = Math.toRadians(yaw.toDouble())
    val rx = cos(yawRad).toFloat()
    val rz = sin(yawRad).toFloat()
    val hw = width * 0.5
    val hh = height * 0.5

    // right = (rx, 0, rz), up = (0, 1, 0), normal = cross(right, up) = (-rz, 0, rx)
    val bl = Vec3(pos.x - rx * hw, pos.y - hh, pos.z - rz * hw)
    val tl = Vec3(pos.x - rx * hw, pos.y + hh, pos.z - rz * hw)
    val tr = Vec3(pos.x + rx * hw, pos.y + hh, pos.z + rz * hw)
    val br = Vec3(pos.x + rx * hw, pos.y - hh, pos.z + rz * hw)

    consumer.texturedQuads.add(TexturedQuadData(texture, bl, tl, tr, br, -rz, 0f, rx, color, depth))
}

fun RenderEvent.Extract.drawTracer(to: Vec3, color: Int, depth: Boolean, thickness: Float = 3f) {
    val cam = context.levelState().cameraRenderState
    drawLine(listOf(cam.pos.add(Vec3.directionFromRotation(cam.xRot, cam.yRot)), to), color, depth, thickness)
}

fun RenderEvent.Extract.drawLine(points: Collection<Vec3>, color: Int, depth: Boolean, thickness: Float = 3f) {
    drawLine(points, color, color, depth, thickness)
}

fun RenderEvent.Extract.drawLine(points: Collection<Vec3>, color1: Int, color2: Int, depth: Boolean, thickness: Float = 3f) {
    if (points.size < 2) return

    val iterator = points.iterator()
    var current = iterator.next()

    while (iterator.hasNext()) {
        val next = iterator.next()
        consumer.lines.add(LineData(current, next, color1, color2, thickness, depth))
        current = next
    }
}

fun RenderEvent.Extract.drawWireFrameBox(aabb: AABB, color: Int, thickness: Float = 3f, depth: Boolean = false) {
    consumer.wireBoxes.add(BoxData(aabb, color, thickness, depth))
}

fun RenderEvent.Extract.drawFilledBox(aabb: AABB, color: Int, depth: Boolean = false) {
    consumer.filledBoxes.add(BoxData(aabb, color, 3f, depth))
}

fun RenderEvent.Extract.drawStyledBox(
    aabb: AABB,
    color: Int,
    style: Int = 0,
    depth: Boolean = true
) {
    when (style) {
        0 -> drawFilledBox(aabb, color, depth = depth)
        1 -> drawWireFrameBox(aabb, color, depth = depth)
        2 -> {
            drawFilledBox(aabb, fade(color, 0.5f), depth = depth)
            drawWireFrameBox(aabb, color, depth = depth)
        }
    }
}

fun RenderEvent.Extract.drawBeaconBeam(position: BlockPos, color: Int) {
    val isScoping = mc.player?.isScoping == true
    val gameTime = mc.level?.gameTime ?: 0L

    consumer.beaconBeams.add(BeaconData(position, color, isScoping, gameTime))
}

fun RenderEvent.Extract.drawText(text: String, pos: Vec3, scale: Float, depth: Boolean) {
    val cameraRotation = Quaternionf(context.levelState().cameraRenderState.orientation)
    val textWidth = mc.font.width(text).toFloat()

    consumer.texts.add(TextData(text, pos, scale, depth, cameraRotation, textWidth))
}

fun RenderEvent.Extract.drawCustomBeacon(
    title: String,
    position: BlockPos,
    color: Int,
    increase: Boolean = true,
    distance: Boolean = true
) {
    val dist = mc.player?.blockPosition()?.distManhattan(position) ?: return

    drawWireFrameBox(AABB(position), color, depth = false)
    drawBeaconBeam(position, color)
    drawText(
        (if (distance) ("$title §r§f(§3${dist}m§f)") else title),
        Vec3.atCenterOf(position).add(0.0, 1.7, 0.0),
        if (increase) max(1f, dist * 0.05f) else 2f,
        false
    )
}

fun RenderEvent.Extract.drawCylinder(
    center: Vec3,
    radius: Float,
    height: Float,
    color: Int,
    segments: Int = 32,
    thickness: Float = 5f,
    depth: Boolean = false
) {
    val angleStep = 2.0 * Math.PI / segments

    for (i in 0 until segments) {
        val angle1 = i * angleStep
        val angle2 = (i + 1) * angleStep

        val x1 = (radius * cos(angle1)).toFloat()
        val z1 = (radius * sin(angle1)).toFloat()
        val x2 = (radius * cos(angle2)).toFloat()
        val z2 = (radius * sin(angle2)).toFloat()

        val p1Top = center.add(x1.toDouble(), height.toDouble(), z1.toDouble())
        val p2Top = center.add(x2.toDouble(), height.toDouble(), z2.toDouble())
        val p1Bottom = center.add(x1.toDouble(), 0.0, z1.toDouble())
        val p2Bottom = center.add(x2.toDouble(), 0.0, z2.toDouble())

        consumer.lines.add(LineData(p1Top, p2Top, color, color, thickness, depth))
        consumer.lines.add(LineData(p1Bottom, p2Bottom, color, color, thickness, depth))
        consumer.lines.add(LineData(p1Bottom, p1Top, color, color, thickness, depth))
    }
}

object PrimitiveRenderer {

    private val edges = intArrayOf(
        0, 1,  1, 5,  5, 4,  4, 0,
        3, 2,  2, 6,  6, 7,  7, 3,
        0, 3,  1, 2,  5, 6,  4, 7
    )

    fun renderLineBox(
        pose: PoseStack.Pose,
        buffer: VertexConsumer,
        aabb: AABB,
        color: Int,
        thickness: Float
    ) {
        val x0 = aabb.minX.toFloat()
        val y0 = aabb.minY.toFloat()
        val z0 = aabb.minZ.toFloat()
        val x1 = aabb.maxX.toFloat()
        val y1 = aabb.maxY.toFloat()
        val z1 = aabb.maxZ.toFloat()

        val corners = floatArrayOf(
            x0, y0, z0,
            x1, y0, z0,
            x1, y1, z0,
            x0, y1, z0,
            x0, y0, z1,
            x1, y0, z1,
            x1, y1, z1,
            x0, y1, z1
        )

        for (i in edges.indices step 2) {
            val i0 = edges[i] * 3
            val i1 = edges[i + 1] * 3

            val sx = corners[i0]
            val sy = corners[i0 + 1]
            val sz = corners[i0 + 2]
            val ex = corners[i1]
            val ey = corners[i1 + 1]
            val ez = corners[i1 + 2]

            val dx = ex - sx
            val dy = ey - sy
            val dz = ez - sz

            buffer.addVertex(pose, sx, sy, sz).setColor(color).setNormal(pose, dx, dy, dz).setLineWidth(thickness)
            buffer.addVertex(pose, ex, ey, ez).setColor(color).setNormal(pose, dx, dy, dz).setLineWidth(thickness)
        }
    }

    fun addChainedFilledBoxVertices(
        pose: PoseStack.Pose,
        buffer: VertexConsumer,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        color: Int
    ) {
        val matrix = pose.pose()

        fun vertex(x: Float, y: Float, z: Float) {
            buffer.addVertex(matrix, x, y, z).setColor(color)
        }

        vertex(minX, minY, minZ)
        vertex(minX, minY, maxZ)
        vertex(minX, maxY, maxZ)
        vertex(minX, maxY, minZ)

        vertex(maxX, minY, maxZ)
        vertex(maxX, minY, minZ)
        vertex(maxX, maxY, minZ)
        vertex(maxX, maxY, maxZ)

        vertex(minX, minY, minZ)
        vertex(minX, maxY, minZ)
        vertex(maxX, maxY, minZ)
        vertex(maxX, minY, minZ)

        vertex(maxX, minY, maxZ)
        vertex(maxX, maxY, maxZ)
        vertex(minX, maxY, maxZ)
        vertex(minX, minY, maxZ)

        vertex(minX, minY, minZ)
        vertex(maxX, minY, minZ)
        vertex(maxX, minY, maxZ)
        vertex(minX, minY, maxZ)

        vertex(minX, maxY, maxZ)
        vertex(maxX, maxY, maxZ)
        vertex(maxX, maxY, minZ)
        vertex(minX, maxY, minZ)
    }

    fun renderVector(
        pose: PoseStack.Pose,
        buffer: VertexConsumer,
        start: Vector3f,
        direction: Vec3,
        startColor: Int,
        endColor: Int,
        thickness: Float
    ) {
        val endX = start.x() + direction.x.toFloat()
        val endY = start.y() + direction.y.toFloat()
        val endZ = start.z() + direction.z.toFloat()

        val nx = direction.x.toFloat()
        val ny = direction.y.toFloat()
        val nz = direction.z.toFloat()

        buffer.addVertex(pose, start.x(), start.y(), start.z())
            .setColor(startColor)
            .setNormal(pose, nx, ny, nz)
            .setLineWidth(thickness)

        buffer.addVertex(pose, endX, endY, endZ)
            .setColor(endColor)
            .setNormal(pose, nx, ny, nz)
            .setLineWidth(thickness)
    }
}
