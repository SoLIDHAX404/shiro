package org.solidhax.shiro.features.impl.misc

import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.NumberSetting
import org.solidhax.shiro.gui.settings.impl.SelectorSetting

object AspectRatio : Module(
    name = "Aspect Ratio",
    description = "Renders the world at a different aspect ratio and stretches it to fill the screen."
) {
    private val mode by SelectorSetting("Mode", "16:9", listOf("16:9", "4:3", "Custom"), desc = "The aspect ratio the world is rendered at.")
    private val customRatio by NumberSetting("Aspect Ratio", 1.33, 0.5, 3.0, 0.01, desc = "Width divided by height, e.g. 1.33 for 4:3.").withDependency { mode == MODE_CUSTOM }

    /**
     * The aspect ratio the world should be rendered at, or null to keep the window's own.
     */
    @JvmStatic
    fun currentRatio(): Float? {
        if (!enabled) return null
        return when (mode) {
            MODE_16_9 -> 16f / 9f
            MODE_4_3 -> 4f / 3f
            else -> customRatio.toFloat()
        }
    }

    private const val MODE_16_9 = 0
    private const val MODE_4_3 = 1
    private const val MODE_CUSTOM = 2
}
