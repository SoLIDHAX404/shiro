package org.solidhax.shiro.features.impl.mining

import com.mojang.authlib.GameProfile
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.DyedItemColor
import org.solidhax.shiro.events.EntityGlowEvent
import org.solidhax.shiro.events.HudRenderEvent
import org.solidhax.shiro.events.LevelEvent
import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.DummyEntity
import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.PreviewLabel
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.ActionSetting
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.LabelPositionSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import org.solidhax.shiro.utils.createSkullStack
import org.solidhax.shiro.utils.render.ItemRenderer
import org.solidhax.shiro.utils.render.itemStack
import org.solidhax.shiro.utils.skyblock.Island
import org.solidhax.shiro.utils.skyblock.LocationUtils
import org.solidhax.shiro.utils.ui.NameTagSegments
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.entityNameTag
import org.solidhax.shiro.utils.ui.labelSegments
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth
import java.util.UUID
import kotlin.math.roundToInt

object CorpseESP : Module(
    name = "Corpse ESP",
    description = "Highlights corpses in Glacite Mineshafts."
) {
    private val corpseSettings = CorpseType.entries.associateWith(::corpseSettings)

    private val nameTagPosition = +LabelPositionSetting("Name Tag Position", desc = "Which side of each corpse the name tag sits on. Drag it in the preview to move it.")

    private val previewCorpse = DummyEntity { RemotePlayer(it, GameProfile(UUID(0L, 0L), "Steve")) }

    private val preview = +PreviewSetting("Preview", *CorpseType.entries.map(::corpsePreview).toTypedArray())

    private val breakdownHud by HUD("Corpse Breakdown", "Shows how many unique corpses of each type you find per hour.") { example ->
        if (!example && sessionStart == 0L) return@HUD 0f to 0f

        var width = 0f
        BREAKDOWN_ORDER.forEachIndexed { index, type ->
            val rowY = index * (ItemRenderer.ITEM_SIZE + BREAKDOWN_ROW_GAP)
            val textY = rowY + (ItemRenderer.ITEM_SIZE - TEXT_SIZE) / 2f
            val label = "${type.displayName}: "
            val rate = "${if (example) EXAMPLE_RATES.getValue(type) else perHour(type)}/h"
            val textX = ItemRenderer.ITEM_SIZE + BREAKDOWN_ICON_GAP

            itemStack(type.icon, 0f, rowY)
            text(label, textX, textY, theme.text)
            text(rate, textX + textWidth(label), textY, theme.textMuted)
            width = maxOf(width, textX + textWidth(label) + textWidth(rate))
        }
        width to BREAKDOWN_ORDER.size * (ItemRenderer.ITEM_SIZE + BREAKDOWN_ROW_GAP) - BREAKDOWN_ROW_GAP
    }

    private val resetBreakdownAction = +ActionSetting("Reset Breakdown", desc = "Clears the corpse breakdown and restarts its timer.") { resetBreakdown() }

    private val corpses = HashMap<ArmorStand, CorpseType>()
    private val countedCorpses = HashSet<UUID>()
    private val corpseCounts = HashMap<CorpseType, Int>()
    private var sessionStart = 0L

    init {
        on<TickEvent.End> {
            corpses.clear()
            if (!LocationUtils.isCurrentArea(Island.Mineshaft)) return@on
            if (sessionStart == 0L) sessionStart = System.currentTimeMillis()

            for (entity in level.entitiesForRendering()) {
                if (entity !is ArmorStand || !entity.isAlive || entity.isInvisible || entity.name.string != "Armor Stand") continue
                val type = CorpseType.fromHelmet(entity.getItemBySlot(EquipmentSlot.HEAD).customName?.string) ?: continue
                corpses[entity] = type
                if (countedCorpses.add(entity.uuid)) corpseCounts.merge(type, 1, Int::plus)
            }
        }

        on<EntityGlowEvent> {
            val settings = corpses[entity]?.settings ?: return@on
            if (settings.highlight.value) color = settings.color.value
        }

        on<HudRenderEvent> {
            val player = mc.player ?: return@on
            for ((entity, type) in corpses) {
                graphics.entityNameTag(entity, partialTick, nameTagPosition.value, nameTagSegments(type, player.distanceTo(entity).roundToInt()))
            }
        }

        on<LevelEvent.Load> {
            corpses.clear()
        }
    }

    private val CorpseType.settings: CorpseSettings get() = corpseSettings.getValue(this)

    private fun perHour(type: CorpseType): Int {
        val hours = (System.currentTimeMillis() - sessionStart) / MILLIS_PER_HOUR
        return if (hours <= 0.0) 0 else ((corpseCounts[type] ?: 0) / hours).roundToInt()
    }

    private fun resetBreakdown() {
        corpseCounts.clear()
        sessionStart = if (LocationUtils.isCurrentArea(Island.Mineshaft)) System.currentTimeMillis() else 0L
    }

    private fun corpseSettings(type: CorpseType): CorpseSettings {
        val name = type.displayName.lowercase()
        val dropdown = +DropdownSetting("${type.displayName} Corpse")
        return CorpseSettings(
            highlight = +BooleanSetting("Highlight", false, desc = "Highlights $name corpses.").withDependency(dropdown),
            showType = +BooleanSetting("Show Type", false, desc = "Shows the type above each $name corpse.").withDependency(dropdown),
            showDistance = +BooleanSetting("Show Distance", false, desc = "Shows how far away each $name corpse is.").withDependency(dropdown),
            color = +ColorSetting("Highlight Color", type.defaultColor, desc = "Color used to highlight $name corpses.").withDependency(dropdown),
        )
    }

    private fun nameTagSegments(type: CorpseType, distance: Int): NameTagSegments {
        val settings = type.settings
        return labelSegments(
            title = "${type.displayName} Corpse".takeIf { settings.showType.value },
            titleColor = settings.color.value,
            distance = distance.takeIf { settings.showDistance.value },
        )
    }

    private fun corpsePreview(type: CorpseType) = EntityPreview(previewCorpse, type.displayName, PreviewLabel(nameTagPosition) { nameTagSegments(type, PREVIEW_DISTANCE) }) {
        skin = DefaultPlayerSkin::getDefaultSkin
        sitting = true
        equipment[EquipmentSlot.HEAD] = type.icon
        equipment[EquipmentSlot.CHEST] = leather(Items.LEATHER_CHESTPLATE, type.armorColor)
        equipment[EquipmentSlot.LEGS] = leather(Items.LEATHER_LEGGINGS, type.armorColor)
        equipment[EquipmentSlot.FEET] = leather(Items.LEATHER_BOOTS, type.armorColor)
    }

    private fun leather(item: Item, color: Int): ItemStack = ItemStack(item).apply { set(DataComponents.DYED_COLOR, DyedItemColor(color)) }

    enum class CorpseType(val displayName: String, val helmetName: String, val defaultColor: Int, val armorColor: Int, createIcon: CorpseType.() -> ItemStack) {
        LAPIS("Lapis", "Lapis Armor Helmet", 0xFF0000FF.toInt(), 0x1A2A6C, { ItemStack(Items.SEA_LANTERN) }),
        UMBER("Umber", "Yog Helmet", 0xFFB56222.toInt(), 0xB56222, { leather(Items.LEATHER_HELMET, armorColor) }),
        TUNGSTEN("Tungsten", "Mineral Helmet", 0xFFFFFFFF.toInt(), 0x9E9E9E, { createSkullStack("a95280a7b21b06e3887f165c8ca7a03bfac96378acd96cdf5944bcd08a0b6587") }),
        VANGUARD("Vanguard", "Vanguard Helmet", 0xFFF224B8.toInt(), 0x2986cc, { createSkullStack("9bb8687e73c84e310d1bc0231b842dad1f93001b6d9ba3329e3c8a685b535623") });

        val icon: ItemStack by lazy { createIcon() }

        companion object {
            fun fromHelmet(name: String?): CorpseType? = entries.find { it.helmetName == name }
        }
    }

    private class CorpseSettings(
        val highlight: BooleanSetting,
        val showType: BooleanSetting,
        val showDistance: BooleanSetting,
        val color: ColorSetting,
    )

    private const val PREVIEW_DISTANCE = 12
    private const val BREAKDOWN_ROW_GAP = 2f
    private const val BREAKDOWN_ICON_GAP = 4f

    private const val MILLIS_PER_HOUR = 3_600_000.0

    private val BREAKDOWN_ORDER = listOf(CorpseType.LAPIS, CorpseType.TUNGSTEN, CorpseType.UMBER, CorpseType.VANGUARD)

    private val EXAMPLE_RATES = mapOf(
        CorpseType.LAPIS to 100,
        CorpseType.TUNGSTEN to 50,
        CorpseType.UMBER to 50,
        CorpseType.VANGUARD to 2,
    )
}
