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
import org.solidhax.shiro.utils.texturesProfile
import org.solidhax.shiro.utils.ui.NameTagSegments
import org.solidhax.shiro.utils.ui.entityNameTag
import org.solidhax.shiro.utils.ui.labelSegments
import kotlin.math.roundToInt

object LittlefootESP : Module(
    name = "Littlefoot ESP",
    description = "Highlights the Littlefoot entity in Glacite Mineshafts."
) {
    private val highlight by BooleanSetting("Highlight", false, desc = "Highlights Littlefoot.")
    private val showName by BooleanSetting("Show Name", false, desc = "Shows Littlefoot's name above it.")
    private val showDistance by BooleanSetting("Show Distance", false, desc = "Shows how far away Littlefoot is.")
    private val highlightColor by ColorSetting("Highlight Color", DEFAULT_COLOR, desc = "Color used to highlight Littlefoot.")

    private val nameTagPosition = +LabelPositionSetting("Name Tag Position", desc = "Which side of Littlefoot the name tag sits on. Drag it in the preview to move it.")

    private val profile by lazy { texturesProfile(LITTLEFOOT_TEXTURES.first()) }
    private val preview = +PreviewSetting("Preview", EntityPreview(DummyEntity { RemotePlayer(it, profile) }, "Littlefoot", PreviewLabel(nameTagPosition) { nameTagSegments(PREVIEW_DISTANCE) }) {
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
            val player = mc.player ?: return@on
            for (entity in littlefoots) {
                graphics.entityNameTag(entity, partialTick, nameTagPosition.value, nameTagSegments(player.distanceTo(entity).roundToInt()))
            }
        }

        on<LevelEvent.Load> {
            littlefoots.clear()
        }
    }

    private fun nameTagSegments(distance: Int): NameTagSegments =
        labelSegments(title = "Littlefoot".takeIf { showName }, titleColor = highlightColor, distance = distance.takeIf { showDistance })

    private val LITTLEFOOT_TEXTURES = setOf(
        "f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b",
        "a3bd16079f764cd541e072e888fe43885e711f98658323db0f9a6045da91ee7a",
        "c7a7eeadb46a1e7beb9ce5cf8d79d732839524dbaceb51a3d160da71c77452c7",
    )
    private const val DEFAULT_COLOR = 0xFF7FD6FF.toInt()
    private const val PREVIEW_DISTANCE = 12
}
