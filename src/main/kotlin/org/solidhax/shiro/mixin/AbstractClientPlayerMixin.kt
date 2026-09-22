package org.solidhax.shiro.mixin

import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.player.PlayerSkin
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.cosmetics.CosmeticsManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(AbstractClientPlayer::class)
abstract class AbstractClientPlayerMixin {

    @Inject(method = ["getSkin"], at = [At("RETURN")], cancellable = true)
    private fun shiro_applyCosmetics(info: CallbackInfoReturnable<PlayerSkin>) {
        if (this !== mc.player) return
        info.returnValue = CosmeticsManager.apply(info.returnValue)
    }
}
