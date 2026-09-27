package org.solidhax.shiro.utils

import net.minecraft.util.Mth
import org.solidhax.shiro.Shiro.mc

data class PlayerPosition(val x: Double, val y: Double, val z: Double, val yaw: Float)

fun localPlayerPosition(): PlayerPosition? {
    val player = mc.player ?: return null
    val partialTick = mc.deltaTracker.getGameTimeDeltaPartialTick(true)
    val tick = partialTick.toDouble()
    return PlayerPosition(
        Mth.lerp(tick, player.xo, player.x),
        Mth.lerp(tick, player.yo, player.y),
        Mth.lerp(tick, player.zo, player.z),
        Mth.rotLerp(partialTick, player.yRotO, player.yRot),
    )
}
