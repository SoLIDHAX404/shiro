// Adapted from Odin (https://github.com/odtheking/Odin), Copyright (c) 2025, odtheking, BSD 3-Clause License.
package org.solidhax.shiro.utils.skyblock

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.events.LevelEvent
import org.solidhax.shiro.events.LocationChangeEvent
import org.solidhax.shiro.events.core.EventPriority
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.events.core.onReceive
import org.solidhax.shiro.utils.noControlCodes
import org.solidhax.shiro.utils.startsWithOneOf
import kotlin.jvm.optionals.getOrNull

object LocationUtils {

    var isInSkyblock: Boolean = false
        private set

    var currentArea: Island = Island.Unknown
        private set(value) {
            field = value
            if (value !== Island.Unknown) LocationChangeEvent.postAndCatch()
        }

    var lobbyId: String? = null
        private set

    private val lobbyRegex = Regex("\\d\\d/\\d\\d/\\d\\d (\\w{0,6}) *")

    init {
        onReceive<ClientboundPlayerInfoUpdatePacket> {
            if (!isCurrentArea(Island.Unknown) || ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME !in actions()) return@onReceive
            val area = entries().find { it.displayName?.string?.startsWithOneOf("Area: ", "Dungeon: ") == true }?.displayName?.string ?: return@onReceive
            currentArea = Island.entries.firstOrNull { area.contains(it.displayName, true) } ?: Island.Unknown
        }

        onReceive<ClientboundSetObjectivePacket> {
            if (!isInSkyblock) isInSkyblock = objectiveName == "SBScoreboard"
        }

        onReceive<ClientboundSetPlayerTeamPacket>(EventPriority.LOW) {
            if (!isCurrentArea(Island.Unknown)) return@onReceive
            val text = parameters.getOrNull()?.let { it.playerPrefix.string.plus(it.playerSuffix.string).noControlCodes } ?: return@onReceive

            lobbyRegex.find(text)?.groupValues?.get(1)?.let { lobbyId = it }
        }

        on<LevelEvent.Load> {
            currentArea = if (mc.hasSingleplayerServer()) Island.SinglePlayer else Island.Unknown
            isInSkyblock = false
            lobbyId = null
        }
    }

    fun isCurrentArea(vararg areas: Island): Boolean =
        currentArea == Island.SinglePlayer || areas.any { currentArea == it }
}