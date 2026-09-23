package org.solidhax.shiro.gui

import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.arc.ring
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.solidhax.shiro.cosmetics.CapeSetting
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.Page.Companion.PADDING
import org.solidhax.shiro.gui.Page.Companion.SPACING
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.gui.settings.impl.ActionSetting
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.DropdownSetting
import org.solidhax.shiro.gui.settings.impl.KeybindSetting
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.gui.settings.impl.RangeSetting
import org.solidhax.shiro.gui.settings.impl.SelectorSetting
import org.solidhax.shiro.gui.settings.impl.StringSetting
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.animation.Animations
import org.solidhax.shiro.utils.ui.icon
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth
import kotlin.math.PI

class SettingList(var settings: Collection<Setting<*>> = emptyList(), topPadding: Float = PADDING) : Page {

    private val scroll = ScrollArea(topPadding)
    private val textFields = HashMap<StringSetting, TextField>()
    private val capeSelectors = HashMap<CapeSetting, CapeSelector>()
    private val hoverAnimations = Animations<Setting<*>>()
    private val stateAnimations = Animations<Setting<*>>(STATE_DURATION)

    private var visible = emptyList<Setting<*>>()
    private var dragging: Setting<*>? = null
    private var drag: ((Float, Float) -> Unit)? = null
    private var listening: KeybindSetting? = null

    private val left get() = scroll.contentX + INNER_PADDING
    private val inner get() = scroll.contentWidth - INNER_PADDING * 2f
    private val right get() = left + inner

    override fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float) {
        visible = settings.filter { it.isVisible }
        var contentHeight = -SPACING
        forEachCard { setting, _, _, gap -> contentHeight += height(setting) + gap }

        val hovered = scroll.isHovered(mouseX, mouseY)
        scroll.draw(graphics, x, y, width, height, contentHeight, mouseX, mouseY) {
            forEachCard { setting, cardY, corners, _ ->
                drawCard(graphics, setting, cardY, corners, if (hovered) mouseX else -1f, if (hovered) mouseY else -1f)
            }
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) unfocus()
        listening?.let {
            it.value = InputConstants.Type.MOUSE.getOrCreate(button)
            listening = null
            return true
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false
        if (scroll.mouseClicked(mouseX, mouseY)) return true
        if (!scroll.isHovered(mouseX, mouseY)) return false

        forEachCard { setting, cardY, _, _ ->
            if (isAreaHovered(mouseX, mouseY, scroll.contentX, cardY, scroll.contentWidth, height(setting))) return click(setting, cardY, mouseX, mouseY)
        }
        return false
    }

    override fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float): Boolean {
        if (scroll.mouseDragged(deltaY)) return true
        drag?.let {
            it(mouseX, mouseY)
            return true
        }
        return textFields.values.any { it.drag(mouseX) }
    }

    override fun mouseReleased(button: Int) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return
        scroll.mouseReleased()
        dragging = null
        drag = null
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean = scroll.mouseScrolled(mouseX, mouseY, amount)

    override fun charTyped(event: CharacterEvent): Boolean = textFields.values.any { it.charTyped(event) }

    override fun keyPressed(event: KeyEvent): Boolean {
        listening?.let {
            it.value = if (event.isEscape) InputConstants.UNKNOWN else InputConstants.getKey(event)
            listening = null
            return true
        }
        return textFields.values.any { it.keyPressed(event) }
    }

    override fun unfocus() {
        textFields.values.forEach { it.unfocus() }
        listening = null
    }

    fun reset() {
        unfocus()
        scroll.reset()
    }

    private inline fun forEachCard(block: (Setting<*>, Float, CascadeGeometricRadius, Float) -> Unit) {
        var cardY = scroll.contentY
        visible.forEachIndexed { index, setting ->
            val groupedAbove = index > 0 && grouped(visible[index - 1], setting)
            val groupedBelow = index < visible.lastIndex && grouped(setting, visible[index + 1])
            val gap = if (groupedBelow) 0f else SPACING
            block(setting, cardY, corners(!groupedAbove, !groupedBelow), gap)
            cardY += height(setting) + gap
        }
    }

    private fun drawCard(graphics: GuiGraphicsExtractor, setting: Setting<*>, y: Float, corners: CascadeGeometricRadius, mouseX: Float, mouseY: Float) {
        val x = scroll.contentX
        val width = scroll.contentWidth
        val hovered = isAreaHovered(mouseX, mouseY, x, y, width, height(setting))
        val hover = hoverAnimations[setting].animate(isClickable(setting) && hovered)
        val textY = y + ((if (setting is StringSetting) TEXT_CARD_HEIGHT else CARD_HEIGHT) - TEXT_SIZE) / 2f

        graphics.roundedRectangle(x, y, width, height(setting), lerpColor(theme.settingCard, theme.settingCardHovered, hover), corners)
        if (setting !is ActionSetting) graphics.text(setting.name, left, textY, theme.text)

        when (setting) {
            is BooleanSetting ->
                Checkbox.draw(graphics, right - Checkbox.SIZE, textY + (TEXT_SIZE - Checkbox.SIZE) / 2f, hovered, stateAnimations[setting].animate(setting.enabled))

            is SelectorSetting -> graphics.drawValue(setting.selected, textY, theme.text)

            is DropdownSetting -> graphics.icon(
                ModuleButton.CHEVRON, right - ICON_SIZE, textY + (TEXT_SIZE - ICON_SIZE) / 2f, ICON_SIZE,
                lerpColor(theme.textMuted, theme.text, hover), QUARTER_TURN * stateAnimations[setting].animate(setting.enabled)
            )

            is ActionSetting -> graphics.text(setting.name, x + (width - textWidth(setting.name)) / 2f, textY, lerpColor(theme.textMuted, theme.text, hover))

            is KeybindSetting -> drawKeybind(graphics, setting, textY, hover)

            is NumberSetting<*> -> drawSlider(graphics, setting, setting.display, 0f, setting.percentage, y, textY, mouseX, mouseY)

            is RangeSetting -> drawSlider(graphics, setting, setting.display, setting.lowerPercentage, setting.upperPercentage, y, textY, mouseX, mouseY)

            is StringSetting -> textField(setting).draw(graphics, right - fieldWidth, y + INNER_PADDING, fieldWidth, mouseX, mouseY)

            is ColorSetting -> {
                val swatchY = textY + (TEXT_SIZE - SWATCH_HEIGHT) / 2f
                graphics.roundedRectangle(right - SWATCH_WIDTH, swatchY, SWATCH_WIDTH, SWATCH_HEIGHT, setting.value, Radius.SMALL)
                graphics.hollowRectangle(right - SWATCH_WIDTH, swatchY, SWATCH_WIDTH, SWATCH_HEIGHT, 1f, theme.divider, Radius.SMALL)
                if (setting.expanded) drawPicker(graphics, setting, y + CARD_HEIGHT + PICKER_GAP)
            }

            is CapeSetting -> capeSelector(setting).draw(graphics, left, controlY(y), inner, mouseX, mouseY)
        }
    }

    private fun click(setting: Setting<*>, y: Float, mouseX: Float, mouseY: Float): Boolean {
        when (setting) {
            is BooleanSetting -> setting.enabled = !setting.enabled
            is SelectorSetting -> setting.value += 1
            is DropdownSetting -> setting.enabled = !setting.enabled
            is ActionSetting -> setting.action()
            is KeybindSetting -> listening = setting
            is StringSetting -> return textField(setting).mouseClicked(mouseX, mouseY)
            is CapeSetting -> return capeSelector(setting).mouseClicked(mouseX, mouseY)

            is NumberSetting<*> -> {
                if (!isSliderHovered(y, mouseX, mouseY)) return false
                startDrag(setting, mouseX, mouseY) { dragX, _ -> setting.setFromPercentage(sliderPercentage(dragX)) }
            }

            is RangeSetting -> {
                if (!isSliderHovered(y, mouseX, mouseY)) return false
                val upper = setting.closerToUpper(sliderPercentage(mouseX))
                startDrag(setting, mouseX, mouseY) { dragX, _ ->
                    if (upper) setting.setUpperFromPercentage(sliderPercentage(dragX)) else setting.setLowerFromPercentage(sliderPercentage(dragX))
                }
            }

            is ColorSetting -> {
                val squareY = y + CARD_HEIGHT + PICKER_GAP
                if (!setting.expanded || !isAreaHovered(mouseX, mouseY, left, squareY - PICKER_GAP, inner, PICKER_HEIGHT)) {
                    setting.expanded = !setting.expanded
                    return true
                }
                val hue = mouseY >= squareY + SQUARE_HEIGHT + PICKER_GAP / 2f
                val pickerX = left
                val pickerWidth = inner
                startDrag(setting, mouseX, mouseY) { dragX, dragY ->
                    val saturation = (dragX - pickerX) / pickerWidth
                    if (hue) setting.setHue(saturation) else setting.setSaturationBrightness(saturation, 1f - (dragY - squareY) / SQUARE_HEIGHT)
                }
            }

            else -> return false
        }
        return true
    }

    private fun startDrag(setting: Setting<*>, mouseX: Float, mouseY: Float, update: (Float, Float) -> Unit) {
        dragging = setting
        drag = update
        update(mouseX, mouseY)
    }

    private fun GuiGraphicsExtractor.drawValue(value: String, textY: Float, color: CascadeGeometricColor) =
        text(value, right - textWidth(value), textY, color)

    private fun drawSlider(graphics: GuiGraphicsExtractor, setting: Setting<*>, display: String, start: Float, end: Float, y: Float, textY: Float, mouseX: Float, mouseY: Float) {
        graphics.drawValue(display, textY, theme.textMuted)
        val active = dragging == setting || (dragging == null && isSliderHovered(y, mouseX, mouseY))
        Slider.draw(graphics, left, controlY(y), inner, start, end, active)
    }

    private fun drawKeybind(graphics: GuiGraphicsExtractor, setting: KeybindSetting, textY: Float, hover: Float) {
        val label = when {
            listening == setting -> LISTENING
            setting.value == InputConstants.UNKNOWN -> UNBOUND
            else -> setting.value.displayName.string
        }
        val labelWidth = textWidth(label)
        val boxWidth = (labelWidth + KEY_PADDING * 2f).coerceAtLeast(KEY_MIN_WIDTH)
        val boxX = right - boxWidth
        val boxY = textY + (TEXT_SIZE - KEY_HEIGHT) / 2f

        graphics.roundedRectangle(boxX, boxY, boxWidth, KEY_HEIGHT, lerpColor(theme.control, theme.controlHovered, hover), Radius.MEDIUM)
        if (listening == setting) graphics.hollowRectangle(boxX, boxY, boxWidth, KEY_HEIGHT, 1f, theme.accent, Radius.MEDIUM)
        graphics.text(label, boxX + (boxWidth - labelWidth) / 2f, textY, theme.text)
    }

    private fun drawPicker(graphics: GuiGraphicsExtractor, setting: ColorSetting, squareY: Float) {
        graphics.roundedRectangle(left, squareY, inner, SQUARE_HEIGHT, CascadeGeometricColor(WHITE, setting.hueColor, WHITE, setting.hueColor), Radius.MEDIUM)
        graphics.roundedRectangle(left, squareY, inner, SQUARE_HEIGHT, CascadeGeometricColor(CLEAR, CLEAR, BLACK, BLACK), Radius.MEDIUM)
        graphics.handle(left + setting.saturation * inner, squareY + (1f - setting.brightness) * SQUARE_HEIGHT)

        val hueY = squareY + SQUARE_HEIGHT + PICKER_GAP
        val segmentWidth = inner / HUE_STOPS.size
        HUE_STOPS.forEachIndexed { index, color ->
            graphics.rectangle(left + index * segmentWidth, hueY, segmentWidth, HUE_HEIGHT, CascadeGeometricColor.horizontal(color, HUE_STOPS[(index + 1) % HUE_STOPS.size]))
        }
        graphics.handle(left + setting.hue * inner, hueY + HUE_HEIGHT / 2f)
    }

    private fun GuiGraphicsExtractor.handle(x: Float, y: Float) = ring(x, y, HANDLE_RADIUS - HANDLE_THICKNESS, HANDLE_RADIUS, theme.text)

    private fun textField(setting: StringSetting) = textFields.getOrPut(setting) { TextField({ setting.value }, { setting.value = it }) }

    private fun capeSelector(setting: CapeSetting) = capeSelectors.getOrPut(setting) { CapeSelector(setting) }

    private fun sliderPercentage(mouseX: Float): Float = Slider.percentage(mouseX, left, inner)

    private fun isSliderHovered(cardY: Float, mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, left, controlY(cardY) - SLIDER_GRAB, inner, Slider.HEIGHT + SLIDER_GRAB * 2f)

    private val fieldWidth get() = inner * FIELD_WIDTH_RATIO

    companion object {
        private const val INNER_PADDING = 8f
        private const val CONTROL_GAP = 6f
        private const val ICON_SIZE = 8f
        private const val SLIDER_GRAB = 4f
        private const val FIELD_WIDTH_RATIO = 0.62f
        private const val KEY_HEIGHT = 13f
        private const val KEY_PADDING = 6f
        private const val KEY_MIN_WIDTH = 34f
        private const val STATE_DURATION = 180L
        private const val QUARTER_TURN = (PI / 2.0).toFloat()
        private const val LISTENING = "..."
        private const val UNBOUND = "None"

        private const val SWATCH_WIDTH = 18f
        private const val SWATCH_HEIGHT = 10f
        private const val PICKER_GAP = 6f
        private const val SQUARE_HEIGHT = 56f
        private const val HUE_HEIGHT = 7f
        private const val HANDLE_RADIUS = 3.5f
        private const val HANDLE_THICKNESS = 1.2f
        private const val PICKER_HEIGHT = PICKER_GAP * 3f + SQUARE_HEIGHT + HUE_HEIGHT

        private const val CARD_HEIGHT = TEXT_SIZE + INNER_PADDING * 2f
        private const val TEXT_CARD_HEIGHT = TextField.HEIGHT + INNER_PADDING * 2f

        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val BLACK = 0xFF000000.toInt()
        private const val CLEAR = 0x00000000

        private val HUE_STOPS = intArrayOf(0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(), 0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt())

        private fun controlY(cardY: Float): Float = cardY + INNER_PADDING + TEXT_SIZE + CONTROL_GAP

        private fun height(setting: Setting<*>): Float = when (setting) {
            is NumberSetting<*>, is RangeSetting -> CARD_HEIGHT + CONTROL_GAP + Slider.HEIGHT
            is StringSetting -> TEXT_CARD_HEIGHT
            is ColorSetting -> if (setting.expanded) CARD_HEIGHT + PICKER_HEIGHT else CARD_HEIGHT
            is CapeSetting -> CARD_HEIGHT + CONTROL_GAP + CapeSelector.HEIGHT
            else -> CARD_HEIGHT
        }

        private fun grouped(previous: Setting<*>, setting: Setting<*>): Boolean {
            val parent = setting.parent ?: return false
            return parent == previous || parent == previous.parent
        }

        private fun corners(roundTop: Boolean, roundBottom: Boolean): CascadeGeometricRadius = when {
            roundTop && roundBottom -> Radius.LARGE
            roundTop -> Radius.TOP
            roundBottom -> Radius.BOTTOM
            else -> CascadeGeometricRadius.ZERO
        }

        private fun isClickable(setting: Setting<*>): Boolean =
            setting is BooleanSetting || setting is SelectorSetting || setting is DropdownSetting || setting is ActionSetting || setting is KeybindSetting
    }
}
