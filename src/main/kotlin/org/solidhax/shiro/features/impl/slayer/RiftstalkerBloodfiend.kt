package org.solidhax.shiro.features.impl.slayer

import com.mojang.authlib.GameProfile
import org.solidhax.shiro.gui.DummyEntity
import java.util.UUID

object RiftstalkerBloodfiend : SlayerModule(
    SlayerType.VAMPIRE,
    DummyEntity.player { GameProfile(UUID(0L, 0L), "Bloodfiend") },
    SlayerInfo(SlayerType.VAMPIRE, "V", timer = "03:30", health = "4.2k"),
)
