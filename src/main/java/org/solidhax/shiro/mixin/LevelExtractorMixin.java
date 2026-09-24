package org.solidhax.shiro.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.solidhax.shiro.events.EntityGlowEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @WrapOperation(method = "isEntityVisible", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;J)Z"))
    private boolean shiro$keepGlowingEntities(LevelRenderer levelRenderer, BlockPos pos, long frame, Operation<Boolean> original, @Local(argsOnly = true) Entity entity) {
        return original.call(levelRenderer, pos, frame) || EntityGlowEvent.colorOf(entity) != null;
    }
}
