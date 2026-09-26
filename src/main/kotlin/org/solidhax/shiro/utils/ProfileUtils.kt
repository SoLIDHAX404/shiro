package org.solidhax.shiro.utils

import com.google.common.collect.ImmutableMultimap
import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import java.util.Base64
import java.util.UUID

fun texturesProfile(textureHash: String): GameProfile {
    val textures = """{"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/$textureHash"}}}"""
    val property = Property("textures", Base64.getEncoder().encodeToString(textures.toByteArray()))
    return GameProfile(UUID.nameUUIDFromBytes(textureHash.toByteArray()), "_", PropertyMap(ImmutableMultimap.of("textures", property)))
}

val GameProfile.skinTexture: String?
    get() {
        val encoded = properties()["textures"].firstOrNull()?.value() ?: return null
        return runCatching {
            JsonParser.parseString(String(Base64.getDecoder().decode(encoded)))
                .asJsonObject.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").asString.substringAfterLast('/')
        }.getOrNull()
    }
