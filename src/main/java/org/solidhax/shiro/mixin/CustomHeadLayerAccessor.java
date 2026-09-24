package org.solidhax.shiro.mixin;

import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(CustomHeadLayer.class)
public interface CustomHeadLayerAccessor {

    @Accessor("transforms")
    CustomHeadLayer.Transforms shiroTransforms();

    @Accessor("skullModels")
    Function<SkullBlock.Type, SkullModelBase> shiroSkullModels();
}
