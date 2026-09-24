package org.solidhax.shiro.features

import net.fabricmc.loader.api.FabricLoader
import org.solidhax.shiro.config.ModuleConfig

object ModuleManager {

    val modules: HashMap<String, Module> = linkedMapOf()

    val modulesByCategory: HashMap<Category, ArrayList<Module>> = hashMapOf()

    val configs: ArrayList<ModuleConfig> = arrayListOf()

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
}
