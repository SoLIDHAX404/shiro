package org.solidhax.shiro.features.impl.misc

import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.resource.GraphicsResourceAllocator
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.buffers.GpuBuffer
import net.minecraft.client.renderer.LevelTargetBundle
import net.minecraft.client.renderer.PostChain
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.lwjgl.system.MemoryUtil
import org.solidhax.shiro.Shiro.MOD_ID
import org.solidhax.shiro.Shiro.logger
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.mixin.PostChainAccessor
import org.solidhax.shiro.mixin.PostPassAccessor

object MotionBlur : Module(
    name = "Motion Blur",
    description = "Blurs the world along the way it moves across the screen when you turn or move."
) {
    private val strength by NumberSetting("Strength", 1.0, 0.1, 3.0, 0.1, desc = "How far the blur reaches compared to how far things moved since the last frame.")
    private val samples by NumberSetting("Samples", 16, 4, 64, 1, desc = "Samples taken along the motion. More looks smoother but costs more.")

    private val previousView = Matrix4f()
    private val previousProjection = Matrix4f()
    private var previousCamera: Vec3? = null

    private var uniforms: GpuBuffer? = null
    private var uniformsOwner: PostChain? = null
    private var loadFailed = false

    /**
     * Called once the world has been drawn this frame, before the hand and HUD.
     */
    @JvmStatic
    fun afterLevel(allocator: GraphicsResourceAllocator, camera: CameraRenderState, projection: Matrix4fc) {
        if (!enabled) {
            previousCamera = null
            return
        }
        val lastCamera = previousCamera
        // skip frames right after enabling or a teleport, where there is no meaningful previous camera to blur from
        if (lastCamera != null && lastCamera.distanceToSqr(camera.pos) < MAX_CAMERA_JUMP_SQR) apply(allocator, camera, projection, lastCamera)

        previousView.set(camera.viewRotationMatrix)
        previousProjection.set(projection)
        previousCamera = camera.pos
    }

    private fun apply(allocator: GraphicsResourceAllocator, camera: CameraRenderState, projection: Matrix4fc, lastCamera: Vec3) {
        val chain = postChain() ?: return
        val buffer = uniformBuffer(chain) ?: return

        val data = MemoryUtil.memCalloc(UNIFORM_SIZE)
        try {
            Std140Builder.intoBuffer(data)
                .putMat4f(Matrix4f(projection).invert())
                .putMat4f(Matrix4f(camera.viewRotationMatrix).invert())
                .putMat4f(previousView)
                .putMat4f(previousProjection)
                .putVec3((camera.pos.x - lastCamera.x).toFloat(), (camera.pos.y - lastCamera.y).toFloat(), (camera.pos.z - lastCamera.z).toFloat())
                .putFloat(strength.toFloat())
                .putInt(samples)
            data.position(0)
            data.limit(UNIFORM_SIZE)
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0L, UNIFORM_SIZE.toLong()), data)
        } finally {
            MemoryUtil.memFree(data)
        }

        chain.process(mc.gameRenderer.mainRenderTarget(), allocator)
    }

    private fun postChain(): PostChain? {
        if (loadFailed) return null
        return try {
            mc.shaderManager.getPostChain(POST_EFFECT, LevelTargetBundle.MAIN_TARGETS)
        } catch (e: Exception) {
            loadFailed = true
            logger.error("Failed to load the motion blur shader", e)
            null
        }
    }

    // the pass's own uniform buffer can't be written to, so it is swapped for one that can; a reload builds a new chain
    // that gets its own buffer, and the old chain closes the old one along with itself
    private fun uniformBuffer(chain: PostChain): GpuBuffer? {
        val pass = (chain as PostChainAccessor).shiroPasses().firstOrNull() ?: return null
        val passUniforms = (pass as PostPassAccessor).shiroCustomUniforms()

        if (chain !== uniformsOwner || uniforms == null) {
            uniforms = RenderSystem.getDevice().createBuffer({ "Shiro motion blur" }, GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, UNIFORM_SIZE.toLong())
            uniformsOwner = chain
        }
        val buffer = uniforms ?: return null
        val replaced = passUniforms.put(UNIFORM_BLOCK, buffer)
        if (replaced != null && replaced !== buffer) replaced.close()
        return buffer
    }

    private val POST_EFFECT: Identifier = Identifier.fromNamespaceAndPath(MOD_ID, "motion_blur")
    private const val UNIFORM_BLOCK = "MotionBlurConfig"

    // four mat4s, then a vec3 with the float packed into its last slot, then the int, rounded up to 16 bytes
    private const val UNIFORM_SIZE = 288
    private const val MAX_CAMERA_JUMP_SQR = 16.0 * 16.0
}
