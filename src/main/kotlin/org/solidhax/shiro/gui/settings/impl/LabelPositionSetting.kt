package org.solidhax.shiro.gui.settings.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.solidhax.shiro.gui.settings.Saving
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.utils.ui.BLACK
import org.solidhax.shiro.utils.ui.Label
import org.solidhax.shiro.utils.ui.LabelPosition
import org.solidhax.shiro.utils.ui.LabelSegments
import org.solidhax.shiro.utils.ui.LabelStyle
import org.solidhax.shiro.utils.ui.TEXT_SIZE
import org.solidhax.shiro.utils.ui.withAlpha

/**
 * Stores where a label sits around an entity's bounds. It has no row in the settings list; it is set by dragging the
 * label in the module's [PreviewSetting].
 */
class LabelPositionSetting(
    name: String,
    override val default: LabelPosition = LabelPosition.TOP,
    desc: String = "",
    textSettings: List<Setting<*>> = emptyList(),
) : Setting<LabelPosition>(name, desc), Saving {

    override var value: LabelPosition = default

    val title: String = name.removeSuffix(" Position")

    private val textDropdown = DropdownSetting("Text")
    private val fontSize = NumberSetting("Font Size", TEXT_SIZE, 4, 16, 0.5, desc = "Size of the label's text.", unit = "px").withDependency(textDropdown)
    private val textShadow = BooleanSetting("Shadow", false, desc = "Draws a shadow behind the label's text.").withDependency(textDropdown)
    private val extraTextSettings = textSettings.map { it.withDependency(textDropdown) }.toMutableList()

    private val backgroundDropdown = DropdownSetting("Background")
    private val backgroundType = SelectorSetting("Type", "Rounded", listOf("None", "Rounded", "Square"), desc = "Shape drawn behind the label.").withDependency(backgroundDropdown)
    private val backgroundColor = ColorSetting("Color", BLACK, desc = "Color of the background.").withDependency(backgroundDropdown)
    private val backgroundOpacity = NumberSetting("Opacity", 20, 0, 100, 5, desc = "How see-through the background is.", unit = "%").withDependency(backgroundDropdown)
    private val backgroundPadding = NumberSetting("Padding", LabelStyle.DEFAULT_PADDING, 0, 12, 1, desc = "Space between the edge of the background and the text.", unit = "px").withDependency(backgroundDropdown)

    val settings: List<Setting<*>>
        get() = listOf(textDropdown, fontSize, textShadow) + extraTextSettings +
                listOf(backgroundDropdown, backgroundType, backgroundColor, backgroundOpacity, backgroundPadding)

    val style: LabelStyle
        get() = LabelStyle(
            fontSize.value,
            textShadow.enabled,
            LabelStyle.Background.entries[backgroundType.value],
            withAlpha(backgroundColor.value, backgroundOpacity.value * 255 / 100),
            backgroundPadding.value,
        )

    init {
        hidden = true
    }

    fun addTextSetting(setting: Setting<*>) {
        extraTextSettings.add(setting.withDependency(textDropdown))
    }

    fun label(segments: LabelSegments): Label = Label(value, segments, style)

    override fun write(gson: Gson): JsonElement = JsonObject().apply {
        addProperty("side", value.side.name)
        addProperty("along", value.along)
        add("settings", JsonObject().apply {
            for (setting in settings) if (setting is Saving) add(setting.key, setting.write(gson))
        })
    }

    override fun read(element: JsonElement, gson: Gson) {
        // older configs saved only the side as a string, which now means the middle of that side
        val obj = element.takeIf { it.isJsonObject }?.asJsonObject
        val sideName = obj?.get("side")?.asString ?: element.takeIf { it.isJsonPrimitive }?.asString
        LabelPosition.Side.entries.find { it.name == sideName }?.let { side ->
            value = LabelPosition(side, obj?.get("along")?.asFloat?.coerceIn(0f, 1f) ?: 0.5f)
        }

        val saved = obj?.get("settings") as? JsonObject ?: return
        for (setting in settings) {
            if (setting is Saving) saved.get(setting.key)?.let { setting.read(it, gson) }
        }
    }
}
