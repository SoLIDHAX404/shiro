package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.joml.Matrix3x2f
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.gui.settings.impl.ActionSetting
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.KeybindSetting
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.gui.settings.impl.RangeSetting
import org.solidhax.shiro.gui.settings.impl.SelectorSetting
import org.solidhax.shiro.gui.settings.impl.StringSetting
import org.solidhax.shiro.utils.ui.animation.Animation
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.ui.svgTexture
import kotlin.math.PI

class SettingCards {

    private val textFields = HashMap<StringSetting, TextField>()
    private val hoverAnimations = HashMap<Setting<*>, Animation>()
    private val stateAnimations = HashMap<Setting<*>, Animation>()

    private var dragging: Setting<*>? = null
    private var draggingUpper = false
    private var dragX = 0f
    private var dragWidth = 0f
    private var listening: KeybindSetting? = null

    fun height(setting: Setting<*>): Float = when (setting) {
        is NumberSetting<*>, is RangeSetting -> SLIDER_CARD_HEIGHT
        is StringSetting -> TEXT_CARD_HEIGHT
        else -> CARD_HEIGHT
    }

    fun draw(
        graphics: GuiGraphicsExtractor,
        setting: Setting<*>,
        x: Float,
        y: Float,
        width: Float,
        mouseX: Float,
        mouseY: Float,
        corners: CascadeGeometricRadius = CORNERS
    ) {
        val height = height(setting)
        val contentX = x + INNER_PADDING
        val contentWidth = width - INNER_PADDING * 2f
        val contentRight = contentX + contentWidth
        val hovered = isAreaHovered(mouseX, mouseY, x, y, width, height)
        val hover = hoverAnimation(setting).animate(isClickable(setting) && hovered)

        graphics.roundedRectangle(x, y, width, height, lerpColor(theme.settingCard, theme.settingCardHovered, hover), corners)

        val textY = if (isSlider(setting)) y + INNER_PADDING else y + (height - TEXT_SIZE) / 2f
        if (setting !is ActionSetting) {
            font.extract(graphics, setting.name, contentX, textY, theme.text, shadow = false, size = TEXT_SIZE)
        }

        when (setting) {
            is BooleanSetting ->
                Checkbox.draw(graphics, contentRight - Checkbox.SIZE, textY + (TEXT_SIZE - Checkbox.SIZE) / 2f, hovered, stateAnimation(setting).animate(setting.enabled))

            is SelectorSetting -> drawValue(graphics, setting.selected, contentRight, textY, theme.text)

            is DropdownSetting -> {
                val iconX = contentRight - ICON_SIZE
                val iconY = textY + (TEXT_SIZE - ICON_SIZE) / 2f
                graphics.image(
                    svgTexture(ModuleButton.CHEVRON, ICON_SIZE.toInt()),
                    iconX, iconY, ICON_SIZE, ICON_SIZE,
                    color = lerpColor(theme.textMuted, theme.text, hover),
                    pose = rotation(iconX + ICON_SIZE / 2f, iconY + ICON_SIZE / 2f, stateAnimation(setting).animate(setting.enabled))
                )
            }

            is ActionSetting -> {
                val labelWidth = font.width(setting.name, TEXT_SIZE)
                font.extract(graphics, setting.name, x + (width - labelWidth) / 2f, textY, lerpColor(theme.textMuted, theme.text, hover), shadow = false, size = TEXT_SIZE)
            }

            is KeybindSetting -> {
                val label = if (listening == setting) LISTENING else keyName(setting)
                val labelWidth = font.width(label, TEXT_SIZE)
                val boxWidth = (labelWidth + KEY_PADDING * 2f).coerceAtLeast(KEY_MIN_WIDTH)
                val boxX = contentRight - boxWidth
                val boxY = textY + (TEXT_SIZE - KEY_HEIGHT) / 2f

                graphics.roundedRectangle(boxX, boxY, boxWidth, KEY_HEIGHT, lerpColor(theme.control, theme.controlHovered, hover), KEY_CORNERS)
                if (listening == setting) graphics.hollowRectangle(boxX, boxY, boxWidth, KEY_HEIGHT, 1f, theme.accent, KEY_CORNERS)
                font.extract(graphics, label, boxX + (boxWidth - labelWidth) / 2f, boxY + (KEY_HEIGHT - TEXT_SIZE) / 2f, theme.text, shadow = false, size = TEXT_SIZE)
            }

            is NumberSetting<*> -> {
                drawValue(graphics, setting.display, contentRight, textY, theme.textMuted)
                Slider.draw(graphics, contentX, sliderY(y), contentWidth, 0f, setting.percentage, sliderActive(setting, mouseX, mouseY, y, contentX, contentWidth))
            }

            is RangeSetting -> {
                drawValue(graphics, setting.display, contentRight, textY, theme.textMuted)
                Slider.draw(graphics, contentX, sliderY(y), contentWidth, setting.lowerPercentage, setting.upperPercentage, sliderActive(setting, mouseX, mouseY, y, contentX, contentWidth))
            }

            is StringSetting -> {
                val fieldWidth = contentWidth * FIELD_WIDTH_RATIO
                val fieldX = contentRight - fieldWidth
                val fieldY = fieldY(y)
                textField(setting).draw(graphics, fieldX, fieldY, fieldWidth, isAreaHovered(mouseX, mouseY, fieldX, fieldY, fieldWidth, TextField.HEIGHT))
            }
        }
    }

    fun mouseClicked(setting: Setting<*>, x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float, button: Int): Boolean {
        listening?.let {
            it.value = InputConstants.Type.MOUSE.getOrCreate(button)
            listening = null
            return true
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
        if (!isAreaHovered(mouseX, mouseY, x, y, width, height(setting))) return false

        val contentX = x + INNER_PADDING
        val contentWidth = width - INNER_PADDING * 2f

        when (setting) {
            is BooleanSetting -> setting.enabled = !setting.enabled
            is SelectorSetting -> setting.value += 1
            is DropdownSetting -> setting.enabled = !setting.enabled
            is ActionSetting -> setting.action()
            is KeybindSetting -> listening = setting
            is StringSetting -> {
                val fieldWidth = contentWidth * FIELD_WIDTH_RATIO
                val fieldX = contentX + contentWidth - fieldWidth
                if (!isAreaHovered(mouseX, mouseY, fieldX, fieldY(y), fieldWidth, TextField.HEIGHT)) return false
                textField(setting).click(mouseX)
            }
            is NumberSetting<*>, is RangeSetting -> {
                if (!isSliderHovered(mouseX, mouseY, y, contentX, contentWidth)) return false
                dragging = setting
                dragX = contentX
                dragWidth = contentWidth
                draggingUpper = setting is RangeSetting && setting.closerToUpper(Slider.percentage(mouseX, contentX, contentWidth))
                updateDragging(mouseX)
            }
            else -> return false
        }
        return true
    }

    fun updateDragging(mouseX: Float) {
        val percentage = Slider.percentage(mouseX, dragX, dragWidth)
        when (val setting = dragging) {
            is NumberSetting<*> -> setting.setFromPercentage(percentage)
            is RangeSetting -> if (draggingUpper) setting.setUpperFromPercentage(percentage) else setting.setLowerFromPercentage(percentage)
        }
    }

    fun mouseDragged(mouseX: Float): Boolean {
        if (dragging != null) {
            updateDragging(mouseX)
            return true
        }
        return textFields.values.any { it.drag(mouseX) }
    }

    fun mouseReleased(button: Int) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) dragging = null
    }

    fun charTyped(event: CharacterEvent): Boolean = textFields.values.any { it.charTyped(event) }

    fun keyPressed(event: KeyEvent): Boolean {
        listening?.let { setting ->
            setting.value = if (event.isEscape) InputConstants.UNKNOWN else InputConstants.getKey(event)
            listening = null
            return true
        }
        return textFields.values.any { it.keyPressed(event) }
    }

    fun unfocus() {
        textFields.values.forEach { it.unfocus() }
        listening = null
    }

    private fun drawValue(graphics: GuiGraphicsExtractor, value: String, right: Float, textY: Float, color: CascadeGeometricColor) {
        if (value.isEmpty()) return
        font.extract(graphics, value, right - font.width(value, TEXT_SIZE), textY, color, shadow = false, size = TEXT_SIZE)
    }

    private fun textField(setting: StringSetting): TextField =
        textFields.getOrPut(setting) { TextField({ setting.value }, { setting.value = it }) }

    private fun keyName(setting: KeybindSetting): String =
        if (setting.value == InputConstants.UNKNOWN) UNBOUND else setting.value.displayName.string

    private fun rotation(centerX: Float, centerY: Float, progress: Float): Matrix3x2f? {
        if (progress <= 0f) return null
        return Matrix3x2f().translate(centerX, centerY).rotate(QUARTER_TURN * progress).translate(-centerX, -centerY)
    }

    private fun hoverAnimation(setting: Setting<*>): Animation = hoverAnimations.getOrPut(setting) { Animation(HOVER_DURATION) }

    private fun stateAnimation(setting: Setting<*>): Animation = stateAnimations.getOrPut(setting) { Animation(STATE_DURATION) }

    private fun sliderY(cardY: Float): Float = cardY + INNER_PADDING + TEXT_SIZE + SLIDER_GAP

    private fun fieldY(cardY: Float): Float = cardY + (TEXT_CARD_HEIGHT - TextField.HEIGHT) / 2f

    private fun isSliderHovered(mouseX: Float, mouseY: Float, cardY: Float, contentX: Float, contentWidth: Float): Boolean =
        isAreaHovered(mouseX, mouseY, contentX, sliderY(cardY) - SLIDER_GRAB, contentWidth, Slider.HEIGHT + SLIDER_GRAB * 2f)

    private fun sliderActive(setting: Setting<*>, mouseX: Float, mouseY: Float, cardY: Float, contentX: Float, contentWidth: Float): Boolean =
        dragging == setting || (dragging == null && isSliderHovered(mouseX, mouseY, cardY, contentX, contentWidth))

    private fun isSlider(setting: Setting<*>): Boolean = setting is NumberSetting<*> || setting is RangeSetting

    private fun isClickable(setting: Setting<*>): Boolean =
        setting is BooleanSetting || setting is SelectorSetting || setting is DropdownSetting ||
                setting is ActionSetting || setting is KeybindSetting

    companion object {
        const val INNER_PADDING = 8f
        const val TEXT_SIZE = 8f

        private const val ICON_SIZE = 8f
        private const val SLIDER_GAP = 6f
        private const val SLIDER_GRAB = 4f
        private const val FIELD_WIDTH_RATIO = 0.62f
        private const val KEY_HEIGHT = 13f
        private const val KEY_PADDING = 6f
        private const val KEY_MIN_WIDTH = 34f
        private const val HOVER_DURATION = 120L
        private const val STATE_DURATION = 180L
        private const val QUARTER_TURN = (PI / 2.0).toFloat()
        private const val LISTENING = "..."
        private const val UNBOUND = "None"

        const val CARD_HEIGHT = TEXT_SIZE + INNER_PADDING * 2f
        const val SLIDER_CARD_HEIGHT = CARD_HEIGHT + SLIDER_GAP + Slider.HEIGHT
        const val TEXT_CARD_HEIGHT = TextField.HEIGHT + INNER_PADDING * 2f

        private const val CORNER = 4f

        private val KEY_CORNERS = CascadeGeometricRadius(3f)
        val CORNERS = CascadeGeometricRadius(CORNER)
        val TOP_CORNERS = CascadeGeometricRadius(CORNER, CORNER, 0f, 0f)
        val BOTTOM_CORNERS = CascadeGeometricRadius(0f, 0f, CORNER, CORNER)

        private val font get() = CascadeFonts.sans
    }
}
