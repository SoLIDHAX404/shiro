package org.solidhax.shiro.cosmetics

import net.minecraft.resources.Identifier

enum class Cape(val displayName: String, private val file: String?) {
    NONE("None", null),
    MINECON13("Minecon 2013", "minecon13"),
    MINECON15("Minecon 2015", "minecon15"),
    MINECON16("Minecon 2016", "minecon16");

    val texture: Identifier? by lazy {
        file?.let { Identifier.fromNamespaceAndPath("shiro", "textures/cape/$it.png") }
    }
}
