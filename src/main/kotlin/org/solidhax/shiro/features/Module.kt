package org.solidhax.shiro.features

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.Shiro
import org.solidhax.shiro.gui.settings.Setting
import org.solidhax.shiro.gui.settings.impl.HudSetting

abstract class Module(
    val name: String,
    category: Category? = null,
    var description: String,
    toggled: Boolean = false,
) {

    val settings: LinkedHashMap<String, Setting<*>> = linkedMapOf()

    val category: Category = category ?: getCategoryFromPackage(this::class.java)

    var enabled: Boolean = toggled
        private set

    protected inline val mc get() = Shiro.mc

    val isDevModule = this::class.java.isAnnotationPresent(DevModule::class.java)

    open fun onEnable() {}

    open fun onDisable() {}

    fun toggle() {
        enabled = !enabled
        if (enabled) onEnable()
        else onDisable()
    }

    fun <K : Setting<*>> registerSetting(setting: K): K {
        settings[setting.name] = setting
        return setting
    }

    operator fun <K : Setting<*>> K.unaryPlus(): K = registerSetting(this)

    /**
     * Creates a HUD element that can be moved and scaled in the HUD editor (/shiro hud).
     * [block] draws the content at (0, 0) and returns its unscaled (width, height).
     */
    fun HUD(
        name: String,
        desc: String,
        toggleable: Boolean = true,
        x: Float = 10f,
        y: Float = 10f,
        scale: Float = 1f,
        block: GuiGraphicsExtractor.(example: Boolean) -> Pair<Float, Float>,
    ): HudSetting = HudSetting(name, x, y, scale, toggleable, desc, this, block)

    private companion object {
        private fun getCategoryFromPackage(clazz: Class<out Module>): Category {
            val packageName = clazz.packageName
            return when {
                packageName.contains("general") -> Category.GENERAL
                packageName.contains("mining") -> Category.MINING
                packageName.contains("farming") -> Category.FARMING
                packageName.contains("fishing") -> Category.FISHING
                packageName.contains("misc") -> Category.MISC
                else -> throw IllegalStateException(
                    "Module ${clazz.name} failed to get category from the package it is in. " +
                            "Either manually assign a category, " +
                            "or put it under any valid package (general, mining, farming, fishing, misc)"
                )
            }
        }
    }
}
