package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import org.joml.Quaternionf
import org.joml.Vector3f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.isAreaHovered
import kotlin.math.PI
import kotlin.math.pow

class ProfilePage {

    private var yaw = 0f
    private var pitch = 0f
    private var zoom = 1f
    private var dragging = false
    private var lastFrame = Util.getMillis()
    private var lastInteraction = 0L

    private var cardX = 0f
    private var cardY = 0f
    private var cardWidth = 0f
    private var cardHeight = 0f

    private val hintWidth by lazy { font.width(HINT, HINT_SIZE) }

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float) {
        cardX = x + PADDING
        cardY = y + PADDING
        cardWidth = width - PADDING * 2f
        cardHeight = height - PADDING * 2f - HINT_AREA

        idleSpin()

        graphics.roundedRectangle(cardX, cardY, cardWidth, cardHeight, theme.card, CORNERS)
        graphics.hollowRectangle(cardX, cardY, cardWidth, cardHeight, 1f, theme.divider, CORNERS)

        mc.player?.let { drawEntity(graphics, it) }

        font.extract(
            graphics, HINT,
            x + (width - hintWidth) / 2f,
            cardY + cardHeight + (HINT_AREA - HINT_SIZE) / 2f + PADDING / 2f,
            theme.textMuted,
            shadow = false,
            size = HINT_SIZE
        )
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

        val scale = cardHeight * BASE_SCALE * zoom
        val camera = Quaternionf().rotateX(pitch * DEG_TO_RAD)
        val rotation = Quaternionf().rotateZ(PI.toFloat()).mul(camera)
        val center = rotation.transform(Vector3f(0f, state.boundingBoxHeight / 2f, 0f))
        val translation = center.negate()

        graphics.entity(
            state, scale, translation, rotation, camera,
            (cardX + 1f).toInt(), (cardY + 1f).toInt(),
            (cardX + cardWidth - 1f).toInt(), (cardY + cardHeight - 1f).toInt()
        )
    }

    private fun idleSpin() {
        val now = Util.getMillis()
        val deltaSeconds = ((now - lastFrame) / 1000f).coerceAtMost(MAX_FRAME_SECONDS)
        lastFrame = now
        if (!dragging && now - lastInteraction > IDLE_DELAY_MS) yaw += SPIN_SPEED * deltaSeconds
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
        if (!isAreaHovered(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight)) return false
        if (doubleClick) reset()
        dragging = true
        interacted()
        return true
    }

    fun mouseDragged(button: Int, deltaX: Float, deltaY: Float): Boolean {
        if (!dragging || button != InputConstants.MOUSE_BUTTON_LEFT) return false
        yaw -= deltaX * ROTATE_SPEED
        pitch = (pitch - deltaY * ROTATE_SPEED).coerceIn(-MAX_PITCH, MAX_PITCH)
        interacted()
        return true
    }

    fun mouseReleased(button: Int) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !dragging) return
        dragging = false
        interacted()
    }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (!isAreaHovered(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight)) return false
        zoom = (zoom * ZOOM_STEP.pow(amount)).coerceIn(MIN_ZOOM, MAX_ZOOM)
        interacted()
        return true
    }

    private fun interacted() {
        lastInteraction = Util.getMillis()
    }

    private fun reset() {
        yaw = 0f
        pitch = 0f
        zoom = 1f
    }

    companion object {
        private const val PADDING = 10f
        private const val HINT_AREA = 14f
        private const val HINT_SIZE = 8f
        private const val HINT = "Drag to rotate • Scroll to zoom"

        private const val BASE_SCALE = 0.4f
        private const val ROTATE_SPEED = 1.2f
        private const val SPIN_SPEED = 20f
        private const val IDLE_DELAY_MS = 2000L
        private const val MAX_FRAME_SECONDS = 0.1f
        private const val MAX_PITCH = 80f
        private const val ZOOM_STEP = 1.1f
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 4f
        private const val DEG_TO_RAD = (PI / 180.0).toFloat()

        private val CORNERS = CascadeGeometricRadius(4f)

        private val font get() = CascadeFonts.sans
    }
}
