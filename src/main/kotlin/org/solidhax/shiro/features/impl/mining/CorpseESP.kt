package org.solidhax.shiro.features.impl.mining

import com.mojang.authlib.GameProfile
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.DyedItemColor
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.DummyEntity
import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import java.util.UUID

object CorpseESP : Module(
    name = "Corpse ESP",
    description = "Highlights corpses in Glacite Mineshafts."
) {
    private const val ARMOR_COLOR = 0x1A2A6C

    private val lapisDropdown by DropdownSetting("Lapis Corpse")
    private val lapisShowDistance by BooleanSetting("Show Distance", false, desc = "Shows how far away each lapis corpse is.").withDependency(lapisDropdown)
    private val lapisAnnounceToParty by BooleanSetting("Announce to Party", false, desc = "Sends found lapis corpses to party chat.").withDependency(lapisDropdown)
    private val lapisHighlightColor by ColorSetting("Highlight Color", 0xFFFFAA00.toInt(), desc = "Color used to highlight lapis corpses.").withDependency(lapisDropdown)

    private val umberDropdown by DropdownSetting("Umber Corpse")
    private val umberShowDistance by BooleanSetting("Show Distance", false, desc = "Shows how far away each umber corpse is.").withDependency(umberDropdown)
    private val umberAnnounceToParty by BooleanSetting("Announce to Party", false, desc = "Sends found umber corpses to party chat.").withDependency(umberDropdown)
    private val umberHighlightColor by ColorSetting("Highlight Color", 0xFFFFAA00.toInt(), desc = "Color used to highlight umber corpses.").withDependency(umberDropdown)

    private val tungstenDropdown by DropdownSetting("Tungsten Corpse")
    private val tungstenShowDistance by BooleanSetting("Show Distance", false, desc = "Shows how far away each tungsten corpse is.").withDependency(tungstenDropdown)
    private val tungstenAnnounceToParty by BooleanSetting("Announce to Party", false, desc = "Sends found tungsten corpses to party chat.").withDependency(tungstenDropdown)
    private val tungstenHighlightColor by ColorSetting("Highlight Color", 0xFFFFAA00.toInt(), desc = "Color used to highlight tungsten corpses.").withDependency(tungstenDropdown)

    private val vanguardDropdown by DropdownSetting("Vanguard Corpse")
    private val vanguardShowDistance by BooleanSetting("Show Distance", false, desc = "Shows how far away each vanguard corpse is.").withDependency(vanguardDropdown)
    private val vanguardAnnounceToParty by BooleanSetting("Announce to Party", false, desc = "Sends found vanguard corpses to party chat.").withDependency(vanguardDropdown)
    private val vanguardHighlightColor by ColorSetting("Highlight Color", 0xFFFFAA00.toInt(), desc = "Color used to highlight vanguard corpses.").withDependency(vanguardDropdown)

    private val corpseBreakdown by HUD("Corpse Breakdown HUD", "An example HUD element.") { 10f to 10f}

    private val preview = +PreviewSetting("Preview", EntityPreview(DummyEntity { RemotePlayer(it, GameProfile(UUID(0L, 0L), "Steve")) }) {
        skin = DefaultPlayerSkin.getDefaultSkin()
        sitting = true
        equipment[EquipmentSlot.HEAD] = ItemStack(Items.SEA_LANTERN)
        equipment[EquipmentSlot.CHEST] = leather(Items.LEATHER_CHESTPLATE)
        equipment[EquipmentSlot.LEGS] = leather(Items.LEATHER_LEGGINGS)
        equipment[EquipmentSlot.FEET] = leather(Items.LEATHER_BOOTS)
    })

    private fun leather(item: Item): ItemStack = ItemStack(item).apply { set(DataComponents.DYED_COLOR, DyedItemColor(ARMOR_COLOR)) }
}
