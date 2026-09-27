package org.solidhax.shiro.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile

fun createSkullStack(textureHash: String): ItemStack =
    ItemStack(Items.PLAYER_HEAD).apply { set(DataComponents.PROFILE, ResolvableProfile.createResolved(texturesProfile(textureHash))) }
