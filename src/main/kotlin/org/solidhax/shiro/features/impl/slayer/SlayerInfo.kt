package org.solidhax.shiro.features.impl.slayer

import com.mojang.authlib.GameProfile
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.decoration.ArmorStand
import org.solidhax.shiro.events.EntityGlowEvent
import org.solidhax.shiro.events.EntityRenderEvent
import org.solidhax.shiro.events.HudRenderEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.DummyEntity
import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.PreviewLabel
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.LabelPositionSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import org.solidhax.shiro.gui.settings.impl.SelectorSetting
import org.solidhax.shiro.utils.ui.Label
import org.solidhax.shiro.utils.ui.LabelSegments
import org.solidhax.shiro.utils.ui.entityLabels
import org.solidhax.shiro.utils.ui.lerpColor
import java.util.Locale
import java.util.UUID

object SlayerInfo : Module(
    name = "Slayer Info",
    description = "Shows information about slayer bosses in cleaner labels."
) {
    private val onlyOwnBoss by BooleanSetting("Only Own Boss", false, desc = "Only applies to bosses you spawned.")
    private val hideOriginal by BooleanSetting("Hide Original", true, desc = "Hides the boss's own name tags.")

    private val bosses = SlayerType.entries.associateWith(::bossSettings)

    private val timerColor = ColorSetting("Color", 0xFF55FFFF.toInt(), desc = "Color of the timer.")
    private val timerPosition = +LabelPositionSetting(
        "Timer Label Position",
        desc = "Where the timer label sits. Drag it in the preview to move it.",
        textSettings = listOf(timerColor),
    )

    private val nameColor = ColorSetting("Color", 0xFFAA0000.toInt(), desc = "Color of the boss's name.")
    private val shortNames = BooleanSetting("Short Names", true, desc = "Uses short boss names like Void instead of Voidgloom Seraph.")
    private val namePosition = +LabelPositionSetting(
        "Name Label Position",
        desc = "Where the name label sits. Drag it in the preview to move it.",
        textSettings = listOf(nameColor, shortNames),
    )

    private val healthColorMode = SelectorSetting("Color Mode", "Static", listOf("Static", "Fade"), desc = "Whether the health uses one color or fades between two as the boss loses health.")
    private val healthColor = ColorSetting("Color", 0xFF55FFFF.toInt(), desc = "Color of the boss's health.")
        .withDependency { healthColorMode.value == COLOR_MODE_STATIC }
    private val healthFadeStart = ColorSetting("Fade Start", 0xFF55FF55.toInt(), desc = "Color of the health when the boss is at full health.")
        .withDependency { healthColorMode.value == COLOR_MODE_FADE }
    private val healthFadeEnd = ColorSetting("Fade End", 0xFFFF5555.toInt(), desc = "Color of the health when the boss is almost dead.")
        .withDependency { healthColorMode.value == COLOR_MODE_FADE }
    private val healthPosition = +LabelPositionSetting(
        "Health Label Position",
        desc = "Where the health label sits. Drag it in the preview to move it.",
        textSettings = listOf(healthColorMode, healthColor, healthFadeStart, healthFadeEnd),
    )

    private val preview = +PreviewSetting("Preview", *SlayerType.entries.map(::bossPreview).toTypedArray())

    init {
        on<EntityGlowEvent> {
            val slayer = SlayerTracker[entity] ?: return@on
            val boss = bosses.getValue(slayer.type)
            if (boss.highlight.enabled && isShown(slayer)) color = boss.highlightColor.value
        }

        on<EntityRenderEvent> {
            if (!hideOriginal || entity !is ArmorStand) return@on
            val slayer = SlayerTracker.ofStand(entity) ?: return@on
            if (isShown(slayer)) cancel()
        }

        on<HudRenderEvent> {
            for (slayer in SlayerTracker.tracked) {
                if (!isShown(slayer)) continue
                graphics.entityLabels(slayer.entity, partialTick, labels(slayer.info))
            }
        }
    }

    private fun isShown(slayer: Slayer): Boolean = bosses.getValue(slayer.type).enabled.enabled && (!onlyOwnBoss || slayer.owned)

    private fun labels(info: SlayerData): List<Label> {
        return listOf(
            healthPosition.label(healthSegments(info)),
            namePosition.label(nameSegments(info)),
            timerPosition.label(timerSegments(info)),
        )
    }

    private fun timerSegments(info: SlayerData): LabelSegments {
        if (!bosses.getValue(info.type).showTimer.enabled) return emptyList()
        val timer = info.timer ?: return emptyList()
        return when (info.type) {
            SlayerType.VOIDGLOOM -> laserSegments(info, timer)
            SlayerType.INFERNO -> attunementSegments(timer)
            else -> null
        } ?: listOf(timer to color(timerColor.value))
    }

    private fun laserSegments(info: SlayerData, timer: String): LabelSegments? {
        val ticks = info.vehicleTicks ?: return null
        val laser = (LASER_SECONDS - ticks / 20.0).coerceAtLeast(0.0)
        val laserText = if (laser <= 0.0) "Soon" else String.format(Locale.ROOT, "%.1fs", laser)
        return listOf(laserText to color(timerColor.value), " $timer" to theme.textMuted)
    }

    private fun attunementSegments(timer: String): LabelSegments? {
        val (attunement, count, time) = ATTUNEMENT_REGEX.matchEntire(timer)?.destructured ?: return null
        return listOf("$attunement $count" to color(attunementColor(attunement)), " $time" to theme.textMuted)
    }

    private fun attunementColor(attunement: String): Int {
        if (bosses.getValue(SlayerType.INFERNO).attunementColors?.enabled != true) return timerColor.value
        return when (attunement) {
            "ASHEN" -> 0xFF555555.toInt()
            "AURIC" -> 0xFFFFAA00.toInt()
            "CRYSTAL" -> 0xFF55FFFF.toInt()
            "SPIRIT" -> 0xFFFFFFFF.toInt()
            else -> timerColor.value
        }
    }

    private fun nameSegments(info: SlayerData): LabelSegments {
        if (!bosses.getValue(info.type).showName.enabled) return emptyList()
        val name = if (shortNames.enabled) info.type.shortName else info.type.displayName
        return listOf("$name ${info.tier ?: "???"}" to color(nameColor.value))
    }

    private fun healthSegments(info: SlayerData): LabelSegments {
        if (!bosses.getValue(info.type).showHealth.enabled) return emptyList()
        val color = color(healthTextColor(info))
        val hits = info.hits ?: return info.health?.let { listOf(it to color) }.orEmpty()
        return buildList {
            add("$hits Hits" to color)
            info.health?.let { add(" $it" to theme.textMuted) }
        }
    }

    private fun healthTextColor(info: SlayerData): Int {
        if (healthColorMode.value != COLOR_MODE_FADE) return healthColor.value
        return lerpColor(healthFadeEnd.value, healthFadeStart.value, info.healthFraction ?: 1f)
    }

    private fun color(color: Int) = CascadeGeometricColor(color)

    private fun bossSettings(type: SlayerType): BossSettings {
        val name = type.displayName
        val dropdown = +DropdownSetting(name)
        return BossSettings(
            enabled = +BooleanSetting("Enabled", true, desc = "Shows info for the $name.").withDependency(dropdown),
            highlight = +BooleanSetting("Highlight", false, desc = "Highlights the $name.").withDependency(dropdown),
            highlightColor = +ColorSetting("Highlight Color", 0xFFFF5555.toInt(), desc = "Color used to highlight the $name.").withDependency(dropdown),
            showTimer = +BooleanSetting("Show Timer", true, desc = "Shows the time left to kill the $name.").withDependency(dropdown),
            showName = +BooleanSetting("Show Name", true, desc = "Shows the $name's name and tier.").withDependency(dropdown),
            showHealth = +BooleanSetting("Show Health", true, desc = "Shows the $name's health.").withDependency(dropdown),
            attunementColors = if (type != SlayerType.INFERNO) null
            else +BooleanSetting("Attunement Colors", true, desc = "Colors the attunement in the timer by its type.").withDependency(dropdown),
        )
    }

    private fun bossPreview(type: SlayerType) = EntityPreview(
        previewEntity(type),
        type.displayName,
        listOf(
            PreviewLabel(healthPosition) { healthSegments(EXAMPLES.getValue(type)) },
            PreviewLabel(namePosition) { nameSegments(EXAMPLES.getValue(type)) },
            PreviewLabel(timerPosition) { timerSegments(EXAMPLES.getValue(type)) },
        ),
    )

    private fun previewEntity(type: SlayerType): () -> Entity? = when (type) {
        SlayerType.REVENANT -> DummyEntity(EntityTypes.ZOMBIE)
        SlayerType.TARANTULA -> DummyEntity(EntityTypes.SPIDER)
        SlayerType.SVEN -> DummyEntity(EntityTypes.WOLF)
        SlayerType.VOIDGLOOM -> DummyEntity(EntityTypes.ENDERMAN)
        SlayerType.INFERNO -> DummyEntity(EntityTypes.BLAZE)
        SlayerType.VAMPIRE -> DummyEntity.player { GameProfile(UUID(0L, 0L), "Bloodfiend") }
    }

    private class BossSettings(
        val enabled: BooleanSetting,
        val highlight: BooleanSetting,
        val highlightColor: ColorSetting,
        val showTimer: BooleanSetting,
        val showName: BooleanSetting,
        val showHealth: BooleanSetting,
        val attunementColors: BooleanSetting?,
    )

    private const val COLOR_MODE_STATIC = 0
    private const val COLOR_MODE_FADE = 1
    private const val LASER_SECONDS = 8.2

    private val ATTUNEMENT_REGEX = Regex("""^([A-Z]+) ♨(\d+) (.+)$""")

    private val EXAMPLES = mapOf(
        SlayerType.REVENANT to SlayerData(SlayerType.REVENANT, "V", timer = "02:31", health = "8.4M", maxHealth = 10_000_000.0),
        SlayerType.TARANTULA to SlayerData(SlayerType.TARANTULA, "IV", timer = "03:12", health = "1.5M", maxHealth = 2_000_000.0),
        SlayerType.SVEN to SlayerData(SlayerType.SVEN, "IV", timer = "02:59", health = "1.4M", maxHealth = 2_000_000.0),
        SlayerType.VOIDGLOOM to SlayerData(SlayerType.VOIDGLOOM, "IV", timer = "02:47", hits = 15, health = "45.2M", maxHealth = 50_000_000.0),
        SlayerType.INFERNO to SlayerData(SlayerType.INFERNO, "IV", timer = "ASHEN ♨3 02:58", health = "120M", maxHealth = 150_000_000.0),
        SlayerType.VAMPIRE to SlayerData(SlayerType.VAMPIRE, "V", timer = "03:30", health = "4.2k", maxHealth = 6_000.0),
    )
}
