package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor

data class Theme(
    val panelTint: CascadeGeometricColor,
    val headerTint: CascadeGeometricColor,
    val border: CascadeGeometricColor,
    val text: CascadeGeometricColor,
    val textMuted: CascadeGeometricColor,
    val entrySelected: Int,
    val entryHovered: Int,
    val accent: Int,
    val badge: Int,
    val card: Int,
    val divider: Int,
    val shadow: Int
) {
    companion object {
        val DEFAULT = Theme(
            panelTint = CascadeGeometricColor.vertical(0xF02A2B2D.toInt(), 0xF5202123.toInt()),
            headerTint = CascadeGeometricColor.vertical(0x14FFFFFF, 0x0AFFFFFF),
            border = CascadeGeometricColor.vertical(0x24FFFFFF, 0x0AFFFFFF),
            text = CascadeGeometricColor(0xFFE6E6E6.toInt()),
            textMuted = CascadeGeometricColor(0xFF8C8D90.toInt()),
            entrySelected = 0x14FFFFFF,
            entryHovered = 0x0AFFFFFF,
            accent = 0xFF9A86F2.toInt(),
            badge = 0x0DFFFFFF,
            card = 0x33000000,
            divider = 0x14FFFFFF,
            shadow = 0x26000000
        )
    }
}
