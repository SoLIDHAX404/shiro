package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor

data class Theme(
    val panelTint: CascadeGeometricColor,
    val headerTint: CascadeGeometricColor,
    val border: CascadeGeometricColor,
    val text: CascadeGeometricColor,
    val divider: Int,
    val shadow: Int
) {
    companion object {
        val DEFAULT = Theme(
            panelTint = CascadeGeometricColor.vertical(0xF02A2B2D.toInt(), 0xF5202123.toInt()),
            headerTint = CascadeGeometricColor.vertical(0x14FFFFFF, 0x0AFFFFFF),
            border = CascadeGeometricColor.vertical(0x24FFFFFF, 0x0AFFFFFF),
            text = CascadeGeometricColor(0xFFE6E6E6.toInt()),
            divider = 0x14FFFFFF,
            shadow = 0x26000000
        )
    }
}
