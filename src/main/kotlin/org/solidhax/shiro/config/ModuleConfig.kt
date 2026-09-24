package org.solidhax.shiro.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.solidhax.shiro.Shiro
import org.solidhax.shiro.Shiro.logger
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.settings.Saving
import java.io.File

/**
 * Saves modules (enabled state and every [Saving] setting) to a JSON file and loads them back.
 */
class ModuleConfig(private val file: File) {

    constructor(fileName: String) : this(File(Shiro.configDir, fileName))

    // key is the module name in lowercase
    internal val modules: HashMap<String, Module> = linkedMapOf()

    fun load() {
        try {
            if (!file.exists()) return
            val text = file.readText()
            if (text.isBlank()) return

            for (element in JsonParser.parseString(text).asJsonArray) {
                val moduleObj = element?.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                val module = modules[moduleObj.get("name")?.asString?.lowercase() ?: continue] ?: continue

                moduleObj.get("enabled")?.asBoolean?.let { if (it != module.enabled) module.toggle() }

                val settingsObj = moduleObj.get("settings")?.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                for ((key, value) in settingsObj.entrySet()) {
                    val setting = module.settings[key] as? Saving ?: continue
                    try {
                        setting.read(value, gson)
                    } catch (e: Exception) {
                        logger.warn("Failed to load setting '$key' of module '${module.name}'", e)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error loading module config from $file", e)
        }
    }

    fun save() {
        try {
            val jsonArray = JsonArray().apply {
                for (module in modules.values) {
                    add(JsonObject().apply {
                        add("name", JsonPrimitive(module.name))
                        add("enabled", JsonPrimitive(module.enabled))
                        add("settings", JsonObject().apply {
                            for ((name, setting) in module.settings) {
                                if (setting is Saving) add(name, setting.write(gson))
                            }
                        })
                    })
                }
            }
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(jsonArray))
        } catch (e: Exception) {
            logger.error("Error saving module config to $file", e)
        }
    }

    override fun toString(): String = "ModuleConfig(file=$file)"

    private companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    }
}
