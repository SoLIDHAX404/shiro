package org.solidhax.shiro.utils.render

import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import org.solidhax.shiro.Shiro.mc
import kotlin.math.ceil
import kotlin.math.hypot

class ItemRenderer : PictureInPictureRenderer<ItemRenderer.State>() {

    private var renderedModel: Any? = null

    override fun renderToTexture(state: State, poseStack: PoseStack, collector: SubmitNodeCollector) {
        poseStack.scale(1f, -1f, -1f)
        mc.gameRenderer.lighting().setupFor(if (state.item.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT)
        state.item.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)
        renderedModel = state.item.modelIdentity
    }

    override fun textureIsReadyToBlit(state: State): Boolean =
        !state.item.isAnimated && state.item.modelIdentity == renderedModel

    override fun getTranslateY(height: Int, guiScale: Int): Float = height / 2f

    override fun getRenderStateClass(): Class<State> = State::class.java

    override fun getTextureLabel(): String = "shiro_item"

    class State(val item: TrackingItemStackRenderState, private val pose: Matrix3x2f, private val size: Int, private val scissor: ScreenRectangle?) : PictureInPictureRenderState {

        private val bounds = PictureInPictureRenderState.getBounds(0, 0, size, size, pose, scissor)

        override fun x0(): Int = 0
        override fun y0(): Int = 0
        override fun x1(): Int = size
        override fun y1(): Int = size
        override fun scale(): Float = size.toFloat()
        override fun pose(): Matrix3x2fc = pose
        override fun scissorArea(): ScreenRectangle? = scissor
        override fun bounds(): ScreenRectangle? = bounds
    }

    companion object {
        const val ITEM_SIZE = 16f
    }
}

fun GuiGraphicsExtractor.itemStack(stack: ItemStack, x: Float, y: Float, size: Float = ItemRenderer.ITEM_SIZE) {
    if (stack.isEmpty) return

    val item = TrackingItemStackRenderState()
    mc.itemModelResolver.updateForTopItem(item, stack, ItemDisplayContext.GUI, mc.level, mc.player, 0)

    val pose = pose()
    val pixels = ceil(size * hypot(pose.m00(), pose.m01())).toInt().coerceAtLeast(1)
    val itemPose = Matrix3x2f(pose).translate(x, y).scale(size / pixels)
    guiRenderState.addPicturesInPictureState(ItemRenderer.State(item, itemPose, pixels, scissorStack.peek()))
}
