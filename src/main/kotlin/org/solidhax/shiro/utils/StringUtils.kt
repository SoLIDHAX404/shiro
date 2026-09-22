package org.solidhax.shiro.utils

import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.font.rendering.impl.FontRenderer

private const val ELLIPSIS = "…"

fun String.truncate(maxWidth: Float, size: Float, font: FontRenderer = CascadeFonts.sans): String {
    if (font.width(this, size) <= maxWidth) return this
    var end = length
    while (end > 0 && font.width(take(end) + ELLIPSIS, size) > maxWidth) end--
    return take(end) + ELLIPSIS
}
