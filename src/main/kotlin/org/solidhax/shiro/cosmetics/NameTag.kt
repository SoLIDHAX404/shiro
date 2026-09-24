package org.solidhax.shiro.cosmetics

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.NameTagSegments

fun nameTagSegments(): NameTagSegments {
    val name = CosmeticsManager.displayName
    if (!CosmeticsManager.faded) return listOf(name to theme.text)
    return name.mapIndexed { index, char -> char.toString() to CascadeGeometricColor(CosmeticsManager.nameColorAt(index, name.length)) }
}
