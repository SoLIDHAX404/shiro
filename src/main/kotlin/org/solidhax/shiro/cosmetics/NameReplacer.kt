package org.solidhax.shiro.cosmetics

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.FormattedCharSink
import org.solidhax.shiro.Shiro.mc
import java.util.Optional

object NameReplacer {

    private val from: String get() = mc.user.name

    private val name: String get() = CosmeticsManager.displayName

    private val active: Boolean get() = CosmeticsManager.faded || name != from

    fun replace(text: String): String = if (active) text.replace(from, name) else text

    fun replace(text: FormattedCharSequence): FormattedCharSequence {
        if (!active) return text
        return FormattedCharSequence { sink ->
            val replacing = ReplacingSink(sink)
            text.accept(replacing) && replacing.flush()
        }
    }

    fun replace(text: FormattedText): FormattedText {
        if (!active) return text
        val result = Component.empty()
        text.visit(FormattedText.StyledContentConsumer<Unit> { style, string ->
            string.split(from).forEachIndexed { index, part ->
                if (index > 0) name.forEachIndexed { i, char -> result.append(Component.literal(char.toString()).setStyle(styleAt(style, i))) }
                if (part.isNotEmpty()) result.append(Component.literal(part).setStyle(style))
            }
            Optional.empty()
        }, Style.EMPTY)
        return result
    }

    private fun styleAt(style: Style, index: Int): Style =
        if (CosmeticsManager.faded) style.withColor(CosmeticsManager.nameColorAt(index, name.length) and 0xFFFFFF) else style

    private class ReplacingSink(private val sink: FormattedCharSink) : FormattedCharSink {

        private val pending = ArrayList<Style>()
        private var position = 0

        override fun accept(index: Int, style: Style, codepoint: Int): Boolean {
            if (codepoint == from[pending.size].code) {
                pending += style
                if (pending.size < from.length) return true
                val first = pending.first()
                pending.clear()
                return name.withIndex().all { (i, char) -> emit(styleAt(first, i), char.code) }
            }
            if (pending.isEmpty()) return emit(style, codepoint)
            return flush() && accept(index, style, codepoint)
        }

        fun flush(): Boolean {
            val styles = pending.toList()
            pending.clear()
            return styles.withIndex().all { (i, style) -> emit(style, from[i].code) }
        }

        private fun emit(style: Style, codepoint: Int): Boolean = sink.accept(position++, style, codepoint)
    }
}
