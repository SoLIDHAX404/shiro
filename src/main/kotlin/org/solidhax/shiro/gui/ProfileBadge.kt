package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.states.impl.image.data.CascadeImageFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.Identifier
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.truncate
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.text

object ProfileBadge {

    const val HEIGHT = 28f

    private const val INSET = 6f
    private const val FACE_SIZE = 16f
    private const val FACE_V0 = 8f / 64f
    private const val FACE_V1 = 16f / 64f
    private val LAYER_U = floatArrayOf(8f / 64f, 40f / 64f)

    private var cachedName = ""
    private var cachedMaxWidth = 0f
    private var cachedDisplayName = ""

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float) {
        val faceX = x + INSET
        val faceY = y + (HEIGHT - FACE_SIZE) / 2f
        val skin = skinTexture()
        for (u in LAYER_U) {
            graphics.image(skin, faceX, faceY, FACE_SIZE, FACE_SIZE, u, FACE_V0, u + 8f / 64f, FACE_V1, CascadeGeometricColor.WHITE, Radius.MEDIUM, CascadeImageFilter.NEAREST)
        }

        val textX = faceX + FACE_SIZE + INSET
        graphics.text(displayName(x + width - INSET - textX), textX, y + (HEIGHT - TEXT_SIZE) / 2f, theme.text)
    }

    private fun skinTexture(): Identifier =
        (mc.player?.skin ?: DefaultPlayerSkin.get(mc.user.profileId)).body().texturePath()

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
