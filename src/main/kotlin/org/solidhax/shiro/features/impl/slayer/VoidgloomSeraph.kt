package org.solidhax.shiro.features.impl.slayer

import net.minecraft.world.entity.EntityTypes
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.DummyEntity
import org.solidhax.shiro.utils.ui.LabelSegments
import java.util.Locale

object VoidgloomSeraph : SlayerModule(
    SlayerType.VOIDGLOOM,
    DummyEntity(EntityTypes.ENDERMAN),
    SlayerInfo(SlayerType.VOIDGLOOM, "IV", timer = "02:47", hits = 15, health = "45.2M"),
) {
    override fun timerText(info: SlayerInfo): LabelSegments {
        val timer = info.timer ?: return emptyList()
        val ticks = info.vehicleTicks ?: return super.timerText(info)
        val laser = (LASER_SECONDS - ticks / 20.0).coerceAtLeast(0.0)
        val laserText = if (laser <= 0.0) "Soon" else String.format(Locale.ROOT, "%.1fs", laser)
        return listOf(laserText to color(timerColor), " $timer" to theme.textMuted)
    }

    private const val LASER_SECONDS = 8.2
}
