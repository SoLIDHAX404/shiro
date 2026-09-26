package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import org.solidhax.shiro.utils.ui.lerpColor

data class Theme(
    val panelTint: CascadeGeometricColor,
    val headerTint: CascadeGeometricColor,
    val border: CascadeGeometricColor,
    val text: CascadeGeometricColor,
    val textMuted: CascadeGeometricColor,
    val entrySelected: Int,
    val entryHovered: Int,
    val accent: Int,
    val card: Int,
    val settingCard: Int,
    val settingCardHovered: Int,
    val control: Int,
    val controlHovered: Int,
    val textSelection: Int,
    val scrollbar: Int,
    val scrollbarHovered: Int,
    val sliderTrack: Int,
    val sliderTrackHovered: Int,
    val sliderFill: Int,
    val divider: Int,
    val shadow: Int
) {
    fun textHover(progress: Float): CascadeGeometricColor = lerpColor(textMuted, text, progress)

    fun controlHover(progress: Float): Int = lerpColor(control, controlHovered, progress)

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
            card = 0x33000000,
            settingCard = 0x0AFFFFFF,
            settingCardHovered = 0x14FFFFFF,
            control = 0x1AFFFFFF,
            controlHovered = 0x26FFFFFF,
            textSelection = 0x809A86F2.toInt(),
            scrollbar = 0x26FFFFFF,
            scrollbarHovered = 0x40FFFFFF,
            sliderTrack = 0x14FFFFFF,
            sliderTrackHovered = 0x1FFFFFFF,
            sliderFill = 0xFF9A86F2.toInt(),
            divider = 0x14FFFFFF,
            shadow = 0x26000000
        )
    }
}
