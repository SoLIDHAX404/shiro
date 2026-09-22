package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.image.image
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.joml.Matrix3x2f
import org.solidhax.shiro.features.Module
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
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.svgTexture
import kotlin.math.PI

class SettingsPage(private val onBack: () -> Unit) {

    private val scrollbar = Scrollbar()
    private val textFields = HashMap<StringSetting, TextField>()

    private var x = 0f
    private var y = 0f
    private var width = 0f
    private var height = 0f
    private var backWidth = 0f
    private var module: Module? = null

    private var dragging: Setting<*>? = null
    private var draggingUpper = false
    private var listening: KeybindSetting? = null

    fun draw(graphics: GuiGraphicsExtractor, module: Module, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float) {
        if (module != this.module) {
            this.module = module
            scrollbar.reset()
        }
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        backWidth = BACK_ICON_SIZE + BACK_GAP + font.width(module.name, TITLE_SIZE)

        drawHeader(graphics, module, mouseX, mouseY)
        updateDragging(mouseX)

        val settings = visibleSettings(module)
        if (settings.isEmpty()) {
            font.extract(graphics, EMPTY, cardX, listY, theme.textMuted, shadow = false, size = TEXT_SIZE)
            return
        }

        graphics.scissor(x, listY, width, listHeight) {
            forEachCard(module) { setting, cardY, roundTop, roundBottom ->
                if (cardY + cardHeight(setting) >= listY && cardY <= listY + listHeight) {
                    drawCard(graphics, setting, cardY, roundTop, roundBottom, mouseX, mouseY)
                }
            }
        }

        scrollbar.draw(graphics, x + width - SCROLLBAR_INSET, listY, listHeight, contentHeight(module), mouseX, mouseY)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        listening?.let { setting ->
            setting.value = InputConstants.Type.MOUSE.getOrCreate(button)
            listening = null
            return true
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
        unfocus()

        if (isBackHovered(mouseX, mouseY)) {
            onBack()
            return true
        }
        if (scrollbar.mouseClicked(mouseX, mouseY)) return true

        val module = module ?: return false
        if (!isAreaHovered(mouseX, mouseY, x, listY, width, listHeight)) return false

        forEachCard(module) { setting, cardY, _, _ ->
            if (!isAreaHovered(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight(setting))) return@forEachCard
            when (setting) {
                is BooleanSetting -> setting.enabled = !setting.enabled
                is SelectorSetting -> setting.value += 1
                is DropdownSetting -> setting.enabled = !setting.enabled
                is ActionSetting -> setting.action()
                is KeybindSetting -> listening = setting
                is StringSetting -> if (isAreaHovered(mouseX, mouseY, fieldX(), fieldY(cardY), fieldWidth(), TextField.HEIGHT)) {
                    textField(setting).click(mouseX)
                } else return@forEachCard
                is NumberSetting<*>, is RangeSetting -> if (isSliderHovered(mouseX, mouseY, cardY)) {
                    dragging = setting
                    draggingUpper = setting is RangeSetting && setting.closerToUpper(Slider.percentage(mouseX, contentX, contentWidth))
                    updateDragging(mouseX)
                } else return@forEachCard
                else -> return@forEachCard
            }
            return true
        }
        return false
    }

    fun mouseDragged(mouseX: Float, deltaY: Float): Boolean {
        if (scrollbar.mouseDragged(deltaY)) return true
        return textFields.values.any { it.drag(mouseX) }
    }

    fun mouseReleased(button: Int) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return
        dragging = null
        scrollbar.mouseReleased()
    }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (!isAreaHovered(mouseX, mouseY, x, listY, width, listHeight)) return false
        return scrollbar.scroll(amount)
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

    private fun drawCard(graphics: GuiGraphicsExtractor, setting: Setting<*>, cardY: Float, roundTop: Boolean, roundBottom: Boolean, mouseX: Float, mouseY: Float) {
        val height = cardHeight(setting)
        val hovered = isAreaHovered(mouseX, mouseY, cardX, cardY, cardWidth, height)
        val clickable = isClickableCard(setting)
        val corners = corners(roundTop, roundBottom)

        graphics.roundedRectangle(cardX, cardY, cardWidth, height, if (clickable && hovered) theme.settingCardHovered else theme.settingCard, corners)

        val textY = if (isSliderSetting(setting)) cardY + INNER_PADDING else cardY + (height - TEXT_SIZE) / 2f
        if (setting !is ActionSetting) {
            font.extract(graphics, setting.name, contentX, textY, theme.text, shadow = false, size = TEXT_SIZE)
        }

        when (setting) {
            is BooleanSetting ->
                Checkbox.draw(graphics, contentRight - Checkbox.SIZE, textY + (TEXT_SIZE - Checkbox.SIZE) / 2f, setting.enabled, hovered)

            is SelectorSetting -> drawValue(graphics, setting.selected, textY, theme.text)

            is DropdownSetting -> {
                val iconX = contentRight - ICON_SIZE
                val iconY = textY + (TEXT_SIZE - ICON_SIZE) / 2f
                graphics.image(
                    svgTexture(ModuleButton.CHEVRON, ICON_SIZE.toInt()),
                    iconX, iconY, ICON_SIZE, ICON_SIZE,
                    color = if (hovered) theme.text else theme.textMuted,
                    pose = if (setting.enabled) rotation(iconX + ICON_SIZE / 2f, iconY + ICON_SIZE / 2f) else null
                )
            }

            is ActionSetting -> {
                val labelWidth = font.width(setting.name, TEXT_SIZE)
                font.extract(graphics, setting.name, cardX + (cardWidth - labelWidth) / 2f, textY, if (hovered) theme.text else theme.textMuted, shadow = false, size = TEXT_SIZE)
            }

            is KeybindSetting -> {
                val label = if (listening == setting) LISTENING else keyName(setting)
                val labelWidth = font.width(label, TEXT_SIZE)
                val boxWidth = (labelWidth + KEY_PADDING * 2f).coerceAtLeast(KEY_MIN_WIDTH)
                val boxX = contentRight - boxWidth
                val boxY = textY + (TEXT_SIZE - KEY_HEIGHT) / 2f

                graphics.roundedRectangle(boxX, boxY, boxWidth, KEY_HEIGHT, if (hovered) theme.controlHovered else theme.control, KEY_CORNERS)
                if (listening == setting) graphics.hollowRectangle(boxX, boxY, boxWidth, KEY_HEIGHT, 1f, theme.accent, KEY_CORNERS)
                font.extract(graphics, label, boxX + (boxWidth - labelWidth) / 2f, boxY + (KEY_HEIGHT - TEXT_SIZE) / 2f, theme.text, shadow = false, size = TEXT_SIZE)
            }

            is NumberSetting<*> -> {
                drawValue(graphics, setting.display, textY, theme.textMuted)
                Slider.draw(graphics, contentX, sliderY(cardY), contentWidth, 0f, setting.percentage, isSliderActive(setting, mouseX, mouseY, cardY))
            }

            is RangeSetting -> {
                drawValue(graphics, setting.display, textY, theme.textMuted)
                Slider.draw(graphics, contentX, sliderY(cardY), contentWidth, setting.lowerPercentage, setting.upperPercentage, isSliderActive(setting, mouseX, mouseY, cardY))
            }

            is StringSetting -> {
                val fieldX = fieldX()
                val fieldY = fieldY(cardY)
                val fieldWidth = fieldWidth()
                textField(setting).draw(graphics, fieldX, fieldY, fieldWidth, isAreaHovered(mouseX, mouseY, fieldX, fieldY, fieldWidth, TextField.HEIGHT))
            }
        }
    }

    private fun drawHeader(graphics: GuiGraphicsExtractor, module: Module, mouseX: Float, mouseY: Float) {
        val color = if (isBackHovered(mouseX, mouseY)) theme.textMuted else theme.text
        val titleY = y + PADDING

        graphics.image(
            svgTexture(ModuleButton.CHEVRON, BACK_ICON_SIZE.toInt()),
            cardX, titleY + (TITLE_SIZE - BACK_ICON_SIZE) / 2f,
            BACK_ICON_SIZE, BACK_ICON_SIZE,
            u0 = 1f, u1 = 0f,
            color = color
        )
        font.extract(graphics, module.name, cardX + BACK_ICON_SIZE + BACK_GAP, titleY, color, shadow = false, size = TITLE_SIZE)
    }

    private fun drawValue(graphics: GuiGraphicsExtractor, value: String, textY: Float, color: CascadeGeometricColor) {
        if (value.isEmpty()) return
        font.extract(graphics, value, contentRight - font.width(value, TEXT_SIZE), textY, color, shadow = false, size = TEXT_SIZE)
    }

    private fun updateDragging(mouseX: Float) {
        val percentage = Slider.percentage(mouseX, contentX, contentWidth)
        when (val setting = dragging) {
            is NumberSetting<*> -> setting.setFromPercentage(percentage)
            is RangeSetting -> if (draggingUpper) setting.setUpperFromPercentage(percentage) else setting.setLowerFromPercentage(percentage)
        }
    }

    private fun textField(setting: StringSetting): TextField =
        textFields.getOrPut(setting) { TextField({ setting.value }, { setting.value = it }) }

    private fun keyName(setting: KeybindSetting): String =
        if (setting.value == InputConstants.UNKNOWN) UNBOUND else setting.value.displayName.string

    private fun rotation(centerX: Float, centerY: Float): Matrix3x2f =
        Matrix3x2f().translate(centerX, centerY).rotate(QUARTER_TURN).translate(-centerX, -centerY)

    private inline fun forEachCard(module: Module, block: (Setting<*>, Float, Boolean, Boolean) -> Unit) {
        val settings = visibleSettings(module)
        var cardY = listY - scrollbar.offset
        for (index in settings.indices) {
            val setting = settings[index]
            val groupedAbove = index > 0 && grouped(settings[index - 1], setting)
            val groupedBelow = index < settings.lastIndex && grouped(setting, settings[index + 1])
            block(setting, cardY, !groupedAbove, !groupedBelow)
            cardY += cardHeight(setting) + if (groupedBelow) 0f else SPACING
        }
    }

    private fun grouped(previous: Setting<*>, setting: Setting<*>): Boolean {
        val parent = setting.parent ?: return false
        return parent == previous || parent == previous.parent
    }

    private fun corners(roundTop: Boolean, roundBottom: Boolean): CascadeGeometricRadius = when {
        roundTop && roundBottom -> CORNERS
        roundTop -> TOP_CORNERS
        roundBottom -> BOTTOM_CORNERS
        else -> CascadeGeometricRadius.ZERO
    }

    private fun contentHeight(module: Module): Float {
        val settings = visibleSettings(module)
        var total = PADDING
        for (index in settings.indices) {
            val groupedBelow = index < settings.lastIndex && grouped(settings[index], settings[index + 1])
            total += cardHeight(settings[index]) + if (groupedBelow) 0f else SPACING
        }
        return (total - SPACING).coerceAtLeast(0f)
    }

    private fun isSliderSetting(setting: Setting<*>): Boolean = setting is NumberSetting<*> || setting is RangeSetting

    private fun cardHeight(setting: Setting<*>): Float = when (setting) {
        is NumberSetting<*>, is RangeSetting -> SLIDER_CARD_HEIGHT
        is StringSetting -> TEXT_CARD_HEIGHT
        else -> CARD_HEIGHT
    }

    private fun isClickableCard(setting: Setting<*>): Boolean =
        setting is BooleanSetting || setting is SelectorSetting || setting is DropdownSetting ||
                setting is ActionSetting || setting is KeybindSetting

    private fun visibleSettings(module: Module): List<Setting<*>> = module.settings.values.filter { it.isVisible }

    private fun sliderY(cardY: Float): Float = cardY + INNER_PADDING + TEXT_SIZE + SLIDER_GAP

    private fun fieldY(cardY: Float): Float = cardY + (TEXT_CARD_HEIGHT - TextField.HEIGHT) / 2f

    private fun fieldX(): Float = contentRight - fieldWidth()

    private fun fieldWidth(): Float = contentWidth * FIELD_WIDTH_RATIO

    private fun isSliderHovered(mouseX: Float, mouseY: Float, cardY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, contentX, sliderY(cardY) - SLIDER_GRAB, contentWidth, Slider.HEIGHT + SLIDER_GRAB * 2f)

    private fun isSliderActive(setting: Setting<*>, mouseX: Float, mouseY: Float, cardY: Float): Boolean =
        dragging == setting || (dragging == null && isSliderHovered(mouseX, mouseY, cardY))

    private fun isBackHovered(mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, cardX, y + PADDING - BACK_GRAB, backWidth, TITLE_SIZE + BACK_GRAB * 2f)

    private val listY get() = y + PADDING + TITLE_SIZE + HEADER_GAP
    private val listHeight get() = height - (listY - y)
    private val cardX get() = x + PADDING
    private val cardWidth get() = width - PADDING * 2f - SCROLLBAR_SPACE
    private val contentX get() = cardX + INNER_PADDING
    private val contentWidth get() = cardWidth - INNER_PADDING * 2f
    private val contentRight get() = contentX + contentWidth

    companion object {
        private const val PADDING = 10f
        private const val SPACING = 6f
        private const val INNER_PADDING = 8f
        private const val TEXT_SIZE = 8f
        private const val TITLE_SIZE = 10f
        private const val HEADER_GAP = 10f
        private const val BACK_ICON_SIZE = 9f
        private const val BACK_GAP = 4f
        private const val BACK_GRAB = 3f
        private const val ICON_SIZE = 8f
        private const val SLIDER_GAP = 6f
        private const val SLIDER_GRAB = 4f
        private const val CARD_HEIGHT = TEXT_SIZE + INNER_PADDING * 2f
        private const val SLIDER_CARD_HEIGHT = INNER_PADDING * 2f + TEXT_SIZE + SLIDER_GAP + Slider.HEIGHT
        private const val TEXT_CARD_HEIGHT = TextField.HEIGHT + INNER_PADDING * 2f
        private const val FIELD_WIDTH_RATIO = 0.62f
        private const val KEY_HEIGHT = 13f
        private const val KEY_PADDING = 6f
        private const val KEY_MIN_WIDTH = 34f
        private const val SCROLLBAR_INSET = 4f
        private const val SCROLLBAR_SPACE = 6f
        private const val QUARTER_TURN = (PI / 2.0).toFloat()
        private const val EMPTY = "This module has no settings."
        private const val LISTENING = "..."
        private const val UNBOUND = "None"

        private const val CORNER = 4f

        private val CORNERS = CascadeGeometricRadius(CORNER)
        private val TOP_CORNERS = CascadeGeometricRadius(CORNER, CORNER, 0f, 0f)
        private val BOTTOM_CORNERS = CascadeGeometricRadius(0f, 0f, CORNER, CORNER)
        private val KEY_CORNERS = CascadeGeometricRadius(3f)

        private val font get() = CascadeFonts.sans
    }
}
