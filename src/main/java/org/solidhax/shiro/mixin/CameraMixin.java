package org.solidhax.shiro.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import org.solidhax.shiro.features.impl.misc.AspectRatio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @ModifyVariable(method = "setupPerspective", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float shiro$applyAspectRatio(float width, @Local(argsOnly = true, ordinal = 4) float height) {
        Float ratio = AspectRatio.currentRatio();
        return ratio != null ? height * ratio : width;
    }

    @ModifyArg(method = "createProjectionMatrixForCulling", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"), index = 1)
    private float shiro$applyCullingAspectRatio(float aspect) {
        Float ratio = AspectRatio.currentRatio();
        return ratio != null ? ratio : aspect;
    }
}
