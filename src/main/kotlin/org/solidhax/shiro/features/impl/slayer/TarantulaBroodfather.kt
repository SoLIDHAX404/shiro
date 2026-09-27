package org.solidhax.shiro.features.impl.slayer

import net.minecraft.world.entity.EntityTypes
import org.solidhax.shiro.gui.DummyEntity

object TarantulaBroodfather : SlayerModule(
    SlayerType.TARANTULA,
    DummyEntity(EntityTypes.SPIDER),
    SlayerInfo(SlayerType.TARANTULA, "IV", timer = "03:12", health = "1.5M"),
)
