package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.util.Util
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TextField(
    private val getText: () -> String,
    private val setText: (String) -> Unit,
    private val placeholder: String = ""
) {

    var focused = false
        private set

    private var cursor = 0
    private var anchor = 0
    private var scroll = 0f
    private var blinkStart = 0L
    private var x = 0f
    private var width = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, hovered: Boolean) {
        this.x = x
        this.width = width

        val text = getText()
        cursor = cursor.coerceIn(0, text.length)
        anchor = anchor.coerceIn(0, text.length)

        graphics.roundedRectangle(x, y, width, HEIGHT, if (focused || hovered) theme.controlHovered else theme.control, CORNERS)
        graphics.hollowRectangle(x, y, width, HEIGHT, 1f, if (focused) theme.accent else theme.divider, CORNERS)

        val innerWidth = width - PADDING * 2f
        val cursorOffset = widthOf(text, cursor)
        scroll = scroll.coerceIn((cursorOffset - innerWidth).coerceAtLeast(0f), cursorOffset)

        val textX = x + PADDING - scroll
        val textY = y + (HEIGHT - TEXT_SIZE) / 2f

        graphics.scissor(x + PADDING, y, innerWidth, HEIGHT) {
            if (hasSelection) {
                val startX = textX + widthOf(text, selectionStart)
                val endX = textX + widthOf(text, selectionEnd)
                graphics.roundedRectangle(startX, textY - 2f, endX - startX, TEXT_SIZE + 4f, theme.textSelection, SELECTION_CORNERS)
            }

            if (text.isEmpty() && !focused) {
                font.extract(graphics, placeholder, textX, textY, theme.textMuted, shadow = false, size = TEXT_SIZE)
            } else {
                font.extract(graphics, text, textX, textY, theme.text, shadow = false, size = TEXT_SIZE)
            }

            if (focused && (Util.getMillis() - blinkStart) % (BLINK_MS * 2) < BLINK_MS) {
                graphics.roundedRectangle(textX + cursorOffset, textY - 2f, CURSOR_WIDTH, TEXT_SIZE + 4f, theme.text, CURSOR_CORNERS)
            }
        }
    }

    fun click(mouseX: Float) {
        focus()
        cursor = indexAt(mouseX)
        anchor = cursor
        resetBlink()
    }

    fun drag(mouseX: Float): Boolean {
        if (!focused) return false
        cursor = indexAt(mouseX)
        resetBlink()
        return true
    }

    fun unfocus() {
        if (!focused) return
        focused = false
        anchor = cursor
        mc.textInputManager().stopTextInput(this)
    }

    fun charTyped(event: CharacterEvent): Boolean {
        if (!focused) return false
        if (event.isAllowedChatCharacter) insert(event.codepointAsString())
        return true
    }

    fun keyPressed(event: KeyEvent): Boolean {
        if (!focused) return false
        val text = getText()
        val shift = event.hasShiftDown()

        when {
            event.isEscape || event.isConfirmation -> unfocus()
            event.isSelectAll -> {
                anchor = 0
                cursor = text.length
            }
            event.isCopy -> copySelection()
            event.isCut -> {
                copySelection()
                deleteSelection()
            }
            event.isPaste -> insert(mc.keyboardHandler.clipboard.filter { !it.isISOControl() })
            event.isLeft -> moveCursor(if (hasSelection && !shift) selectionStart else cursor - 1, shift)
            event.isRight -> moveCursor(if (hasSelection && !shift) selectionEnd else cursor + 1, shift)
            event.key() == InputConstants.KEY_HOME -> moveCursor(0, shift)
            event.key() == InputConstants.KEY_END -> moveCursor(text.length, shift)
            event.key() == InputConstants.KEY_BACKSPACE -> if (hasSelection) deleteSelection() else if (cursor > 0) {
                replace(cursor - 1, cursor, "")
            }
            event.key() == InputConstants.KEY_DELETE -> if (hasSelection) deleteSelection() else if (cursor < text.length) {
                replace(cursor, cursor + 1, "")
            }
        }
        resetBlink()
        return true
    }

    private fun focus() {
        if (focused) return
        focused = true
        mc.textInputManager().startTextInput(this)
    }

    private fun insert(string: String) {
        if (string.isEmpty()) return
        replace(selectionStart, selectionEnd, string)
    }

    private fun deleteSelection() {
        if (hasSelection) replace(selectionStart, selectionEnd, "")
    }

    private fun copySelection() {
        if (hasSelection) mc.keyboardHandler.clipboard = getText().substring(selectionStart, selectionEnd)
    }

    private fun replace(start: Int, end: Int, string: String) {
        val text = getText()
        val updated = text.substring(0, start) + string + text.substring(end)
        setText(updated)
        if (getText() != updated) return
        cursor = start + string.length
        anchor = cursor
    }

    private fun moveCursor(target: Int, keepSelection: Boolean) {
        cursor = target.coerceIn(0, getText().length)
        if (!keepSelection) anchor = cursor
    }

    private fun indexAt(mouseX: Float): Int {
        val text = getText()
        val localX = mouseX - (x + PADDING) + scroll
        return (0..text.length).minBy { abs(widthOf(text, it) - localX) }
    }

    private fun widthOf(text: String, index: Int): Float = font.width(text.take(index), TEXT_SIZE)

    private fun resetBlink() {
        blinkStart = Util.getMillis()
    }

    private val hasSelection get() = cursor != anchor
    private val selectionStart get() = min(cursor, anchor)
    private val selectionEnd get() = max(cursor, anchor)

    companion object {
        const val HEIGHT = 15f

        private const val PADDING = 6f
        private const val TEXT_SIZE = 8f
        private const val BLINK_MS = 500L
        private const val CURSOR_WIDTH = 1f

        private val CORNERS = CascadeGeometricRadius(3f)
        private val SELECTION_CORNERS = CascadeGeometricRadius(1f)
        private val CURSOR_CORNERS = CascadeGeometricRadius(CURSOR_WIDTH / 2f)

        private val font get() = CascadeFonts.sans
    }
}
