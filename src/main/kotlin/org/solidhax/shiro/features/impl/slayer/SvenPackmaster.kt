package org.solidhax.shiro.features.impl.slayer

import net.minecraft.world.entity.EntityTypes
import org.solidhax.shiro.gui.DummyEntity

object SvenPackmaster : SlayerModule(
    SlayerType.SVEN,
    DummyEntity(EntityTypes.WOLF),
    SlayerInfo(SlayerType.SVEN, "IV", timer = "02:59", health = "1.4M", maxHealth = 2_000_000.0),
)
