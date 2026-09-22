package org.solidhax.shiro.cosmetics

import net.minecraft.core.ClientAsset
import net.minecraft.world.entity.player.PlayerSkin
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.gui.settings.impl.StringSetting
import java.util.Optional

object CosmeticsManager {

    val nameSetting = StringSetting("Display Name", "", 16, desc = "Name shown on your profile.")

    val sizeSetting = NumberSetting("Player Size", 100, 50, 150, 5, desc = "Scale of your player model.", unit = "%")

    var cape: Cape = Cape.NONE

    val displayName: String
        get() = nameSetting.value.ifBlank { mc.user.name }

    val size: Float
        get() = sizeSetting.value / 100f

    fun apply(skin: PlayerSkin): PlayerSkin {
        val texture = cape.texture ?: return skin
        val asset = ClientAsset.ResourceTexture(texture, texture)
        return skin.with(PlayerSkin.Patch.create(Optional.empty(), Optional.of(asset), Optional.empty(), Optional.empty()))
    }

    fun isLocalPlayer(entityId: Int): Boolean = mc.player?.id == entityId
}
