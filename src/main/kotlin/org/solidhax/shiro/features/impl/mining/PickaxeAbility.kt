package org.solidhax.shiro.features.impl.mining

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.utils.skyblock.MiningUtils
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.WHITE
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth
import java.util.Locale
import kotlin.math.floor

object PickaxeAbility : Module(
    name = "Pickaxe Ability",
    description = "Various features surrounding pickaxe abilities."
) {
    private val nameColor by ColorSetting("Name Color", WHITE, desc = "Color of the ability name.")
    private val readyColor by ColorSetting("Ready Color", 0xFF55FF55.toInt(), desc = "Color of the status when the ability is ready.")
    private val cooldownColor by ColorSetting("Cooldown Color", 0xFFFF5555.toInt(), desc = "Color of the remaining cooldown.")

    private val pickaxeAbilityHud by HUD("Pickaxe Ability", "Display for the active pickaxe ability.") { example ->
        val ability = if (example) EXAMPLE_ABILITY else MiningUtils.pickaxeAbility ?: return@HUD 0f to 0f

        val label = "${ability.name}: "
        val status = if (ability.isReady) "READY!" else String.format(Locale.ROOT, "%.2fs", floor(ability.cooldown * 100.0) / 100.0)

        text(label, 0f, 0f, CascadeGeometricColor(nameColor))
        text(status, textWidth(label), 0f, CascadeGeometricColor(if (ability.isReady) readyColor else cooldownColor))

        textWidth(label) + textWidth(status) to TEXT_SIZE
    }

    private val EXAMPLE_ABILITY = MiningUtils.PickaxeAbility("Mining Speed Boost", 34.0)
}
