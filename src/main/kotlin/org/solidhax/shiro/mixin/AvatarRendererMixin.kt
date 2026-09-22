package org.solidhax.shiro.mixin

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.player.AvatarRenderer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import org.solidhax.shiro.cosmetics.CosmeticsManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(AvatarRenderer::class)
abstract class AvatarRendererMixin {

    @Inject(
        method = ["scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"],
        at = [At("HEAD")]
    )
    private fun shiro_applySize(state: AvatarRenderState, poseStack: PoseStack, info: CallbackInfo) {
        if (CosmeticsManager.size == 1f || !CosmeticsManager.isLocalPlayer(state.id)) return
        poseStack.scale(CosmeticsManager.size, CosmeticsManager.size, CosmeticsManager.size)
    }
}
