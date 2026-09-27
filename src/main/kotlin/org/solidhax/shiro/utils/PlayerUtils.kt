package org.solidhax.shiro.utils

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.solidhax.shiro.Shiro.mc
import kotlin.math.roundToInt

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

fun playerDistance(pos: Vec3): Int? = mc.player?.position()?.distanceTo(pos)?.roundToInt()
