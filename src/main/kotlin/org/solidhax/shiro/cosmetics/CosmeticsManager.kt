package org.solidhax.shiro.cosmetics

import net.minecraft.core.ClientAsset
import net.minecraft.world.entity.player.PlayerSkin
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.gui.settings.impl.StringSetting
import org.solidhax.shiro.utils.ui.lerpColor
import java.util.Optional

object CosmeticsManager {

    const val DEFAULT_COLOR = 0xFFFFFFFF.toInt()

    val displayNameSetting = DropdownSetting("Display Name")

    val nameSetting = StringSetting("Name", mc.user.name, 16, desc = "Name shown instead of yours.").withDependency(displayNameSetting)

    val startColorSetting = ColorSetting("Start Color", DEFAULT_COLOR, desc = "Color the name fades from.").withDependency(displayNameSetting)

    val endColorSetting = ColorSetting("End Color", DEFAULT_COLOR, desc = "Color the name fades to.").withDependency(displayNameSetting)

    val sizeSetting = DropdownSetting("Player Size")

    val sizeXSetting = NumberSetting("Width", 100, 50, 150, 5, desc = "Scale along the X axis.", unit = "%").withDependency(sizeSetting)

    val sizeYSetting = NumberSetting("Height", 100, 50, 150, 5, desc = "Scale along the Y axis.", unit = "%").withDependency(sizeSetting)

    val sizeZSetting = NumberSetting("Depth", 100, 50, 150, 5, desc = "Scale along the Z axis.", unit = "%").withDependency(sizeSetting)

    val displayNameSettings = listOf(displayNameSetting, nameSetting, startColorSetting, endColorSetting)

    val sizeSettings = listOf(sizeSetting, sizeXSetting, sizeYSetting, sizeZSetting)

    val profileSettings = displayNameSettings + sizeSettings

    var cape: Cape = Cape.NONE

    val startColor: Int get() = startColorSetting.value

    val endColor: Int get() = endColorSetting.value

    val faded: Boolean get() = startColor != DEFAULT_COLOR || endColor != DEFAULT_COLOR

    val displayName: String get() = nameSetting.value.ifBlank { mc.user.name }

    fun nameColorAt(index: Int, length: Int): Int {
        val progress = if (length <= 1) 0f else index.toFloat() / (length - 1)
        return lerpColor(startColor, endColor, progress)
    }

    val sizeX: Float get() = sizeXSetting.value / 100f

    val sizeY: Float get() = sizeYSetting.value / 100f

    val sizeZ: Float get() = sizeZSetting.value / 100f

    val scaled: Boolean get() = sizeX != 1f || sizeY != 1f || sizeZ != 1f

    fun apply(skin: PlayerSkin): PlayerSkin {
        val texture = cape.texture ?: return skin
        val asset = ClientAsset.ResourceTexture(texture, texture)
        return skin.with(PlayerSkin.Patch.create(Optional.empty(), Optional.of(asset), Optional.empty(), Optional.empty()))
    }

    fun isLocalPlayer(entityId: Int): Boolean = mc.player?.id == entityId
}
