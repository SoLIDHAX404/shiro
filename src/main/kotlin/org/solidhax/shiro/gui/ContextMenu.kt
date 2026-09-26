package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.Page.Companion.PADDING
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.utils.ui.glassPanel
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.text

class ContextMenu(private val anchorX: Float, private val anchorY: Float, private val title: String, settings: List<Setting<*>>) {

    private val list = SettingList(settings, HEADER_HEIGHT)

    private var x = anchorX
    private var y = anchorY
    private var height = 0f

    fun draw(graphics: GuiGraphicsExtractor, screenWidth: Float, screenHeight: Float, mouseX: Float, mouseY: Float) {
        height = (HEADER_HEIGHT + list.contentHeight + PADDING).coerceAtMost(screenHeight - EDGE_MARGIN * 2f)
        x = anchorX.coerceAtMost(screenWidth - WIDTH - EDGE_MARGIN).coerceAtLeast(EDGE_MARGIN)
        y = anchorY.coerceAtMost(screenHeight - height - EDGE_MARGIN).coerceAtLeast(EDGE_MARGIN)

        graphics.glassPanel(x, y, WIDTH, height) {
            graphics.text(title, x + PADDING, y + (HEADER_HEIGHT - TITLE_SIZE) / 2f, theme.text, TITLE_SIZE)
            list.draw(graphics, x, y, WIDTH, height, mouseX, mouseY)
        }
    }

    fun isHovered(mouseX: Float, mouseY: Float): Boolean = isAreaHovered(mouseX, mouseY, x, y, WIDTH, height)

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        list.mouseClicked(mouseX, mouseY, button, false)
        return true
    }

    fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float): Boolean =
        list.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    fun mouseReleased(button: Int) = list.mouseReleased(button)

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        list.mouseScrolled(mouseX, mouseY, amount)
        return true
    }

    companion object {
        private const val WIDTH = 180f
        private const val HEADER_HEIGHT = 26f
        private const val TITLE_SIZE = 9f
        private const val EDGE_MARGIN = 4f
    }
}
