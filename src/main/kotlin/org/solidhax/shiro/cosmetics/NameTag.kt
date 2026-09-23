package org.solidhax.shiro.cosmetics

import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth

private const val NAMETAG_OFFSET = 0.22f
private const val NAMETAG_PADDING = 4f

fun drawNameTag(graphics: GuiGraphicsExtractor, preview: EntityPreview) {
    val name = CosmeticsManager.displayName
    val textY = preview.modelTop - NAMETAG_OFFSET * preview.blockSize - TEXT_SIZE
    var charX = preview.x + (preview.width - textWidth(name)) / 2f

    graphics.scissor(preview.x, preview.y, preview.width, preview.height) {
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
