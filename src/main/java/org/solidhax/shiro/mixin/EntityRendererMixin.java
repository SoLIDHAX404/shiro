package org.solidhax.shiro.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.solidhax.shiro.events.EntityGlowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void shiro$applyGlow(T entity, S state, float partialTicks, CallbackInfo ci) {
        Integer color = EntityGlowEvent.colorOf(entity);
        if (color != null) state.outlineColor = ARGB.opaque(color);
    }
}
