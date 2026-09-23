package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import org.joml.Quaternionf
import org.joml.Vector3f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.cosmetics.CosmeticsManager
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.Page.Companion.PADDING
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.animation.AnimationManager
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth
import kotlin.math.PI
import kotlin.math.pow

class ProfilePage private constructor(private val list: SettingList) : Page by list {

    constructor() : this(SettingList(CosmeticsManager.settings))

    private var yaw = 0f
    private var pitch = 0f
    private var zoom = 1f
    private var dragging = false
    private var lastInteraction = 0L

    private var previewX = 0f
    private var previewY = 0f
    private var previewWidth = 0f
    private var previewHeight = 0f

    override fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float) {
        list.draw(graphics, x, y, (width + PADDING) / 2f, height, mouseX, mouseY)

        previewX = x + (width + PADDING) / 2f
        previewY = y + PADDING
        previewWidth = width / 2f - PADDING * 1.5f
        previewHeight = height - PADDING * 2f - HINT_AREA

        graphics.roundedRectangle(previewX, previewY, previewWidth, previewHeight, theme.card, Radius.LARGE)
        if (!dragging && Util.getMillis() - lastInteraction > IDLE_DELAY_MS) yaw += SPIN_SPEED * AnimationManager.deltaSeconds
        mc.player?.let { drawEntity(graphics, it) }
        graphics.text(HINT, previewX + (previewWidth - textWidth(HINT)) / 2f, previewY + previewHeight + (HINT_AREA - TEXT_SIZE) / 2f, theme.textMuted)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        if (list.mouseClicked(mouseX, mouseY, button, doubleClick)) return true
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !isPreviewHovered(mouseX, mouseY)) return false
        if (doubleClick) {
            yaw = 0f
            pitch = 0f
            zoom = 1f
        }
        dragging = true
        lastInteraction = Util.getMillis()
        return true
    }

    override fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float): Boolean {
        if (list.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true
        if (!dragging) return false
        yaw -= deltaX * ROTATE_SPEED
        pitch = (pitch - deltaY * ROTATE_SPEED).coerceIn(-MAX_PITCH, MAX_PITCH)
        lastInteraction = Util.getMillis()
        return true
    }

    override fun mouseReleased(button: Int) {
        list.mouseReleased(button)
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !dragging) return
        dragging = false
        lastInteraction = Util.getMillis()
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (list.mouseScrolled(mouseX, mouseY, amount)) return true
        if (!isPreviewHovered(mouseX, mouseY)) return false
        zoom = (zoom * ZOOM_STEP.pow(amount)).coerceIn(MIN_ZOOM, MAX_ZOOM)
        lastInteraction = Util.getMillis()
        return true
    }

    private fun drawEntity(graphics: GuiGraphicsExtractor, entity: Entity) {
        val state = mc.entityRenderDispatcher.getRenderer(entity).createRenderState(entity, 1f)
        state.shadowPieces.clear()
        state.outlineColor = 0

        if (state is LivingEntityRenderState) {
            state.bodyRot = 180f + yaw
            state.yRot = 0f
            state.xRot = 0f
            state.boundingBoxWidth /= state.scale
            state.boundingBoxHeight /= state.scale
            state.scale = 1f
        }

        val modelHeight = state.boundingBoxHeight * CosmeticsManager.heightScale
        val scale = previewHeight * BASE_SCALE * zoom
        val camera = Quaternionf().rotateX(pitch * DEG_TO_RAD)
        val rotation = Quaternionf().rotateZ(PI.toFloat()).mul(camera)
        val translation = rotation.transform(Vector3f(0f, modelHeight / 2f, 0f)).negate()

        drawNameTag(graphics, previewY + previewHeight / 2f - modelHeight * scale / 2f - NAMETAG_OFFSET * scale - TEXT_SIZE)

        val panelScale = ClickGUI.panelScale
        val centerX = mc.window.guiScaledWidth / 2f
        val centerY = mc.window.guiScaledHeight / 2f
        fun scaledX(value: Float) = (centerX + (value - centerX) * panelScale).toInt()
        fun scaledY(value: Float) = (centerY + (value - centerY) * panelScale).toInt()

        graphics.entity(
            state, scale * panelScale, translation, rotation, camera,
            scaledX(previewX + 1f), scaledY(previewY + 1f), scaledX(previewX + previewWidth - 1f), scaledY(previewY + previewHeight - 1f)
        )
    }

    private fun drawNameTag(graphics: GuiGraphicsExtractor, textY: Float) {
        val name = CosmeticsManager.displayName
        var charX = previewX + (previewWidth - textWidth(name)) / 2f

        graphics.scissor(previewX, previewY, previewWidth, previewHeight) {
            graphics.roundedRectangle(
                charX - NAMETAG_PADDING, textY - NAMETAG_PADDING / 2f,
                textWidth(name) + NAMETAG_PADDING * 2f, TEXT_SIZE + NAMETAG_PADDING,
                theme.card, Radius.MEDIUM
            )
            name.forEachIndexed { index, char ->
                val color = if (CosmeticsManager.faded) CascadeGeometricColor(CosmeticsManager.nameColorAt(index, name.length)) else theme.text
                graphics.text(char.toString(), charX, textY, color)
                charX += textWidth(char.toString())
            }
        }
    }

    private fun isPreviewHovered(mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, previewX, previewY, previewWidth, previewHeight)

    companion object {
        private const val NAMETAG_OFFSET = 0.22f
        private const val NAMETAG_PADDING = 4f
        private const val HINT_AREA = 14f
        private const val HINT = "Drag to rotate • Scroll to zoom"

        private const val BASE_SCALE = 0.38f
        private const val ROTATE_SPEED = 1.2f
        private const val SPIN_SPEED = 20f
        private const val IDLE_DELAY_MS = 2000L
        private const val MAX_PITCH = 80f
        private const val ZOOM_STEP = 1.1f
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 4f
        private const val DEG_TO_RAD = (PI / 180.0).toFloat()
    }
}
