package org.solidhax.shiro.features.impl.slayer

import foo.starred.cascade.graphics.geometry.CascadeGeometricColor
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import org.solidhax.shiro.events.EntityGlowEvent
import org.solidhax.shiro.events.EntityRenderEvent
import org.solidhax.shiro.events.HudRenderEvent
import org.solidhax.shiro.events.LevelEvent
import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.gui.DummyEntity
import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.PreviewLabel
import org.solidhax.shiro.gui.settings.Setting.Companion.withDependency
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ColorSetting
import org.solidhax.shiro.gui.settings.impl.LabelPositionSetting
import org.solidhax.shiro.gui.settings.impl.PreviewSetting
import org.solidhax.shiro.utils.strippedName
import org.solidhax.shiro.utils.ui.LabelPosition
import org.solidhax.shiro.utils.ui.LabelSegments
import org.solidhax.shiro.utils.ui.entityLabels
import java.util.Locale

object SlayerInfo : Module(
    name = "Slayer Info",
    description = "Shows information about slayer bosses in cleaner labels."
) {
    private val highlight by BooleanSetting("Highlight", false, desc = "Highlights slayer bosses.")
    private val highlightColor by ColorSetting("Highlight Color", 0xFFFF5555.toInt(), desc = "Color used to highlight slayer bosses.").withDependency { highlight }
    private val hideOriginal by BooleanSetting("Hide Original", true, desc = "Hides the boss's own name tags.")
    private val onlyOwnBoss by BooleanSetting("Only Own Boss", false, desc = "Only shows info for bosses you spawned.")
    private val showTimer by BooleanSetting("Show Timer", true, desc = "Shows the time left to kill the boss, the Voidgloom laser timer and the Inferno attunement.")
    private val showName by BooleanSetting("Show Name", true, desc = "Shows the boss's name and tier.")
    private val shortNames by BooleanSetting("Short Names", true, desc = "Uses short boss names like Void instead of Voidgloom Seraph.").withDependency { showName }
    private val showHealth by BooleanSetting("Show Health", true, desc = "Shows the boss's health, and the hits left while its shield is up.")
    private val showKillTime by BooleanSetting("Show Kill Time", true, desc = "Shows how long the boss took to kill after it dies.")
    private val attunementColors by BooleanSetting("Attunement Colors", true, desc = "Colors the Inferno Demonlord's attunement by its type.").withDependency { showTimer }
    private val timerColor by ColorSetting("Timer Color", 0xFF55FFFF.toInt(), desc = "Color of the timer.").withDependency { showTimer }
    private val nameColor by ColorSetting("Name Color", 0xFFAA0000.toInt(), desc = "Color of the boss's name.").withDependency { showName }
    private val healthColor by ColorSetting("Health Color", 0xFF55FFFF.toInt(), desc = "Color of the boss's health and hits.").withDependency { showHealth }
    private val killTimeColor by ColorSetting("Kill Time Color", 0xFFFF5555.toInt(), desc = "Color of the kill time.").withDependency { showKillTime }

    private val timerPosition = +LabelPositionSetting("Timer Label Position", desc = "Where the timer label sits. Drag it in the preview to move it.")
    private val namePosition = +LabelPositionSetting("Name Label Position", desc = "Where the name label sits, and the kill time once the boss dies. Drag it in the preview to move it.")
    private val healthPosition = +LabelPositionSetting("Health Label Position", desc = "Where the health label sits. Drag it in the preview to move it.")

    private val preview = +PreviewSetting(
        "Preview",
        bossPreview(EntityTypes.ZOMBIE, BossInfo(SlayerBoss.REVENANT, "V", timer = "02:31", health = "8.4M")),
        bossPreview(EntityTypes.SPIDER, BossInfo(SlayerBoss.TARANTULA, "IV", timer = "03:12", health = "1.5M")),
        bossPreview(EntityTypes.ENDERMAN, BossInfo(SlayerBoss.VOIDGLOOM, "IV", timer = "02:47", hits = 15, health = "45.2M")),
        bossPreview(EntityTypes.BLAZE, BossInfo(SlayerBoss.INFERNO, "IV", timer = "ASHEN ♨3 02:58", health = "120M")),
    )

    private val bosses = LinkedHashMap<LivingEntity, Boss>()
    private val hiddenStands = HashSet<Int>()

    init {
        on<TickEvent.End> {
            val playerName = mc.player?.name?.string
            for (entity in level.entitiesForRendering()) {
                if (entity is ArmorStand) track(level, entity, playerName)
            }

            hiddenStands.clear()
            val iterator = bosses.values.iterator()
            while (iterator.hasNext()) {
                val boss = iterator.next()
                if (boss.info.killTime != null) {
                    if (++boss.deadTicks > KILL_TIME_TICKS) iterator.remove()
                    continue
                }
                if (boss.entity.isDeadOrDying) {
                    boss.info.killTime = boss.entity.tickCount / 20.0
                    continue
                }
                if (boss.entity.isRemoved) {
                    iterator.remove()
                    continue
                }
                boss.update(level)
                if (isShown(boss)) hiddenStands.addAll(boss.standIds)
            }
        }

        on<EntityGlowEvent> {
            if (!highlight) return@on
            val boss = bosses[entity] ?: return@on
            if (boss.info.killTime == null && isShown(boss)) color = highlightColor
        }

        on<EntityRenderEvent> {
            if (hideOriginal && entity is ArmorStand && entity.id in hiddenStands) cancel()
        }

        on<HudRenderEvent> {
            for (boss in bosses.values) {
                if (!isShown(boss) || (boss.info.killTime != null && !showKillTime)) continue
                graphics.entityLabels(boss.entity, partialTick, labels(boss.info))
            }
        }

        on<LevelEvent.Load> {
            clear()
        }
    }

    override fun onDisable() {
        super.onDisable()
        clear()
    }

    private fun clear() {
        bosses.clear()
        hiddenStands.clear()
    }

    private fun bossPreview(type: EntityType<*>, info: BossInfo) =
        EntityPreview(
            DummyEntity(type),
            info.type.displayName,
            listOf(
                PreviewLabel(healthPosition) { healthSegments(info) },
                PreviewLabel(namePosition) { nameSegments(info) },
                PreviewLabel(timerPosition) { timerSegments(info) },
            ),
        )

    private fun isShown(boss: Boss): Boolean = !onlyOwnBoss || boss.owned

    private fun track(level: ClientLevel, nameStand: ArmorStand, playerName: String?) {
        val name = nameStand.strippedName ?: return
        if (!name.startsWith("☠") && !name.endsWith("❤") && !name.endsWith("❤ ✯") && !name.endsWith(" Hits")) return
        val type = SlayerBoss.fromName(name) ?: return

        val entity = level.getEntity(nameStand.id - 1) as? LivingEntity ?: return
        if (entity is ArmorStand || entity in bosses || !entity.isAlive) return

        val owner = level.getEntity(nameStand.id + 2)?.strippedName ?: return
        if (!owner.startsWith(OWNER_PREFIX)) return

        bosses[entity] = Boss(entity, owner.removePrefix(OWNER_PREFIX).trim() == playerName, BossInfo(type, type.tierOf(name)))
    }

    private fun labels(boss: BossInfo): List<Pair<LabelPosition, LabelSegments>> {
        boss.killTime?.let { return listOf(namePosition.value to listOf(formatDuration(it) to color(killTimeColor))) }
        return listOf(
            healthPosition.value to healthSegments(boss),
            namePosition.value to nameSegments(boss),
            timerPosition.value to timerSegments(boss),
        )
    }

    private fun timerSegments(boss: BossInfo): LabelSegments {
        val timer = boss.timer?.takeIf { showTimer } ?: return emptyList()
        val attunement = ATTUNEMENT_REGEX.matchEntire(timer).takeIf { boss.type == SlayerBoss.INFERNO }
        if (attunement != null) {
            val (type, count, time) = attunement.destructured
            return listOf("$type $count" to color(attunementColor(type)), " $time" to theme.textMuted)
        }
        val laser = boss.laser ?: return listOf(timer to color(timerColor))
        val laserText = if (laser <= 0.0) "Soon" else String.format(Locale.ROOT, "%.1fs", laser)
        return listOf(laserText to color(timerColor), " $timer" to theme.textMuted)
    }

    private fun nameSegments(boss: BossInfo): LabelSegments {
        if (!showName) return emptyList()
        val name = if (shortNames) boss.type.shortName else boss.type.displayName
        return listOf("$name ${boss.tier ?: "???"}" to color(nameColor))
    }

    private fun healthSegments(boss: BossInfo): LabelSegments {
        if (!showHealth) return emptyList()
        val hits = boss.hits ?: return boss.health?.let { listOf(it to color(healthColor)) } ?: emptyList()
        return buildList {
            add("$hits Hits" to color(healthColor))
            boss.health?.let { add(" $it" to theme.textMuted) }
        }
    }

    private fun attunementColor(attunement: String): Int {
        if (!attunementColors) return timerColor
        return when (attunement) {
            "ASHEN" -> 0xFF555555.toInt()
            "AURIC" -> 0xFFFFAA00.toInt()
            "CRYSTAL" -> 0xFF55FFFF.toInt()
            "SPIRIT" -> 0xFFFFFFFF.toInt()
            else -> timerColor
        }
    }

    private fun color(color: Int) = CascadeGeometricColor(color)

    private fun formatDuration(seconds: Double): String {
        val minutes = (seconds / 60.0).toInt()
        val rest = String.format(Locale.ROOT, "%.1fs", seconds - minutes * 60)
        return if (minutes > 0) "${minutes}m $rest" else rest
    }

    private class BossInfo(
        val type: SlayerBoss,
        val tier: String?,
        var timer: String? = null,
        var hits: Int? = null,
        var health: String? = null,
        var laser: Double? = null,
        var killTime: Double? = null,
    )

    private class Boss(val entity: LivingEntity, val owned: Boolean, val info: BossInfo) {
        var deadTicks = 0

        val standIds: IntRange get() = entity.id + 1..entity.id + 3

        fun update(level: ClientLevel) {
            val name = level.getEntity(entity.id + 1)?.strippedName
            if (name != null) {
                info.hits = HITS_REGEX.find(name)?.groupValues?.get(1)?.toIntOrNull()
                HEALTH_REGEX.find(name)?.let { info.health = it.groupValues[1] }
            }

            val timer = level.getEntity(entity.id + 2)?.strippedName?.trim()
            if (timer != null && ':' in timer && !timer.startsWith(OWNER_PREFIX)) info.timer = timer

            val vehicleTicks = entity.vehicle?.tickCount.takeIf { info.type == SlayerBoss.VOIDGLOOM }
            info.laser = vehicleTicks?.let { (LASER_SECONDS - it / 20.0).coerceAtLeast(0.0) }
        }
    }

    private enum class SlayerBoss(val displayName: String, val shortName: String, val names: List<String> = listOf(displayName)) {
        REVENANT("Revenant Horror", "Rev", listOf("Revenant Horror", "Atoned Horror")),
        TARANTULA("Tarantula Broodfather", "Tara", listOf("Tarantula Broodfather", "Conjoined Brood")),
        SVEN("Sven Packmaster", "Sven"),
        VOIDGLOOM("Voidgloom Seraph", "Void"),
        INFERNO("Inferno Demonlord", "Blaze"),
        VAMPIRE("Bloodfiend", "Vamp");

        fun tierOf(name: String): String? {
            if (names.drop(1).any { it in name }) return "V"
            return TIER_REGEX.find(name)?.groupValues?.get(1)
        }

        companion object {
            fun fromName(name: String): SlayerBoss? = entries.find { boss -> boss.names.any { it in name } }
        }
    }

    private const val OWNER_PREFIX = "Spawned by:"
    private const val KILL_TIME_TICKS = 100
    private const val LASER_SECONDS = 8.2

    private val TIER_REGEX = Regex("""(?:^|\s)([IVX]{1,4})\s""")
    private val HITS_REGEX = Regex("""(\d+) Hits""")
    private val HEALTH_REGEX = Regex("""([\d.,]+[kMB]?)❤""")
    private val ATTUNEMENT_REGEX = Regex("""^([A-Z]+) ♨(\d+) (.+)$""")
}
