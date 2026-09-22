package org.solidhax.shiro

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.Minecraft
import org.solidhax.shiro.commands.mainCommand
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.features.impl.misc.TestModule

object Shiro : ClientModInitializer {

    @JvmStatic
    val mc: Minecraft = Minecraft.getInstance()

    override fun onInitializeClient() {
        ModuleManager.registerModules(
            TestModule,
        )

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(mainCommand).forEach { commodore -> commodore.register(dispatcher) }
        }
    }
}
