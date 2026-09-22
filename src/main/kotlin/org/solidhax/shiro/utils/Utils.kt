package org.solidhax.shiro.utils

import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.Identifier
import org.solidhax.shiro.Shiro.mc

private var cachedName = ""
private var cachedMaxWidth = 0f
private var cachedSize = 0f
private var cachedDisplayName = ""

fun skinTexture(): Identifier {
    val skin = mc.player?.skin ?: DefaultPlayerSkin.get(mc.user.profileId)
    return skin.body().texturePath()
}

fun displayName(maxWidth: Float, size: Float): String {
    val current = mc.user.name
    if (current != cachedName || maxWidth != cachedMaxWidth || size != cachedSize) {
        cachedName = current
        cachedMaxWidth = maxWidth
        cachedSize = size
        cachedDisplayName = current.truncate(maxWidth, size)
    }
    return cachedDisplayName
}
