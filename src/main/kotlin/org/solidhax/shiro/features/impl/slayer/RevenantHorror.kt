package org.solidhax.shiro.features.impl.slayer

import net.minecraft.world.entity.EntityTypes
import org.solidhax.shiro.gui.DummyEntity

object RevenantHorror : SlayerModule(
    SlayerType.REVENANT,
    DummyEntity(EntityTypes.ZOMBIE),
    SlayerInfo(SlayerType.REVENANT, "V", timer = "02:31", health = "8.4M", maxHealth = 10_000_000.0),
)
