package org.solidhax.shiro.utils.ui

import foo.starred.cascade.wrappers.svg.impl.CascadeSVG
import net.minecraft.resources.Identifier
import org.solidhax.shiro.Shiro.mc

private val sources = HashMap<Identifier, String>()

fun svgTexture(id: Identifier, width: Int, height: Int = width): Identifier {
    val source = sources.getOrPut(id) {
        mc.resourceManager.getResourceOrThrow(id).openAsReader().use { it.readText() }
            .replace("currentColor", "#FFFFFF")
    }
    return CascadeSVG.load(source, id, width, height)
}
