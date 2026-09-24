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
    description = "Blurs the world as you turn the camera."
) {
    private val strength by NumberSetting("Strength", 1.0, 0.1, 3.0, 0.1, desc = "How far the blur reaches compared to how far the view turns in one refresh of your monitor.")
    private val maxSamples by NumberSetting("Max Samples", 64, 8, 128, 1, desc = "The most samples taken along fast motion. More looks smoother but costs more.")

    private val previousView = Matrix4f()
    private var previousCamera: Vec3? = null
    private var lastFrameNanos = 0L

    private var uniforms: GpuBuffer? = null
    private var uniformsOwner: PostChain? = null
    private var loadFailed = false

    @JvmStatic
    fun afterLevel(allocator: GraphicsResourceAllocator, camera: CameraRenderState, projection: Matrix4fc) {
        val now = System.nanoTime()
        val frameSeconds = (now - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = now
        if (!enabled) {
            previousCamera = null
            return
        }
        val lastCamera = previousCamera
        if (lastCamera != null && lastCamera.distanceToSqr(camera.pos) < MAX_CAMERA_JUMP_SQR && !isStill(camera, lastCamera)) {
            apply(allocator, camera, projection, lastCamera, strength.toFloat() * frameRateScale(frameSeconds))
        }

        previousView.set(camera.viewRotationMatrix)
        previousCamera = camera.pos
    }

    private fun isStill(camera: CameraRenderState, lastCamera: Vec3): Boolean =
        lastCamera.distanceToSqr(camera.pos) < STILL_DISTANCE_SQR && camera.viewRotationMatrix.equals(previousView, STILL_EPSILON)

    private fun frameRateScale(frameSeconds: Float): Float {
        if (frameSeconds <= 0f || frameSeconds >= 1f) return 1f
        val refreshRate = mc.window.findBestMonitor()?.currentMode()?.refreshRate?.toFloat() ?: return 1f
        if (refreshRate <= 0f) return 1f
        return (1f / frameSeconds / refreshRate).coerceAtLeast(1f)
    }

    private fun apply(allocator: GraphicsResourceAllocator, camera: CameraRenderState, projection: Matrix4fc, lastCamera: Vec3, strength: Float) {
        val chain = postChain() ?: return
        val buffer = uniformBuffer(chain) ?: return
        val target = mc.gameRenderer.mainRenderTarget()

        val data = MemoryUtil.memCalloc(UNIFORM_SIZE)
        try {
            Std140Builder.intoBuffer(data)
                .putMat4f(Matrix4f(projection).invert())
                .putMat4f(Matrix4f(camera.viewRotationMatrix).invert())
                .putMat4f(previousView)
                .putMat4f(projection)
                .putVec3((camera.pos.x - lastCamera.x).toFloat(), (camera.pos.y - lastCamera.y).toFloat(), (camera.pos.z - lastCamera.z).toFloat())
                .putFloat(strength)
                .putVec2(target.width.toFloat(), target.height.toFloat())
                .putInt(maxSamples)
            data.position(0)
            data.limit(UNIFORM_SIZE)
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0L, UNIFORM_SIZE.toLong()), data)
        } finally {
            MemoryUtil.memFree(data)
        }

        chain.process(target, allocator)
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

    private const val UNIFORM_SIZE = 288
    private const val MAX_CAMERA_JUMP_SQR = 16.0 * 16.0
    private const val STILL_DISTANCE_SQR = 1.0e-10
    private const val STILL_EPSILON = 1.0e-6f
}
