package org.solidhax.shiro.utils

import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import java.util.Base64
import java.util.UUID

fun createSkullStack(textureHash: String): ItemStack {
    val textures = """{"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/$textureHash"}}}"""
    val property = Property("textures", Base64.getEncoder().encodeToString(textures.toByteArray()))
    val profile = GameProfile(UUID.nameUUIDFromBytes(textureHash.toByteArray()), "_", PropertyMap(ImmutableMultimap.of("textures", property)))
    return ItemStack(Items.PLAYER_HEAD).apply { set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile)) }
}
