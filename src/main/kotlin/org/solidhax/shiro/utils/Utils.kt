package org.solidhax.shiro.utils

import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.solidhax.shiro.Shiro.MOD_ID
import org.solidhax.shiro.Shiro.logger
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI.theme

fun shiroId(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

fun modMessage(message: Component) {
    mc.player?.sendSystemMessage(Component.empty().append(Component.literal("[Shiro] ").withColor(theme.accent and 0xFFFFFF)).append(message))
}

fun logError(throwable: Throwable, context: Any) =
    logger.error("Caught an ${throwable::class.simpleName ?: "error"} at ${context::class.simpleName}.", throwable)
