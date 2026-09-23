package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.util.Util
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TextInput(private val getText: () -> String, private val setText: (String) -> Unit) {

    var focused = false
        private set

    private var cursor = 0
    private var anchor = 0
    private var scroll = 0f
    private var blinkStart = 0L
    private val cursorAnimation = Animation(CURSOR_DURATION)
    private var x = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, placeholder: String = "") {
        this.x = x

        val text = getText()
        cursor = cursor.coerceIn(0, text.length)
        anchor = anchor.coerceIn(0, text.length)

        val cursorOffset = widthOf(text, cursor)
        scroll = scroll.coerceIn((cursorOffset - width).coerceAtLeast(0f), cursorOffset)

        val textX = x - scroll
        val textY = y + (height - TEXT_SIZE) / 2f

        graphics.scissor(x, y, width, height) {
            if (hasSelection) {
                val startX = textX + widthOf(text, selectionStart)
                val endX = textX + widthOf(text, selectionEnd)
                graphics.roundedRectangle(startX, textY - 2f, endX - startX, TEXT_SIZE + 4f, theme.textSelection, SELECTION_CORNERS)
            }

            if (text.isEmpty() && !focused) graphics.text(placeholder, textX, textY, theme.textMuted)
            else graphics.text(text, textX, textY, theme.text)

            val animatedCursor = cursorAnimation.animateTo(cursorOffset)
            if (focused && (Util.getMillis() - blinkStart) % (BLINK_MS * 2) < BLINK_MS) {
                graphics.roundedRectangle(textX + animatedCursor - CURSOR_OFFSET, textY, CURSOR_WIDTH, TEXT_SIZE, theme.text, CURSOR_CORNERS)
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
        cursorAnimation.set(widthOf(getText(), cursor))
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
        val localX = mouseX - x + scroll
        return (0..text.length).minBy { abs(widthOf(text, it) - localX) }
    }

    private fun widthOf(text: String, index: Int): Float = textWidth(text.take(index))

    private fun resetBlink() {
        blinkStart = Util.getMillis()
    }

    private val hasSelection get() = cursor != anchor
    private val selectionStart get() = min(cursor, anchor)
    private val selectionEnd get() = max(cursor, anchor)

    companion object {
        private const val BLINK_MS = 500L
        private const val CURSOR_WIDTH = 1f
        private const val CURSOR_OFFSET = 1f
        private const val CURSOR_DURATION = 80L

        private val SELECTION_CORNERS = CascadeGeometricRadius(1f)
        private val CURSOR_CORNERS = CascadeGeometricRadius(CURSOR_WIDTH / 2f)
    }
}
