package org.solidhax.shiro.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {

    @Invoker("setupRotations")
    void shiroSetupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale);

    @Invoker("scale")
    void shiroScale(LivingEntityRenderState state, PoseStack poseStack);
}
