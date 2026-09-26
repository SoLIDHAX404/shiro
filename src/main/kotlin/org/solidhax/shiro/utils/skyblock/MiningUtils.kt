package org.solidhax.shiro.utils.skyblock

import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.events.core.onTabWidget
import kotlin.math.abs
import kotlin.math.max

object MiningUtils {

    data class PickaxeAbility(val name: String, val cooldown: Double) {
        val isReady: Boolean get() = cooldown <= 0.0
    }

    var pickaxeAbility: PickaxeAbility? = null
        private set

    private val cooldownRegex = Regex("(?:(?<minutes>\\d+)m)? ?(?:(?<seconds>[\\d.]+)s)?")

    init {
        onTabWidget(TabWidget.PICKAXE_ABILITY) {
            syncPickaxeAbility(parsePickaxeAbility())
        }

        on<TickEvent.End> {
            val current = pickaxeAbility ?: return@on
            if (current.isReady) return@on
            pickaxeAbility = current.copy(cooldown = max(0.0, current.cooldown - COOLDOWN_SECONDS_PER_TICK))
        }
    }

    private fun parsePickaxeAbility(): PickaxeAbility? {
        val (name, status) = TabWidget.PICKAXE_ABILITY.values.entries.firstOrNull() ?: return null
        return PickaxeAbility(name, parseCooldown(status))
    }

    private fun parseCooldown(status: String): Double {
        if (status == "Available") return 0.0
        val match = cooldownRegex.matchEntire(status) ?: return 0.0
        val minutes = match.groups["minutes"]?.value?.toDoubleOrNull() ?: 0.0
        val seconds = match.groups["seconds"]?.value?.toDoubleOrNull() ?: 0.0
        return minutes * 60.0 + seconds
    }

    private fun syncPickaxeAbility(parsed: PickaxeAbility?) {
        if (parsed == null) {
            pickaxeAbility = null
            return
        }

        val current = pickaxeAbility
        pickaxeAbility = when {
            current == null -> parsed
            current.name != parsed.name -> parsed
            parsed.isReady -> parsed
            current.isReady -> parsed
            parsed.cooldown > current.cooldown + COOLDOWN_RESYNC_THRESHOLD -> parsed
            abs(parsed.cooldown - current.cooldown) <= COOLDOWN_RESYNC_THRESHOLD -> parsed
            else -> current
        }
    }

    private const val COOLDOWN_SECONDS_PER_TICK = 0.05
    private const val COOLDOWN_RESYNC_THRESHOLD = 0.25
}
