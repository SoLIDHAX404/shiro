package org.solidhax.shiro.utils

import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.font.rendering.impl.FontRenderer

private const val ELLIPSIS = "…"

inline val String?.noControlCodes: String
    get() {
        val s = this ?: return ""
        val len = s.length

        if (s.indexOf('§') == -1) return s

        val out = CharArray(len)
        var outPos = 0
        var i = 0

        while (i < len) {
            val c = s[i]
            if (c == '§') i += 2
            else {
                out[outPos++] = c
                i++
            }
        }

        return String(out, 0, outPos)
    }

fun String.startsWithOneOf(vararg options: String, ignoreCase: Boolean = false): Boolean =
    options.any { this.startsWith(it, ignoreCase) }

fun String.truncate(maxWidth: Float, size: Float, font: FontRenderer = CascadeFonts.sans): String {
    if (font.width(this, size) <= maxWidth) return this
    var end = length
    while (end > 0 && font.width(take(end) + ELLIPSIS, size) > maxWidth) end--
    return take(end) + ELLIPSIS
}
