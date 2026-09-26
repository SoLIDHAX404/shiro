package org.solidhax.shiro.cosmetics

import com.mojang.blaze3d.vertex.PoseStack
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.core.ClientAsset
import net.minecraft.world.entity.player.PlayerSkin
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.PreviewLabel
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import org.solidhax.shiro.gui.settings.impl.StringSetting
import org.solidhax.shiro.utils.ui.NameTagSegments
import org.solidhax.shiro.utils.ui.WHITE
import org.solidhax.shiro.utils.ui.lerpColor
import java.util.Optional

object CosmeticsManager {

    private val displayNameDropdown = DropdownSetting("Display Name")
    private val name = StringSetting("Name", mc.user.name, 16, desc = "Name shown instead of yours.").withDependency(displayNameDropdown)
    private val startColor = ColorSetting("Start Color", WHITE, desc = "Color the name fades from.").withDependency(displayNameDropdown)
    private val endColor = ColorSetting("End Color", WHITE, desc = "Color the name fades to.").withDependency(displayNameDropdown)

    private val sizeDropdown = DropdownSetting("Player Size")
    private val width = sizeSetting("Width", "X")
    private val height = sizeSetting("Height", "Y")
    private val depth = sizeSetting("Depth", "Z")

    private val cape = CapeSetting("Cape")

    private val preview = PreviewSetting("Preview", EntityPreview(label = PreviewLabel(segments = ::nameTagSegments)))

    val settings = listOf(displayNameDropdown, name, startColor, endColor, sizeDropdown, width, height, depth, cape, preview)

    val displayName: String get() = name.value.ifBlank { mc.user.name }

    val faded: Boolean get() = startColor.value != WHITE || endColor.value != WHITE

    fun nameColorAt(index: Int, length: Int): Int =
        lerpColor(startColor.value, endColor.value, if (length <= 1) 0f else index.toFloat() / (length - 1))

    fun apply(skin: PlayerSkin): PlayerSkin {
        val texture = cape.value.texture ?: return skin
        val asset = ClientAsset.ResourceTexture(texture, texture)
        return skin.with(PlayerSkin.Patch.create(Optional.empty(), Optional.of(asset), Optional.empty(), Optional.empty()))
    }

    fun scale(entityId: Int, poseStack: PoseStack) {
        if (mc.player?.id == entityId) poseStack.scale(width.value / 100f, height.value / 100f, depth.value / 100f)
    }

    private fun nameTagSegments(): NameTagSegments {
        val name = displayName
        if (!faded) return listOf(name to theme.text)
        return name.mapIndexed { index, char -> char.toString() to CascadeGeometricColor(nameColorAt(index, name.length)) }
    }

    private fun sizeSetting(name: String, axis: String) =
        NumberSetting(name, 100, 50, 150, 5, desc = "Scale along the $axis axis.", unit = "%").withDependency(sizeDropdown)
}
