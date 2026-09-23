package org.solidhax.shiro.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.solidhax.shiro.Shiro;
import org.solidhax.shiro.cosmetics.CosmeticsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void shiro$applyCosmetics(CallbackInfoReturnable<PlayerSkin> cir) {
        if ((Object) this == Shiro.getMc().player) cir.setReturnValue(CosmeticsManager.INSTANCE.apply(cir.getReturnValue()));
    }
}
