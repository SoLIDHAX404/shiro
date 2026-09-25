package org.solidhax.shiro.features.impl.mining

import com.mojang.authlib.GameProfile
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.DyeColor
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
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.LabelPositionSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import org.solidhax.shiro.utils.createSkullStack
import org.solidhax.shiro.utils.render.ItemRenderer
import org.solidhax.shiro.utils.render.ModelBounds
import org.solidhax.shiro.utils.render.itemStack
import org.solidhax.shiro.utils.skyblock.Island
import org.solidhax.shiro.utils.skyblock.LocationUtils
import org.solidhax.shiro.utils.ui.NameTagSegments
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.nameTag
import org.solidhax.shiro.utils.ui.screenBounds
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

    private val previewLabel = PreviewLabel(nameTagPosition) { nameTagSegments(CorpseType.LAPIS, PREVIEW_DISTANCE) }

    private val preview = +PreviewSetting("Preview", EntityPreview(DummyEntity { RemotePlayer(it, GameProfile(UUID(0L, 0L), "Steve")) }, label = previewLabel) {
        skin = DefaultPlayerSkin.getDefaultSkin()
        sitting = true
        equipment[EquipmentSlot.HEAD] = ItemStack(Items.SEA_LANTERN)
        equipment[EquipmentSlot.CHEST] = leather(Items.LEATHER_CHESTPLATE)
        equipment[EquipmentSlot.LEGS] = leather(Items.LEATHER_LEGGINGS)
        equipment[EquipmentSlot.FEET] = leather(Items.LEATHER_BOOTS)
    })

    private val breakdownHud by HUD("Corpse Breakdown", "Shows a breakdown of the corpses in the mineshaft.") { example ->
        var width = 0f
        EXAMPLE_BREAKDOWN.entries.forEachIndexed { index, (type, perHour) ->
            val rowY = index * (ItemRenderer.ITEM_SIZE + BREAKDOWN_ROW_GAP)
            val textY = rowY + (ItemRenderer.ITEM_SIZE - TEXT_SIZE) / 2f
            val label = "${type.displayName}: "
            val rate = "${if (example) perHour else "-"}/h"
            val textX = ItemRenderer.ITEM_SIZE + BREAKDOWN_ICON_GAP

            itemStack(type.icon, 0f, rowY)
            text(label, textX, textY, theme.text)
            text(rate, textX + textWidth(label), textY, theme.textMuted)
            width = maxOf(width, textX + textWidth(label) + textWidth(rate))
        }
        width to EXAMPLE_BREAKDOWN.size * (ItemRenderer.ITEM_SIZE + BREAKDOWN_ROW_GAP) - BREAKDOWN_ROW_GAP
    }

    private val corpses = HashMap<ArmorStand, CorpseType>()

    init {
        on<TickEvent.End> {
            corpses.clear()
            if (!LocationUtils.isCurrentArea(Island.Mineshaft)) return@on

            for (entity in level.entitiesForRendering()) {
                if (entity !is ArmorStand || !entity.isAlive || entity.isInvisible || entity.name.string != "Armor Stand") continue
                corpses[entity] = CorpseType.fromHelmet(entity.getItemBySlot(EquipmentSlot.HEAD).customName?.string) ?: continue
            }
        }

        on<EntityGlowEvent> {
            val settings = corpses[entity]?.settings ?: return@on
            if (settings.highlight.value) color = settings.color.value
        }

        on<HudRenderEvent> {
            val player = mc.player ?: return@on
            for ((entity, type) in corpses) {
                val segments = nameTagSegments(type, player.distanceTo(entity).roundToInt())
                if (segments.isEmpty()) continue
                graphics.nameTag(screenBounds(ModelBounds.of(entity, partialTick)) ?: continue, nameTagPosition.value, segments)
            }
        }

        on<LevelEvent.Load> {
            corpses.clear()
        }
    }

    private val CorpseType.settings: CorpseSettings get() = corpseSettings.getValue(this)

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
        return buildList {
            if (settings.showType.value) add("${type.displayName} Corpse" to CascadeGeometricColor(settings.color.value))
            if (settings.showDistance.value) add((if (isEmpty()) "" else " ") + "${distance}m" to theme.textMuted)
        }
    }

    private fun leather(item: Item, color: Int = ARMOR_COLOR): ItemStack = ItemStack(item).apply { set(DataComponents.DYED_COLOR, DyedItemColor(color)) }

    enum class CorpseType(val displayName: String, val helmetName: String, val defaultColor: Int, createIcon: () -> ItemStack) {
        LAPIS("Lapis", "Lapis Armor Helmet", 0xFF0000FF.toInt(), { ItemStack(Items.SEA_LANTERN) }),
        UMBER("Umber", "Yog Helmet", 0xFFB56222.toInt(), { createSkullStack("b565b5aa83d4aa7f7af22dc1271b2f0b27441f9ac1495f6b4653cf68dfb105ef") }),
        TUNGSTEN("Tungsten", "Mineral Helmet", 0xFFFFFFFF.toInt(), { createSkullStack("d811f3e723bbd46393f8aad8556b1df8ed33f559be827f47fe736f704c35586e") }),
        VANGUARD("Vanguard", "Vanguard Helmet", 0xFFF224B8.toInt(), { createSkullStack("9bb8687e73c84e310d1bc0231b842dad1f93001b6d9ba3329e3c8a685b535623") });

        val icon: ItemStack by lazy(createIcon)

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

    private const val ARMOR_COLOR = 0x1A2A6C
    private const val PREVIEW_DISTANCE = 12
    private const val BREAKDOWN_ROW_GAP = 2f
    private const val BREAKDOWN_ICON_GAP = 4f

    private val EXAMPLE_BREAKDOWN = linkedMapOf(
        CorpseType.LAPIS to 100,
        CorpseType.TUNGSTEN to 50,
        CorpseType.UMBER to 50,
        CorpseType.VANGUARD to 2,
    )
}
