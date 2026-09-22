package org.solidhax.shiro.features

import net.fabricmc.loader.api.FabricLoader

object ModuleManager {

    val modules: HashMap<String, Module> = linkedMapOf()

    val modulesByCategory: HashMap<Category, ArrayList<Module>> = hashMapOf()

    fun registerModules(vararg modules: Module) {
        for (module in modules) {
            if (module.isDevModule && !FabricLoader.getInstance().isDevelopmentEnvironment) continue

            val lowercase = module.name.lowercase()
            this.modules[lowercase] = module
            this.modulesByCategory.getOrPut(module.category) { arrayListOf() }.add(module)
        }
    }
}
