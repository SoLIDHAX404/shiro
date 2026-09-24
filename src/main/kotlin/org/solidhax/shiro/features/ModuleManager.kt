package org.solidhax.shiro.features

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.config.ModuleConfig
import org.solidhax.shiro.gui.HudEditor
import org.solidhax.shiro.gui.settings.impl.HudSetting
import org.solidhax.shiro.utils.ui.animation.AnimationManager

object ModuleManager {

    val modules: HashMap<String, Module> = linkedMapOf()

    val modulesByCategory: HashMap<Category, ArrayList<Module>> = hashMapOf()

    val configs: ArrayList<ModuleConfig> = arrayListOf()

    val hudSettings: ArrayList<HudSetting> = arrayListOf()

    /**
     * Registers [modules] and loads their saved state from [config].
     */
    fun registerModules(config: ModuleConfig, vararg modules: Module) {
        for (module in modules) {
            if (module.isDevModule && !FabricLoader.getInstance().isDevelopmentEnvironment) continue

            val lowercase = module.name.lowercase()
            config.modules[lowercase] = module
            this.modules[lowercase] = module
            this.modulesByCategory.getOrPut(module.category) { arrayListOf() }.add(module)

            for (setting in module.settings.values) {
                if (setting is HudSetting) hudSettings.add(setting)
            }
        }
        configs.add(config)
        config.load()
    }

    fun loadConfigurations() {
        for (config in configs) config.load()
    }

    fun saveConfigurations() {
        for (config in configs) config.save()
    }

    fun renderHud(graphics: GuiGraphicsExtractor, @Suppress("UNUSED_PARAMETER") deltaTracker: DeltaTracker) {
        if (mc.level == null || mc.player == null || mc.gui.screen() == HudEditor || mc.gui.hud.isHidden) return
        AnimationManager.update()
        for (setting in hudSettings) {
            if (setting.isEnabled) setting.value.draw(graphics, false)
        }
    }
}
