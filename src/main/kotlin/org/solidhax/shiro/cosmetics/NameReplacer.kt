package org.solidhax.shiro.cosmetics

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.FormattedCharSink
import org.solidhax.shiro.Shiro.mc
import java.util.Optional

object NameReplacer {

    private val from: String get() = mc.user.name

    private val to: String get() = CosmeticsManager.nameSetting.value


    val active: Boolean
        get() = from.isNotEmpty() && (CosmeticsManager.faded || (to.isNotBlank() && to != from))

    private val replacement: String get() = to.ifBlank { from }

    fun replace(text: String): String = if (active) text.replace(from, replacement) else text

    fun replace(text: FormattedCharSequence): FormattedCharSequence {
        if (!active) return text
        return FormattedCharSequence { sink ->
            val replacing = ReplacingSink(sink, from, replacement)
            text.accept(replacing) && replacing.flush()
        }
    }

    fun replace(text: FormattedText): FormattedText {
        if (!active) return text

        val result = Component.empty()
        text.visit(
            FormattedText.StyledContentConsumer<Unit> { style, string ->
                appendReplaced(result, string, style)
                Optional.empty()
            },
            Style.EMPTY
        )
        return result
    }

    private fun appendReplaced(result: MutableComponent, string: String, style: Style) {
        if (!string.contains(from)) {
            result.append(Component.literal(string).setStyle(style))
            return
        }

        var index = 0
        while (index < string.length) {
            val match = string.indexOf(from, index)
            if (match < 0) {
                result.append(Component.literal(string.substring(index)).setStyle(style))
                return
            }
            if (match > index) result.append(Component.literal(string.substring(index, match)).setStyle(style))
            result.append(fadedName(style))
            index = match + from.length
        }
    }

    private fun fadedName(style: Style): Component {
        val name = replacement
        if (!CosmeticsManager.faded) return Component.literal(name).setStyle(style)

        val result = Component.empty()
        name.forEachIndexed { index, char ->
            result.append(Component.literal(char.toString()).setStyle(style.withColor(colorAt(index, name.length))))
        }
        return result
    }

    private fun colorAt(index: Int, length: Int): Int = CosmeticsManager.nameColorAt(index, length) and 0xFFFFFF

    private class ReplacingSink(
        private val sink: FormattedCharSink,
        private val from: String,
        private val to: String
    ) : FormattedCharSink {

        private val styles = ArrayList<Style>()
        private var matched = 0
        private var position = 0

        override fun accept(index: Int, style: Style, codepoint: Int): Boolean {
            if (codepoint == from[matched].code) {
                styles.add(style)
                matched++
                if (matched < from.length) return true

                val replacementStyle = styles.first()
                reset()
                return emit(to, replacementStyle)
            }

            if (matched > 0) {
                val pending = styles.toList()
                val text = from.take(matched)
                reset()
                for (i in text.indices) if (!sink.accept(position++, pending[i], text[i].code)) return false
            }
            return sink.accept(position++, style, codepoint)
        }

        fun flush(): Boolean {
            if (matched == 0) return true
            val pending = styles.toList()
            val text = from.take(matched)
            reset()
            for (i in text.indices) if (!sink.accept(position++, pending[i], text[i].code)) return false
            return true
        }

        private fun emit(text: String, style: Style): Boolean {
            text.forEachIndexed { index, char ->
                val charStyle = if (CosmeticsManager.faded) style.withColor(colorAt(index, text.length)) else style
                if (!sink.accept(position++, charStyle, char.code)) return false
            }
            return true
        }

        private fun reset() {
            matched = 0
            styles.clear()
        }
    }
}
