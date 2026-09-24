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
import org.solidhax.shiro.events.LevelEvent
import org.solidhax.shiro.events.RenderEvent
import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.DummyEntity
import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import org.solidhax.shiro.utils.render.drawWireFrameBox
import org.solidhax.shiro.utils.skyblock.Island
import org.solidhax.shiro.utils.skyblock.LocationUtils
import java.util.UUID

object CorpseESP : Module(
    name = "Corpse ESP",
    description = "Highlights corpses in Glacite Mineshafts."
) {
    private const val ARMOR_COLOR = 0x1A2A6C

    enum class CorpseType(val displayName: String, val helmetName: String, val defaultColor: Int) {
        LAPIS("Lapis", "Lapis Armor Helmet", 0xFF5555FF.toInt()),
        UMBER("Umber", "Yog Helmet", 0xFFFFAA00.toInt()),
        TUNGSTEN("Tungsten", "Mineral Helmet", 0xFFAAAAAA.toInt()),
        VANGUARD("Vanguard", "Vanguard Helmet", 0xFF55FFFF.toInt());

        companion object {
            fun fromHelmet(name: String?): CorpseType? = entries.find { it.helmetName == name }
        }
    }

    private class CorpseSettings(
        val highlight: BooleanSetting,
        val showDistance: BooleanSetting,
        val announceToParty: BooleanSetting,
        val color: ColorSetting,
    )

    private val corpseSettings = CorpseType.entries.associateWith { type ->
        val name = type.displayName.lowercase()
        val dropdown = +DropdownSetting("${type.displayName} Corpse")
        CorpseSettings(
            highlight = +BooleanSetting("Highlight", false, desc = "Highlights $name corpses.").withDependency(dropdown),
            showDistance = +BooleanSetting("Show Distance", false, desc = "Shows how far away each $name corpse is.").withDependency(dropdown),
            announceToParty = +BooleanSetting("Announce to Party", false, desc = "Sends found $name corpses to party chat.").withDependency(dropdown),
            color = +ColorSetting("Highlight Color", type.defaultColor, desc = "Color used to highlight $name corpses.").withDependency(dropdown),
        )
    }

    private val CorpseType.settings: CorpseSettings get() = corpseSettings.getValue(this)

    private val corpseBreakdown by HUD("Corpse Breakdown HUD", "An example HUD element.") { 10f to 10f}

    private val preview = +PreviewSetting("Preview", EntityPreview(DummyEntity { RemotePlayer(it, GameProfile(UUID(0L, 0L), "Steve")) }) {
        skin = DefaultPlayerSkin.getDefaultSkin()
        sitting = true
        equipment[EquipmentSlot.HEAD] = ItemStack(Items.SEA_LANTERN)
        equipment[EquipmentSlot.CHEST] = leather(Items.LEATHER_CHESTPLATE)
        equipment[EquipmentSlot.LEGS] = leather(Items.LEATHER_LEGGINGS)
        equipment[EquipmentSlot.FEET] = leather(Items.LEATHER_BOOTS)
    })

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

        on<RenderEvent.Extract> {
            for ((entity, type) in corpses) {
                val settings = type.settings
                if (settings.highlight.value) drawWireFrameBox(entity.boundingBox, settings.color.value)
            }
        }

        on<LevelEvent.Load> {
            corpses.clear()
        }
    }

    private fun leather(item: Item): ItemStack = ItemStack(item).apply { set(DataComponents.DYED_COLOR, DyedItemColor(ARMOR_COLOR)) }
}
