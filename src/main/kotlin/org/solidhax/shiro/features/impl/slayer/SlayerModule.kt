package org.solidhax.shiro.features.impl.slayer

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import org.solidhax.shiro.events.EntityGlowEvent
import org.solidhax.shiro.events.EntityRenderEvent
import org.solidhax.shiro.events.HudRenderEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme
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

abstract class SlayerModule(
    val type: SlayerType,
    previewEntity: () -> Entity?,
    example: SlayerInfo,
) : Module(
    name = type.displayName,
    description = "Features for the ${type.displayName} slayer boss."
) {
    private val onlyOwnBoss by BooleanSetting("Only Own Boss", false, desc = "Only applies to bosses you spawned.")

    private val highlightDropdown = +DropdownSetting("Highlight")
    private val highlight by BooleanSetting("Enabled", false, desc = "Highlights the boss.").withDependency(highlightDropdown)
    private val highlightColor by ColorSetting("Color", 0xFFFF5555.toInt(), desc = "Color used to highlight the boss.").withDependency(highlightDropdown)

    protected val infoDropdown = +DropdownSetting("Info")
    private val showInfo by BooleanSetting("Enabled", true, desc = "Shows the boss's timer, name and health in cleaner labels.").withDependency(infoDropdown)
    private val hideOriginal by BooleanSetting("Hide Original", true, desc = "Hides the boss's own name tags.").withDependency(infoDropdown)
    private val showTimer by BooleanSetting("Show Timer", true, desc = "Shows the time left to kill the boss.").withDependency(infoDropdown)
    private val showName by BooleanSetting("Show Name", true, desc = "Shows the boss's name and tier.").withDependency(infoDropdown)
    private val showHealth by BooleanSetting("Show Health", true, desc = "Shows the boss's health.").withDependency(infoDropdown)
    private val showKillTime by BooleanSetting("Show Kill Time", true, desc = "Shows how long the boss took to kill in the name label after it dies.").withDependency(infoDropdown)

    protected val timerColor = ColorSetting("Color", 0xFF55FFFF.toInt(), desc = "Color of the timer.")
    protected val timerPosition = +LabelPositionSetting(
        "Timer Label Position",
        desc = "Where the timer label sits. Drag it in the preview to move it.",
        textSettings = listOf(timerColor),
    )

    private val nameColor = ColorSetting("Color", 0xFFAA0000.toInt(), desc = "Color of the boss's name.")
    private val shortNames = BooleanSetting("Short Names", true, desc = "Uses the boss's short name, like ${type.shortName}.")
    private val killTimeColor = ColorSetting("Kill Time Color", 0xFFFF5555.toInt(), desc = "Color of the kill time.")
    private val namePosition = +LabelPositionSetting(
        "Name Label Position",
        desc = "Where the name label sits, and the kill time once the boss dies. Drag it in the preview to move it.",
        textSettings = listOf(nameColor, shortNames, killTimeColor),
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

    private val preview = +PreviewSetting(
        "Preview",
        EntityPreview(
            previewEntity,
            type.displayName,
            listOf(
                PreviewLabel(healthPosition) { healthSegments(example) },
                PreviewLabel(namePosition) { nameSegments(example) },
                PreviewLabel(timerPosition) { timerSegments(example) },
            ),
        ),
    )

    init {
        on<EntityGlowEvent> {
            if (!highlight) return@on
            val slayer = SlayerTracker[entity] ?: return@on
            if (isShown(slayer) && !slayer.isDead) color = highlightColor
        }

        on<EntityRenderEvent> {
            if (!showInfo || !hideOriginal || entity !is ArmorStand) return@on
            val slayer = SlayerTracker.ofStand(entity) ?: return@on
            if (isShown(slayer)) cancel()
        }

        on<HudRenderEvent> {
            if (!showInfo) return@on
            for (slayer in SlayerTracker.tracked) {
                if (!isShown(slayer) || (slayer.isDead && !showKillTime)) continue
                graphics.entityLabels(slayer.entity, partialTick, labels(slayer.info))
            }
        }
    }

    protected fun isShown(slayer: Slayer): Boolean = slayer.type == type && (!onlyOwnBoss || slayer.owned)

    protected open fun timerText(info: SlayerInfo): LabelSegments =
        info.timer?.let { listOf(it to color(timerColor.value)) }.orEmpty()

    protected fun color(color: Int) = CascadeGeometricColor(color)

    private fun labels(info: SlayerInfo): List<Label> {
        info.killTime?.let { return listOf(namePosition.label(listOf(formatDuration(it) to color(killTimeColor.value)))) }
        return listOf(
            healthPosition.label(healthSegments(info)),
            namePosition.label(nameSegments(info)),
            timerPosition.label(timerSegments(info)),
        )
    }

    private fun timerSegments(info: SlayerInfo): LabelSegments =
        if (showInfo && showTimer) timerText(info) else emptyList()

    private fun nameSegments(info: SlayerInfo): LabelSegments {
        if (!showInfo || !showName) return emptyList()
        val name = if (shortNames.enabled) type.shortName else type.displayName
        return listOf("$name ${info.tier ?: "???"}" to color(nameColor.value))
    }

    private fun healthSegments(info: SlayerInfo): LabelSegments {
        if (!showInfo || !showHealth) return emptyList()
        val color = color(healthTextColor(info))
        val hits = info.hits ?: return info.health?.let { listOf(it to color) }.orEmpty()
        return buildList {
            add("$hits Hits" to color)
            info.health?.let { add(" $it" to theme.textMuted) }
        }
    }

    private fun healthTextColor(info: SlayerInfo): Int {
        if (healthColorMode.value != COLOR_MODE_FADE) return healthColor.value
        return lerpColor(healthFadeEnd.value, healthFadeStart.value, info.healthFraction ?: 1f)
    }

    private fun formatDuration(seconds: Double): String {
        val minutes = (seconds / 60.0).toInt()
        val rest = String.format(Locale.ROOT, "%.1fs", seconds - minutes * 60)
        return if (minutes > 0) "${minutes}m $rest" else rest
    }

    private companion object {
        const val COLOR_MODE_STATIC = 0
        const val COLOR_MODE_FADE = 1
    }
}
