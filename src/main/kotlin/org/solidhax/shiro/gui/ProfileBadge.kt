package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.truncate
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.playerFace
import org.solidhax.shiro.utils.ui.text

object ProfileBadge {

    const val HEIGHT = 28f

    private const val INSET = 6f
    private const val FACE_SIZE = 16f

    private var cachedName = ""
    private var cachedMaxWidth = 0f
    private var cachedDisplayName = ""

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float) {
        val faceX = x + INSET
        val faceY = y + (HEIGHT - FACE_SIZE) / 2f
        graphics.playerFace(faceX, faceY, FACE_SIZE, Radius.MEDIUM)

        val textX = faceX + FACE_SIZE + INSET
        graphics.text(displayName(x + width - INSET - textX), textX, y + (HEIGHT - TEXT_SIZE) / 2f, theme.text)
    }

    private fun displayName(maxWidth: Float): String {
        val current = mc.user.name
        if (current != cachedName || maxWidth != cachedMaxWidth) {
            cachedName = current
            cachedMaxWidth = maxWidth
            cachedDisplayName = current.truncate(maxWidth, TEXT_SIZE)
        }
        return cachedDisplayName
    }
}
