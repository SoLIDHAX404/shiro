package org.solidhax.shiro.utils.ui

fun isAreaHovered(mouseX: Float, mouseY: Float, x: Float, y: Float, width: Float, height: Float): Boolean {
    return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
}
