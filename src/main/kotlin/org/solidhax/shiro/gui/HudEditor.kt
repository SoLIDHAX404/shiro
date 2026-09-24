package org.solidhax.shiro.gui

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.InputConstants
import foo.starred.cascade.graphics.extensions.blur.blur
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.solidhax.shiro.Shiro
import org.solidhax.shiro.Shiro.logger
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.settings.impl.HudElement
import org.solidhax.shiro.gui.settings.impl.HudSetting
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.animation.AnimationManager
import org.solidhax.shiro.utils.ui.animation.Animations
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.lerpColor
import org.solidhax.shiro.utils.ui.text
import org.solidhax.shiro.utils.ui.textWidth
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Screen for moving and resizing [HudSetting]s. Opened with /shiro hud or from a HUD setting in the ClickGUI.
 */
object HudEditor : Screen(Component.literal("Shiro HUD Editor")) {

    // declared first: object properties initialize top to bottom and gridSize reads this
    private val GRID_SIZES = intArrayOf(5, 10, 20)

    var gridEnabled = true
        private set

    var gridSize = GRID_SIZES[1]
        private set

    private var parent: Screen? = null
    private var dragging: HudSetting? = null
    private var resizing: HudSetting? = null
    private var selected: HudSetting? = null
    private var offsetX = 0f
    private var offsetY = 0f

    private val buttonHover = Animations<ToolbarButton>()
    private val elementHover = Animations<HudSetting>()
    private val buttons = listOf(
        ToolbarButton({ "Grid" }, checkbox = { gridEnabled }) { gridEnabled = !gridEnabled },
        ToolbarButton({ "Size: $gridSize" }) { gridSize = GRID_SIZES[(GRID_SIZES.indexOf(gridSize) + 1) % GRID_SIZES.size] },
        ToolbarButton({ "Reset All" }) { elements.forEach { it.value.reset() } },
    )

    private val prefsFile get() = File(Shiro.configDir, "hud-editor.json")
    private val elements get() = ModuleManager.hudSettings.filter(HudSetting::isEnabled)
    private val snapping get() = gridEnabled != mc.hasShiftDown()

    init {
        loadPrefs()
    }

    fun open(parent: Screen? = null) {
        this.parent = parent
        mc.gui.setScreen(this)
    }

    override fun init() {
        AnimationManager.update()
        elements.forEach { it.value.clamp(width.toFloat(), height.toFloat()) }
        super.init()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        AnimationManager.update()
        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()
        val elements = elements
        val toolbarHovered = isToolbarHovered(mx, my)
        val hovered = if (toolbarHovered) null else hoveredElement(elements, mx, my)

        graphics.rectangle(0f, 0f, width.toFloat(), height.toFloat(), BACKDROP)
        if (gridEnabled) drawGrid(graphics)
        if (dragging != null || resizing != null) drawCenterGuides(graphics)

        for (setting in elements) {
            val hud = setting.value
            hud.draw(graphics, true)

            val active = setting == dragging || setting == resizing || setting == selected
            val highlight = elementHover[setting].animate(active || setting == hovered)
            graphics.rectangle(hud.x, hud.y, hud.scaledWidth, hud.scaledHeight, lerpColor(ELEMENT_FILL, theme.accent and 0xFFFFFF or ELEMENT_FILL_ALPHA, highlight))
            graphics.hollowRectangle(hud.x, hud.y, hud.scaledWidth, hud.scaledHeight, pixel, lerpColor(theme.divider, theme.accent, highlight), Radius.SMALL)
            if (highlight > 0f) drawHandle(graphics, hud, highlight)
        }

        (dragging ?: resizing ?: hovered)?.let { drawLabel(graphics, it) }

        if (elements.isEmpty()) {
            graphics.text(EMPTY, (width - textWidth(EMPTY)) / 2f, (height - TEXT_SIZE) / 2f, theme.textMuted)
        }

        drawToolbar(graphics, mx, my)
        graphics.text(HINT, (width - textWidth(HINT, HINT_SIZE)) / 2f, height - HINT_SIZE - EDGE_MARGIN, theme.textMuted, HINT_SIZE)

        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x().toFloat()
        val my = event.y().toFloat()

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            buttons.firstOrNull { it.isHovered(mx, my) }?.let {
                it.action()
                return true
            }
        }
        if (isToolbarHovered(mx, my)) return true

        val elements = elements
        val hovered = resizeTarget(elements, mx, my) ?: hoveredElement(elements, mx, my)
        selected = hovered
        if (hovered == null) return super.mouseClicked(event, doubleClick)
        val hud = hovered.value

        when (event.button()) {
            InputConstants.MOUSE_BUTTON_LEFT -> if (isOnHandle(hud, mx, my)) {
                resizing = hovered
                offsetX = hud.x + hud.scaledWidth - mx
                offsetY = hud.y + hud.scaledHeight - my
            } else {
                dragging = hovered
                offsetX = hud.x - mx
                offsetY = hud.y - my
            }

            InputConstants.MOUSE_BUTTON_RIGHT -> {
                hud.reset()
                hud.clamp(width.toFloat(), height.toFloat())
            }
        }
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        val mx = event.x().toFloat()
        val my = event.y().toFloat()

        dragging?.value?.let {
            it.x = snap(mx + offsetX, it.scaledWidth, width.toFloat())
            it.y = snap(my + offsetY, it.scaledHeight, height.toFloat())
            return true
        }
        resizing?.value?.let {
            resize(it, mx + offsetX, my + offsetY)
            return true
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        dragging = null
        resizing = null
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val hovered = hoveredElement(elements, mouseX.toFloat(), mouseY.toFloat())
            ?: return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        scaleBy(hovered.value, verticalAmount.sign.toFloat() * SCALE_STEP)
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == InputConstants.KEY_G) {
            gridEnabled = !gridEnabled
            return true
        }

        val target = (selected?.takeIf(HudSetting::isEnabled) ?: hoveredElement(elements, cursorX, cursorY))?.value
            ?: return super.keyPressed(event)
        val step = if (gridEnabled && !event.hasShiftDown()) gridSize.toFloat() else 1f

        when (event.key()) {
            InputConstants.KEY_LEFT -> target.x -= step
            InputConstants.KEY_RIGHT -> target.x += step
            InputConstants.KEY_UP -> target.y -= step
            InputConstants.KEY_DOWN -> target.y += step
            InputConstants.KEY_EQUALS, InputConstants.KEY_ADD -> scaleBy(target, SCALE_STEP)
            InputConstants.KEY_MINUS -> scaleBy(target, -SCALE_STEP)
            InputConstants.KEY_R -> target.reset()
            else -> return super.keyPressed(event)
        }
        target.clamp(width.toFloat(), height.toFloat())
        return true
    }

    override fun onClose() {
        mc.gui.setScreen(parent)
    }

    override fun removed() {
        dragging = null
        resizing = null
        selected = null
        ModuleManager.saveConfigurations()
        savePrefs()
        super.removed()
    }

    override fun extractBlurredBackground(graphics: GuiGraphicsExtractor) {}
    override fun extractMenuBackground(graphics: GuiGraphicsExtractor) {}
    override fun isPauseScreen(): Boolean = false

    // mouse position for keyboard input, which has no coordinates of its own
    private val cursorX get() = (mc.mouseHandler.xpos() * width / mc.window.screenWidth).toFloat()
    private val cursorY get() = (mc.mouseHandler.ypos() * height / mc.window.screenHeight).toFloat()

    // one physical pixel, so grid lines stay crisp at every GUI scale
    private val pixel get() = 1f / mc.window.guiScale

    private fun hoveredElement(elements: List<HudSetting>, mouseX: Float, mouseY: Float): HudSetting? =
        elements.lastOrNull { it.value.isHovered(mouseX, mouseY) }

    private fun resizeTarget(elements: List<HudSetting>, mouseX: Float, mouseY: Float): HudSetting? =
        elements.lastOrNull { isOnHandle(it.value, mouseX, mouseY) }

    private fun isOnHandle(hud: HudElement, mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(
            mouseX, mouseY,
            hud.x + hud.scaledWidth - HANDLE_SIZE / 2f - HANDLE_GRAB, hud.y + hud.scaledHeight - HANDLE_SIZE / 2f - HANDLE_GRAB,
            HANDLE_SIZE + HANDLE_GRAB * 2f, HANDLE_SIZE + HANDLE_GRAB * 2f
        )

    /**
     * Snaps a position so that either edge of the element lands on the grid, or on the far screen edge.
     */
    private fun snap(value: Float, size: Float, screen: Float): Float {
        val max = (screen - size).coerceAtLeast(0f)
        if (!snapping) return value.coerceIn(0f, max)

        val grid = gridSize.toFloat()
        val candidates = floatArrayOf(round(value / grid) * grid, round((value + size) / grid) * grid - size, max)
        return candidates.minBy { abs(it - value) }.coerceIn(0f, max)
    }

    private fun resize(hud: HudElement, right: Float, bottom: Float) {
        if (hud.width <= 0f || hud.height <= 0f) return
        var scale = max((right - hud.x) / hud.width, (bottom - hud.y) / hud.height)

        if (snapping) {
            val grid = gridSize.toFloat()
            val snappedRight = round((hud.x + hud.width * scale) / grid) * grid
            if (snappedRight > hud.x) scale = (snappedRight - hud.x) / hud.width
        }

        val fits = min((width - hud.x) / hud.width, (height - hud.y) / hud.height)
        hud.scale = scale.coerceAtMost(fits)
    }

    private fun scaleBy(hud: HudElement, amount: Float) {
        hud.scale = ((hud.scale + amount) / SCALE_STEP).roundToInt() * SCALE_STEP
        hud.clamp(width.toFloat(), height.toFloat())
    }

    private fun drawGrid(graphics: GuiGraphicsExtractor) {
        val pixel = pixel
        var line = 0
        while (line * gridSize <= width) {
            graphics.rectangle(line * gridSize.toFloat(), 0f, pixel, height.toFloat(), if (line % MAJOR_EVERY == 0) GRID_MAJOR else GRID_MINOR)
            line++
        }
        line = 0
        while (line * gridSize <= height) {
            graphics.rectangle(0f, line * gridSize.toFloat(), width.toFloat(), pixel, if (line % MAJOR_EVERY == 0) GRID_MAJOR else GRID_MINOR)
            line++
        }
    }

    private fun drawCenterGuides(graphics: GuiGraphicsExtractor) {
        val guide = theme.accent and 0xFFFFFF or GUIDE_ALPHA
        graphics.rectangle(width / 2f, 0f, pixel, height.toFloat(), guide)
        graphics.rectangle(0f, height / 2f, width.toFloat(), pixel, guide)
    }

    private fun drawHandle(graphics: GuiGraphicsExtractor, hud: HudElement, alpha: Float) {
        val x = hud.x + hud.scaledWidth - HANDLE_SIZE / 2f
        val y = hud.y + hud.scaledHeight - HANDLE_SIZE / 2f
        graphics.roundedRectangle(x, y, HANDLE_SIZE, HANDLE_SIZE, lerpColor(theme.accent and 0xFFFFFF, theme.accent, alpha), Radius.SMALL)
    }

    private fun drawLabel(graphics: GuiGraphicsExtractor, setting: HudSetting) {
        val hud = setting.value
        val label = "${setting.name}  ${(hud.scale * 100f).roundToInt()}%"
        val labelWidth = textWidth(label, LABEL_SIZE) + LABEL_PADDING * 2f
        val labelHeight = LABEL_SIZE + LABEL_PADDING * 2f

        val x = hud.x.coerceIn(0f, (width - labelWidth).coerceAtLeast(0f))
        val above = hud.y - labelHeight - LABEL_GAP
        val y = if (above >= 0f) above else hud.y + hud.scaledHeight + LABEL_GAP

        graphics.roundedRectangle(x, y, labelWidth, labelHeight, LABEL_BACKGROUND, Radius.MEDIUM)
        graphics.text(label, x + LABEL_PADDING, y + LABEL_PADDING, theme.text, LABEL_SIZE)
    }

    private fun drawToolbar(graphics: GuiGraphicsExtractor, mouseX: Float, mouseY: Float) {
        var x = (width - toolbarWidth) / 2f
        val y = EDGE_MARGIN
        graphics.blur(x, y, toolbarWidth, TOOLBAR_HEIGHT, theme.panelTint, Radius.LARGE, BLUR_RADIUS)
        graphics.hollowRectangle(x, y, toolbarWidth, TOOLBAR_HEIGHT, 1f, theme.border, Radius.LARGE)

        x += TOOLBAR_PADDING
        for (button in buttons) {
            button.x = x
            button.y = y + TOOLBAR_PADDING
            val hover = buttonHover[button].animate(button.isHovered(mouseX, mouseY))
            graphics.roundedRectangle(button.x, button.y, button.width, BUTTON_HEIGHT, lerpColor(theme.control, theme.controlHovered, hover), Radius.MEDIUM)

            var textX = button.x + BUTTON_PADDING
            val textY = button.y + (BUTTON_HEIGHT - TEXT_SIZE) / 2f
            button.checkbox?.let {
                Checkbox.draw(graphics, textX, button.y + (BUTTON_HEIGHT - Checkbox.SIZE) / 2f, hover > 0.5f, if (it()) 1f else 0f)
                textX += Checkbox.SIZE + CHECKBOX_GAP
            }
            graphics.text(button.label(), textX, textY, lerpColor(theme.textMuted, theme.text, hover))
            x += button.width + TOOLBAR_PADDING
        }
    }

    private val toolbarWidth: Float
        get() = buttons.sumOf { it.width.toDouble() }.toFloat() + TOOLBAR_PADDING * (buttons.size + 1)

    private fun isToolbarHovered(mouseX: Float, mouseY: Float): Boolean =
        isAreaHovered(mouseX, mouseY, (width - toolbarWidth) / 2f, EDGE_MARGIN, toolbarWidth, TOOLBAR_HEIGHT)

    private fun loadPrefs() {
        try {
            if (!prefsFile.exists()) return
            val json = JsonParser.parseString(prefsFile.readText()).asJsonObject
            json.get("grid")?.asBoolean?.let { gridEnabled = it }
            json.get("gridSize")?.asInt?.takeIf { it in GRID_SIZES }?.let { gridSize = it }
        } catch (e: Exception) {
            logger.error("Error loading HUD editor preferences", e)
        }
    }

    private fun savePrefs() {
        try {
            val json = JsonObject().apply {
                addProperty("grid", gridEnabled)
                addProperty("gridSize", gridSize)
            }
            prefsFile.parentFile?.mkdirs()
            prefsFile.writeText(GsonBuilder().setPrettyPrinting().create().toJson(json))
        } catch (e: Exception) {
            logger.error("Error saving HUD editor preferences", e)
        }
    }

    private class ToolbarButton(val label: () -> String, val checkbox: (() -> Boolean)? = null, val action: () -> Unit) {
        var x = 0f
        var y = 0f

        val width: Float
            get() = textWidth(label()) + BUTTON_PADDING * 2f + if (checkbox != null) Checkbox.SIZE + CHECKBOX_GAP else 0f

        fun isHovered(mouseX: Float, mouseY: Float): Boolean = isAreaHovered(mouseX, mouseY, x, y, width, BUTTON_HEIGHT)
    }

    private const val MAJOR_EVERY = 5
    private const val SCALE_STEP = 0.1f

    private const val HANDLE_SIZE = 5f
    private const val HANDLE_GRAB = 2f

    private const val EDGE_MARGIN = 8f
    private const val TOOLBAR_PADDING = 4f
    private const val BUTTON_HEIGHT = 18f
    private const val BUTTON_PADDING = 7f
    private const val CHECKBOX_GAP = 5f
    private const val TOOLBAR_HEIGHT = BUTTON_HEIGHT + TOOLBAR_PADDING * 2f
    private const val BLUR_RADIUS = 30f

    private const val LABEL_SIZE = 7f
    private const val LABEL_PADDING = 3f
    private const val LABEL_GAP = 3f
    private const val HINT_SIZE = 7f

    private const val BACKDROP = 0x66000000
    private const val GRID_MINOR = 0x14FFFFFF
    private const val GRID_MAJOR = 0x2EFFFFFF
    private const val GUIDE_ALPHA = 0x80000000.toInt()
    private const val ELEMENT_FILL = 0x0FFFFFFF
    private const val ELEMENT_FILL_ALPHA = 0x1F000000
    private const val LABEL_BACKGROUND = 0xE0202123.toInt()

    private const val EMPTY = "No HUD elements are enabled. Turn on a module with a HUD to edit it here."
    private const val HINT = "Drag to move · Drag corner to resize · Scroll to scale · Right-click to reset · Shift bypasses grid · G toggles grid"
}
