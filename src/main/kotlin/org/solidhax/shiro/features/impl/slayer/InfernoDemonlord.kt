package org.solidhax.shiro.features.impl.slayer

import net.minecraft.world.entity.EntityTypes
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.DummyEntity
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.utils.ui.LabelSegments

object InfernoDemonlord : SlayerModule(
    SlayerType.INFERNO,
    DummyEntity(EntityTypes.BLAZE),
    SlayerInfo(SlayerType.INFERNO, "IV", timer = "ASHEN ♨3 02:58", health = "120M", maxHealth = 150_000_000.0),
) {
    private val attunementColors = BooleanSetting("Attunement Colors", true, desc = "Colors the attunement in the timer by its type.")

    init {
        timerPosition.addTextSetting(attunementColors)
    }

    override fun timerText(info: SlayerInfo): LabelSegments {
        val attunement = info.timer?.let(ATTUNEMENT_REGEX::matchEntire) ?: return super.timerText(info)
        val (type, count, time) = attunement.destructured
        return listOf("$type $count" to color(attunementColor(type)), " $time" to theme.textMuted)
    }

    private fun attunementColor(attunement: String): Int {
        if (!attunementColors.enabled) return timerColor.value
        return when (attunement) {
            "ASHEN" -> 0xFF555555.toInt()
            "AURIC" -> 0xFFFFAA00.toInt()
            "CRYSTAL" -> 0xFF55FFFF.toInt()
            "SPIRIT" -> 0xFFFFFFFF.toInt()
            else -> timerColor.value
        }
    }

    private val ATTUNEMENT_REGEX = Regex("""^([A-Z]+) ♨(\d+) (.+)$""")
}
