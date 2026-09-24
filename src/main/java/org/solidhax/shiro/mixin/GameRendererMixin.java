package org.solidhax.shiro.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.solidhax.shiro.features.impl.misc.MotionBlur;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    // runs right after the world is drawn and before the hand and HUD, so only the world gets blurred
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZZ)V"))
    private void shiro$motionBlur(
            LevelRenderer levelRenderer, GraphicsResourceAllocator allocator, boolean renderOutline, CameraRenderState cameraState,
            GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, boolean consistentDepthRequired,
            Operation<Void> original, @Local(ordinal = 0) Matrix4f projectionMatrix
    ) {
        original.call(levelRenderer, allocator, renderOutline, cameraState, terrainFog, fogColor, shouldRenderSky, consistentDepthRequired);
        MotionBlur.afterLevel(allocator, cameraState, projectionMatrix);
    }
}
