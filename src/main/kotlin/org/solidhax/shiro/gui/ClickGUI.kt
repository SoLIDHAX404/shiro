package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.animation.AnimationManager
import kotlin.math.floor

object ClickGUI : OverlayScreen("Shiro Click GUI") {

    var theme = Theme.DEFAULT

    private val panel = Panel("Shiro")
    private val openAnimation = Animation(OPEN_DURATION)
    private var closing = false

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        AnimationManager.update()

        val progress = openAnimation.value
        if (closing && progress <= CLOSE_THRESHOLD) {
            closing = false
            mc.gui.setScreen(null)
            return
        }

        graphics.pose().pushMatrix()
        graphics.pose().scaleAround(MIN_SCALE + (1f - MIN_SCALE) * progress, width / 2f, height / 2f)
        panel.draw(graphics, panelX, panelY, mouseX.toFloat(), mouseY.toFloat())
        graphics.pose().popMatrix()

        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (panel.mouseClicked(event.x().toFloat(), event.y().toFloat(), event.button(), doubleClick)) return true
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (panel.mouseDragged(event.x().toFloat(), event.y().toFloat(), event.button(), deltaX.toFloat(), deltaY.toFloat())) return true
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        panel.mouseReleased(event.button())
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (panel.mouseScrolled(mouseX.toFloat(), mouseY.toFloat(), verticalAmount.toFloat())) return true
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (panel.charTyped(event)) return true
        return super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (panel.keyPressed(event)) return true
        return super.keyPressed(event)
    }

    override fun init() {
        AnimationManager.update()
        if (!closing) openAnimation.restart()
        super.init()
    }

    override fun onClose() {
        if (closing) return
        AnimationManager.update()
        closing = true
        openAnimation.animateTo(0f)
    }

    override fun removed() {
        panel.unfocus()
        ModuleManager.saveConfigurations()
        super.removed()
    }

    private const val OPEN_DURATION = 320L
    private const val CLOSE_THRESHOLD = 0.001f
    private const val MIN_SCALE = 0.94f

    private val panelX get() = floor((width - Panel.WIDTH) / 2f)
    private val panelY get() = floor((height - Panel.HEIGHT) / 2f)
}
