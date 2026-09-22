package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import org.joml.Quaternionf
import org.joml.Vector3f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.cosmetics.CosmeticsManager
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.animation.AnimationManager
import org.solidhax.shiro.utils.ui.isAreaHovered
import kotlin.math.PI
import kotlin.math.pow

class ProfilePage {

    private val cards = SettingCards()
    private val capeSelector = CapeSelector()

    private var yaw = 0f
    private var pitch = 0f
    private var zoom = 1f
    private var dragging = false
    private var lastInteraction = 0L

    private var x = 0f
    private var y = 0f
    private var width = 0f
    private var height = 0f

    private val hintWidth by lazy { font.width(HINT, HINT_SIZE) }

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height

        cards.draw(graphics, CosmeticsManager.nameSetting, columnX, nameCardY, columnWidth, mouseX, mouseY)
        drawCapeCard(graphics, mouseX, mouseY)
        cards.draw(graphics, CosmeticsManager.sizeSetting, columnX, sizeCardY, columnWidth, mouseX, mouseY)
        drawPreview(graphics)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) cards.unfocus()

        if (cards.mouseClicked(CosmeticsManager.nameSetting, columnX, nameCardY, columnWidth, mouseX, mouseY, button)) return true
        if (cards.mouseClicked(CosmeticsManager.sizeSetting, columnX, sizeCardY, columnWidth, mouseX, mouseY, button)) return true
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
        if (capeSelector.mouseClicked(mouseX, mouseY)) return true

        if (isAreaHovered(mouseX, mouseY, previewX, previewY, previewWidth, previewHeight)) {
            if (doubleClick) resetView()
            dragging = true
            interacted()
            return true
        }
        return false
    }

    fun mouseDragged(mouseX: Float, button: Int, deltaX: Float, deltaY: Float): Boolean {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
        if (cards.mouseDragged(mouseX)) return true
        if (!dragging) return false

        yaw -= deltaX * ROTATE_SPEED
        pitch = (pitch - deltaY * ROTATE_SPEED).coerceIn(-MAX_PITCH, MAX_PITCH)
        interacted()
        return true
    }

    fun mouseReleased(button: Int) {
        cards.mouseReleased(button)
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !dragging) return
        dragging = false
        interacted()
    }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (!isAreaHovered(mouseX, mouseY, previewX, previewY, previewWidth, previewHeight)) return false
        zoom = (zoom * ZOOM_STEP.pow(amount)).coerceIn(MIN_ZOOM, MAX_ZOOM)
        interacted()
        return true
    }

    fun charTyped(event: CharacterEvent): Boolean = cards.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = cards.keyPressed(event)

    fun unfocus() {
        cards.unfocus()
    }

    private fun drawCapeCard(graphics: GuiGraphicsExtractor, mouseX: Float, mouseY: Float) {
        graphics.roundedRectangle(columnX, capeCardY, columnWidth, CAPE_CARD_HEIGHT, theme.settingCard, CORNERS)
        font.extract(graphics, CAPE_LABEL, controlX, capeCardY + SettingCards.INNER_PADDING, theme.text, shadow = false, size = SettingCards.TEXT_SIZE)
        capeSelector.draw(graphics, controlX, capeCardY + SettingCards.INNER_PADDING + SettingCards.TEXT_SIZE + GAP, controlWidth, mouseX, mouseY)
    }

    private fun drawPreview(graphics: GuiGraphicsExtractor) {
        graphics.roundedRectangle(previewX, previewY, previewWidth, previewHeight, theme.card, CORNERS)

        idleSpin()
        mc.player?.let { drawEntity(graphics, it) }

        font.extract(
            graphics, HINT,
            previewX + (previewWidth - hintWidth) / 2f,
            previewY + previewHeight + (HINT_AREA - HINT_SIZE) / 2f,
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

        val size = CosmeticsManager.size
        val scale = previewHeight * BASE_SCALE * zoom
        val camera = Quaternionf().rotateX(pitch * DEG_TO_RAD)
        val rotation = Quaternionf().rotateZ(PI.toFloat()).mul(camera)
        val translation = rotation.transform(Vector3f(0f, state.boundingBoxHeight * size / 2f, 0f)).negate()

        graphics.entity(
            state, scale, translation, rotation, camera,
            (previewX + 1f).toInt(), (previewY + 1f).toInt(),
            (previewX + previewWidth - 1f).toInt(), (previewY + previewHeight - 1f).toInt()
        )
    }

    private fun idleSpin() {
        if (!dragging && Util.getMillis() - lastInteraction > IDLE_DELAY_MS) {
            yaw += SPIN_SPEED * AnimationManager.deltaSeconds
        }
    }

    private fun interacted() {
        lastInteraction = Util.getMillis()
    }

    private fun resetView() {
        yaw = 0f
        pitch = 0f
        zoom = 1f
    }

    private val columnX get() = x + PADDING
    private val columnWidth get() = width / 2f - PADDING * 1.5f
    private val controlX get() = columnX + SettingCards.INNER_PADDING
    private val controlWidth get() = columnWidth - SettingCards.INNER_PADDING * 2f

    private val nameCardY get() = y + PADDING
    private val capeCardY get() = nameCardY + SettingCards.TEXT_CARD_HEIGHT + SPACING
    private val sizeCardY get() = capeCardY + CAPE_CARD_HEIGHT + SPACING

    private val previewX get() = x + width / 2f + PADDING / 2f
    private val previewY get() = y + PADDING
    private val previewWidth get() = width / 2f - PADDING * 1.5f
    private val previewHeight get() = height - PADDING * 2f - HINT_AREA

    companion object {
        private const val PADDING = 10f
        private const val SPACING = 6f
        private const val GAP = 6f
        private const val CAPE_LABEL = "Cape"
        private const val CAPE_CARD_HEIGHT = SettingCards.INNER_PADDING * 2f + SettingCards.TEXT_SIZE + GAP + CapeSelector.HEIGHT

        private const val HINT_AREA = 14f
        private const val HINT_SIZE = 8f
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

        private val CORNERS = CascadeGeometricRadius(4f)

        private val font get() = CascadeFonts.sans
    }
}
