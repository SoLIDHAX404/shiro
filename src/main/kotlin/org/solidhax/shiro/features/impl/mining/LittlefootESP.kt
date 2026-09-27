package org.solidhax.shiro.features.impl.mining

import net.minecraft.client.player.RemotePlayer
import org.solidhax.shiro.events.EntityGlowEvent
import org.solidhax.shiro.events.HudRenderEvent
import org.solidhax.shiro.events.LevelEvent
import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.DummyEntity
import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.PreviewLabel
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.LabelPositionSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import org.solidhax.shiro.utils.skinTexture
import org.solidhax.shiro.utils.skyblock.Island
import org.solidhax.shiro.utils.skyblock.LocationUtils
import org.solidhax.shiro.utils.playerDistance
import org.solidhax.shiro.utils.texturesProfile
import org.solidhax.shiro.utils.ui.LabelSegments
import org.solidhax.shiro.utils.ui.PREVIEW_DISTANCE
import org.solidhax.shiro.utils.ui.entityLabel
import org.solidhax.shiro.utils.ui.labelSegments

object LittlefootESP : Module(
    name = "Littlefoot ESP",
    description = "Highlights the Littlefoot entity in Glacite Mineshafts."
) {
    private val highlight by BooleanSetting("Highlight", false, desc = "Highlights Littlefoot.")
    private val showName by BooleanSetting("Show Name", false, desc = "Shows Littlefoot's name above it.")
    private val showDistance by BooleanSetting("Show Distance", false, desc = "Shows how far away Littlefoot is.")
    private val highlightColor by ColorSetting("Highlight Color", DEFAULT_COLOR, desc = "Color used to highlight Littlefoot.")

    private val labelPosition = +LabelPositionSetting("Label Position", desc = "Where Littlefoot's label sits. Drag it in the preview to move it.")

    private val profile by lazy { texturesProfile(LITTLEFOOT_TEXTURES.first()) }
    private val preview = +PreviewSetting("Preview", EntityPreview(DummyEntity.player { profile }, "Littlefoot", listOf(PreviewLabel(labelPosition) { segments(PREVIEW_DISTANCE) })) {
        skin = mc.skinManager.createLookup(profile, false)::get
    })

    private val littlefoots = HashSet<RemotePlayer>()

    init {
        on<TickEvent.End> {
            littlefoots.clear()
            if (!LocationUtils.isCurrentArea(Island.Mineshaft)) return@on

            for (entity in level.entitiesForRendering()) {
                if (entity is RemotePlayer && entity.isAlive && entity.gameProfile.skinTexture in LITTLEFOOT_TEXTURES) littlefoots.add(entity)
            }
        }

        on<EntityGlowEvent> {
            if (highlight && entity in littlefoots) color = highlightColor
        }

        on<HudRenderEvent> {
            for (entity in littlefoots) {
                graphics.entityLabel(entity, partialTick, labelPosition.label(segments(playerDistance(entity.position()))))
            }
        }

        on<LevelEvent.Load> {
            littlefoots.clear()
        }
    }

    private fun segments(distance: Int?): LabelSegments =
        labelSegments(title = "Littlefoot".takeIf { showName }, titleColor = highlightColor, distance = distance?.takeIf { showDistance })

    private val LITTLEFOOT_TEXTURES = setOf(
        "f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b",
        "a3bd16079f764cd541e072e888fe43885e711f98658323db0f9a6045da91ee7a",
        "c7a7eeadb46a1e7beb9ce5cf8d79d732839524dbaceb51a3d160da71c77452c7",
    )
    private const val DEFAULT_COLOR = 0xFF7FD6FF.toInt()
}
