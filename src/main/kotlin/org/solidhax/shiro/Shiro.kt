package org.solidhax.shiro

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.solidhax.shiro.commands.mainCommand
import org.solidhax.shiro.config.ModuleConfig
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.features.impl.mining.CorpseESP
import org.solidhax.shiro.features.impl.misc.TestModule
import java.io.File

object Shiro : ClientModInitializer {

    const val MOD_ID = "shiro"

    @JvmStatic
    val mc: Minecraft = Minecraft.getInstance()

    val logger: Logger = LoggerFactory.getLogger("Shiro")

    val configDir: File = FabricLoader.getInstance().configDir.resolve(MOD_ID).toFile()

    override fun onInitializeClient() {
        ModuleManager.registerModules(
            ModuleConfig("shiro-config.json"),
            TestModule,
            CorpseESP,
        )

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(MOD_ID, "hud"), ModuleManager::renderHud)

        ClientLifecycleEvents.CLIENT_STOPPING.register { ModuleManager.saveConfigurations() }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            mainCommand.register(dispatcher)
        }
    }
}
