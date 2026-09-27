package org.solidhax.shiro.features.impl.mining

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.states.impl.image.data.CascadeImageFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.utils.PlayerPosition
import org.solidhax.shiro.utils.localPlayerPosition
import org.solidhax.shiro.utils.localSkinTexture
import org.solidhax.shiro.utils.skyblock.Island
import org.solidhax.shiro.utils.skyblock.LocationUtils
import org.solidhax.shiro.utils.truncate
import org.solidhax.shiro.utils.ui.BLACK
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.WHITE
import org.solidhax.shiro.utils.ui.centeredText
import org.solidhax.shiro.utils.ui.withAlpha
import kotlin.math.abs
import kotlin.math.floor

object CrystalHollowsMap : Module(
    name = "Crystal Hollows Map",
    description = "Shows a map of the Crystal Hollows with your position."
) {
    private val showLabels by BooleanSetting("Show Labels", true, desc = "Writes the zone names on the map.")
    private val showGemstones by BooleanSetting("Show Gemstones", true, desc = "Writes which gemstone each zone has below its name.")
    private val showLocation by BooleanSetting("Show Location", true, desc = "Shows the zone you are in and your coordinates below the map.")
    private val headSize by NumberSetting("Head Size", 10f, 6, 16, 1, desc = "Size of your head on the map.", unit = "px")

    private val mapHud by HUD("Crystal Hollows Map", "Map of the Crystal Hollows.", toggleable = false) { example ->
        if (!example && !LocationUtils.isCurrentArea(Island.CrystalHollows)) return@HUD 0f to 0f

        drawZones()
        val position = if (example) EXAMPLE_POSITION else localPlayerPosition() ?: EXAMPLE_POSITION
        drawHead(position.x.toMap(), position.z.toMap(), position.yaw)

        if (!showLocation) return@HUD MAP_SIZE to MAP_SIZE
        val zone = if (example) NUCLEUS_NAME else LocationUtils.subArea ?: zoneAt(position.x, position.z)
        val coordinates = "${floor(position.x).toInt()}, ${floor(position.y).toInt()}, ${floor(position.z).toInt()}"
        val zoneY = MAP_SIZE + LOCATION_GAP
        val coordinatesY = zoneY + TEXT_SIZE + LINE_GAP
        centeredText(zone.truncate(MAP_SIZE, TEXT_SIZE), MAP_SIZE / 2f, zoneY, CascadeGeometricColor(WHITE))
        centeredText(coordinates.truncate(MAP_SIZE, TEXT_SIZE), MAP_SIZE / 2f, coordinatesY, CascadeGeometricColor(WHITE))
        MAP_SIZE to coordinatesY + TEXT_SIZE
    }

    private fun GuiGraphicsExtractor.drawZones() {
        for (zone in Zone.entries) {
            val x = zone.column * (TILE_SIZE + TILE_GAP)
            val y = zone.row * (TILE_SIZE + TILE_GAP)
            roundedRectangle(x, y, TILE_SIZE, TILE_SIZE, zone.color, Radius.LARGE)

            val lines = buildList {
                if (showLabels) zone.label.forEach { add(it to NAME_COLOR) }
                if (showGemstones) add(zone.gemstone to GEMSTONE_COLOR)
            }
            drawLines(lines, x + TILE_SIZE / 2f, y + TILE_SIZE / 2f, ZONE_LABEL_SIZE)
        }

        val nucleusX = (MAP_SIZE - NUCLEUS_SIZE) / 2f
        roundedRectangle(nucleusX, nucleusX, NUCLEUS_SIZE, NUCLEUS_SIZE, NUCLEUS_COLOR, Radius.LARGE)
        if (showLabels) drawLines(listOf("Nucleus" to NAME_COLOR), MAP_SIZE / 2f, MAP_SIZE / 2f, NUCLEUS_LABEL_SIZE)
    }

    private fun GuiGraphicsExtractor.drawLines(lines: List<Pair<String, Int>>, centerX: Float, centerY: Float, size: Float) {
        var y = centerY - lines.size * size / 2f
        for ((line, color) in lines) {
            centeredText(line, centerX, y, CascadeGeometricColor(color), size)
            y += size
        }
    }

    private fun GuiGraphicsExtractor.drawHead(x: Float, y: Float, yaw: Float) {
        val skin = localSkinTexture()
        val half = headSize / 2f
        val reach = half * SQRT_2 + HEAD_BORDER

        pose().pushMatrix()
        pose().translate(x.coerceIn(reach, MAP_SIZE - reach), y.coerceIn(reach, MAP_SIZE - reach))
        pose().rotate((yaw - 180f) * Mth.DEG_TO_RAD)
        roundedRectangle(-half - HEAD_BORDER, -half - HEAD_BORDER, headSize + HEAD_BORDER * 2f, headSize + HEAD_BORDER * 2f, BLACK, Radius.MEDIUM)
        for (u in FACE_LAYER_U) {
            image(skin, -half, -half, headSize, headSize, u, FACE_V0, u + FACE_UV_SIZE, FACE_V1, CascadeGeometricColor.WHITE, Radius.SMALL, CascadeImageFilter.NEAREST)
        }
        pose().popMatrix()
    }

    private fun zoneAt(x: Double, z: Double): String {
        if (abs(x - CENTER) < NUCLEUS_RADIUS && abs(z - CENTER) < NUCLEUS_RADIUS) return NUCLEUS_NAME
        val column = if (x < CENTER) 0 else 1
        val row = if (z < CENTER) 0 else 1
        return Zone.entries.first { it.column == column && it.row == row }.displayName
    }

    private fun Double.toMap(): Float = ((this - MIN_COORD) / MAP_BLOCKS * MAP_SIZE).toFloat()

    private enum class Zone(val displayName: String, val label: List<String>, val gemstone: String, val column: Int, val row: Int, val color: Int) {
        JUNGLE("Jungle", listOf("Jungle"), "Amethyst", 0, 0, 0xFF5E0B61.toInt()),
        MITHRIL_DEPOSITS("Mithril Deposits", listOf("Mithril", "Deposits"), "Jade", 1, 0, 0xFF0B560B.toInt()),
        GOBLIN_HOLDOUT("Goblin Holdout", listOf("Goblin", "Holdout"), "Amber", 0, 1, 0xFF8A5A00.toInt()),
        PRECURSOR_REMNANTS("Precursor Remnants", listOf("Precursor", "Remnants"), "Sapphire", 1, 1, 0xFF2B7B79.toInt()),
    }

    private const val MIN_COORD = 202.0
    private const val MAP_BLOCKS = 621.0
    private const val CENTER = 513.0
    private const val NUCLEUS_RADIUS = 51.0
    private const val NUCLEUS_NAME = "Crystal Nucleus"

    private const val MAP_SIZE = 128f
    private const val TILE_GAP = 3f
    private const val TILE_SIZE = (MAP_SIZE - TILE_GAP) / 2f
    private const val NUCLEUS_SIZE = 24f
    private const val NUCLEUS_COLOR = 0xFF3A3D46.toInt()
    private const val ZONE_LABEL_SIZE = 7f
    private const val NUCLEUS_LABEL_SIZE = 5f
    private const val NAME_COLOR = WHITE
    private val GEMSTONE_COLOR = withAlpha(WHITE, 140)
    private const val LOCATION_GAP = 5f
    private const val LINE_GAP = 2f

    private const val HEAD_BORDER = 1f
    private const val SQRT_2 = 1.4142135f
    private const val FACE_UV_SIZE = 8f / 64f
    private const val FACE_V0 = 8f / 64f
    private const val FACE_V1 = 16f / 64f
    private val FACE_LAYER_U = floatArrayOf(8f / 64f, 40f / 64f)

    private val EXAMPLE_POSITION = PlayerPosition(513.0, 106.0, 526.0, 180f)
}
