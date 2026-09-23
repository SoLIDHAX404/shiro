package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.cosmetics.CosmeticsManager
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.Page.Companion.PADDING
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth

class ProfilePage private constructor(private val list: SettingList) : Page by list {

    constructor() : this(SettingList(CosmeticsManager.settings))

    private val preview = EntityPreview()

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
        preview.heightScale = CosmeticsManager.heightScale
        preview.draw(graphics, previewX, previewY, previewWidth, previewHeight)
        if (mc.player != null) drawNameTag(graphics, preview.modelTop - NAMETAG_OFFSET * preview.blockSize - TEXT_SIZE)
        graphics.text(HINT, previewX + (previewWidth - textWidth(HINT)) / 2f, previewY + previewHeight + (HINT_AREA - TEXT_SIZE) / 2f, theme.textMuted)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean =
        list.mouseClicked(mouseX, mouseY, button, doubleClick) || (button == InputConstants.MOUSE_BUTTON_LEFT && preview.mouseClicked(mouseX, mouseY, doubleClick))

    override fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float): Boolean =
        list.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) || preview.mouseDragged(deltaX, deltaY)

    override fun mouseReleased(button: Int) {
        list.mouseReleased(button)
        if (button == InputConstants.MOUSE_BUTTON_LEFT) preview.mouseReleased()
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean =
        list.mouseScrolled(mouseX, mouseY, amount) || preview.mouseScrolled(mouseX, mouseY, amount)

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

    companion object {
        private const val NAMETAG_OFFSET = 0.22f
        private const val NAMETAG_PADDING = 4f
        private const val HINT_AREA = 14f
        private const val HINT = "Drag to rotate • Scroll to zoom"
    }
}
