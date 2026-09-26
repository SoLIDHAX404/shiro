package org.solidhax.shiro.utils

import net.minecraft.resources.Identifier
import org.solidhax.shiro.Shiro.MOD_ID
import org.solidhax.shiro.Shiro.logger

fun shiroId(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

fun Any?.equalsOneOf(vararg options: Any?): Boolean =
    options.any { this == it }

fun logError(throwable: Throwable, context: Any) =
    logger.error("Caught an ${throwable::class.simpleName ?: "error"} at ${context::class.simpleName}.", throwable)
