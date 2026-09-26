package org.solidhax.shiro.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HeadedModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.`object`.skull.SkullModelBase
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Pose
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.mixin.CustomHeadLayerAccessor
import org.solidhax.shiro.mixin.LivingEntityRendererAccessor
import org.solidhax.shiro.mixin.ModelPartAccessor

/**
 * Bounding boxes that follow an entity's posed model instead of its hitbox, so sitting, crouching or posed entities get a
 * box that fits what is drawn. Worn items and armor layers are not included.
 */
object ModelBounds {

    /**
     * The box around the entity as it is drawn this frame, in world space. Falls back to the hitbox for entities without a
     * posable model.
     */
    fun of(entity: Entity, partialTick: Float): AABB {
        val renderer = mc.entityRenderDispatcher.getRenderer(entity)
        val state = renderer.createRenderState(entity, partialTick)
        return of(renderer, state)?.move(state.x, state.y, state.z) ?: entity.boundingBox.inflate(0.0, VERTICAL_PADDING, 0.0)
    }

    /**
     * The box around [state]'s model as [renderer] poses it, relative to the entity's position, or null when [renderer]
     * doesn't draw a posable model.
     */
    fun of(renderer: EntityRenderer<*, *>, state: EntityRenderState): AABB? {
        if (renderer !is LivingEntityRenderer<*, *, *> || state !is LivingEntityRenderState) return null
        val poseStack = PoseStack()

        // mirrors LivingEntityRenderer.submit up to where the model is drawn
        val bed = state.bedOrientation
        if (state.hasPose(Pose.SLEEPING) && bed != null) {
            val offset = state.eyeHeight - 0.1f
            poseStack.translate(-bed.stepX * offset, 0f, -bed.stepZ * offset)
        }
        poseStack.scale(state.scale, state.scale, state.scale)
        val accessor = renderer as LivingEntityRendererAccessor
        accessor.shiroSetupRotations(state, poseStack, state.bodyRot, state.scale)
        poseStack.scale(-1f, -1f, 1f)
        accessor.shiroScale(state, poseStack)
        poseStack.translate(0f, -1.501f, 0f)

        @Suppress("UNCHECKED_CAST")
        val model = renderer.model as EntityModel<LivingEntityRenderState>
        model.setupAnim(state)

        val min = Vector3f(Float.POSITIVE_INFINITY)
        val max = Vector3f(Float.NEGATIVE_INFINITY)
        val corner: (Vector3f) -> Unit = { point ->
            min.min(point)
            max.max(point)
        }
        visit(model.root(), poseStack, corner)
        val headLayer = accessor.shiroLayers().firstNotNullOfOrNull { it as? CustomHeadLayer<*, *> }
        if (headLayer != null && model is HeadedModel) visitHead(headLayer, model, state, poseStack, corner)
        if (min.x > max.x) return null
        return AABB(min.x.toDouble(), min.y.toDouble(), min.z.toDouble(), max.x.toDouble(), max.y.toDouble(), max.z.toDouble()).inflate(0.0, VERTICAL_PADDING, 0.0)
    }

    // walks the parts the way ModelPart.render does, skipping hidden parts, and reports every cube corner
    private fun visit(part: ModelPart, poseStack: PoseStack, corner: (Vector3f) -> Unit) {
        if (!part.visible) return
        // ModelPart is final, but the accessor mixin adds this interface at runtime
        @Suppress("CAST_NEVER_SUCCEEDS")
        val accessor = part as ModelPartAccessor
        poseStack.pushPose()
        part.translateAndRotate(poseStack)

        if (!part.skipDraw) {
            val pose = poseStack.last().pose()
            for (cube in accessor.shiroCubes()) {
                for (x in floatArrayOf(cube.minX, cube.maxX)) for (y in floatArrayOf(cube.minY, cube.maxY)) for (z in floatArrayOf(cube.minZ, cube.maxZ)) {
                    corner(pose.transformPosition(x / PIXELS_PER_BLOCK, y / PIXELS_PER_BLOCK, z / PIXELS_PER_BLOCK, Vector3f()))
                }
            }
        }
        for (child in accessor.shiroChildren().values) visit(child, poseStack, corner)
        poseStack.popPose()
    }

    private fun visitHead(layer: CustomHeadLayer<*, *>, model: EntityModel<*>, state: LivingEntityRenderState, poseStack: PoseStack, corner: (Vector3f) -> Unit) {
        val wornHead = state.wornHeadType
        if (state.headItem.isEmpty && wornHead == null) return
        val accessor = layer as CustomHeadLayerAccessor
        val transforms = accessor.shiroTransforms()

        poseStack.pushPose()
        poseStack.scale(transforms.horizontalScale, transforms.verticalScale, transforms.horizontalScale)
        model.root().translateAndRotate(poseStack)
        (model as HeadedModel).translateToHead(poseStack)

        if (wornHead != null) {
            poseStack.translate(0f, transforms.skullYOffset, 0f)
            poseStack.scale(CustomHeadLayer.SKULL_SCALE, CustomHeadLayer.SKULL_SCALE, CustomHeadLayer.SKULL_SCALE)
            val skull = accessor.shiroSkullModels().apply(wornHead)
            skull.setupAnim(SkullModelBase.State().apply { animationPos = state.wornHeadAnimationPos })
            visit(skull.root(), poseStack, corner)
        } else {
            CustomHeadLayer.translateToHead(poseStack, transforms)
            val pose = poseStack.last().pose()
            state.headItem.visitExtents { point -> corner(pose.transformPosition(point, Vector3f())) }
        }
        poseStack.popPose()
    }

    const val VERTICAL_PADDING = 0.1

    private const val PIXELS_PER_BLOCK = 16f
}

val AABB.corners: List<Vec3>
    get() = buildList(8) {
        for (x in doubleArrayOf(minX, maxX)) for (y in doubleArrayOf(minY, maxY)) for (z in doubleArrayOf(minZ, maxZ)) add(Vec3(x, y, z))
    }
