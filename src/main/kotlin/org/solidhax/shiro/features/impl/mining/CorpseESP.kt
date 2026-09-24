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
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import java.util.UUID

object CorpseESP : Module(
    name = "Corpse ESP",
    description = "Highlights corpses in Glacite Mineshafts."
) {
    private const val ARMOR_COLOR = 0x1A2A6C

    private val showType by BooleanSetting("Show Type", false, desc = "Shows the type of each corpse.")
    private val showDistance by BooleanSetting("Show Distance", false, desc = "Shows how far away each corpse is.")
    private val announceToParty by BooleanSetting("Announce to Party", false, desc = "Sends found corpses to party chat.")
    private val highlightColor by ColorSetting("Highlight Color", 0xFFFFAA00.toInt(), desc = "Color used to highlight corpses.")

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
