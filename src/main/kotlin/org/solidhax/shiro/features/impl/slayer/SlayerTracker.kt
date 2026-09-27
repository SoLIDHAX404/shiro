package org.solidhax.shiro.features.impl.slayer

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.events.LevelEvent
import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.utils.strippedName

class SlayerInfo(
    val type: SlayerType,
    val tier: String?,
    var timer: String? = null,
    var hits: Int? = null,
    var health: String? = null,
    var vehicleTicks: Int? = null,
    var killTime: Double? = null,
    var maxHealth: Double? = null,
) {
    val healthFraction: Float?
        get() {
            val current = parseHealth(health ?: return null) ?: return null
            val max = maxHealth?.takeIf { it > 0.0 } ?: return null
            return (current / max).toFloat().coerceIn(0f, 1f)
        }
}

fun parseHealth(text: String): Double? {
    val number = text.trimEnd('k', 'M', 'B').replace(",", "").toDoubleOrNull() ?: return null
    return number * when (text.lastOrNull()) {
        'k' -> 1_000.0
        'M' -> 1_000_000.0
        'B' -> 1_000_000_000.0
        else -> 1.0
    }
}

class Slayer(val entity: LivingEntity, val owned: Boolean, val info: SlayerInfo) {

    val type: SlayerType get() = info.type

    val isDead: Boolean get() = info.killTime != null

    internal var deadTicks = 0

    internal val standIds: IntRange get() = entity.id + 1..entity.id + 3

    internal fun update(level: ClientLevel) {
        val name = level.getEntity(entity.id + 1)?.strippedName
        if (name != null) {
            info.hits = HITS_REGEX.find(name)?.groupValues?.get(1)?.toIntOrNull()
            HEALTH_REGEX.find(name)?.let { match ->
                val health = match.groupValues[1]
                info.health = health
                val value = parseHealth(health) ?: return@let
                if (value > (info.maxHealth ?: 0.0)) info.maxHealth = value
            }
        }

        val timer = level.getEntity(entity.id + 2)?.strippedName?.trim()
        if (timer != null && ':' in timer && !timer.startsWith(OWNER_PREFIX)) info.timer = timer

        info.vehicleTicks = entity.vehicle?.tickCount
    }

    private companion object {
        val HITS_REGEX = Regex("""(\d+) Hits""")
        val HEALTH_REGEX = Regex("""([\d.,]+[kMB]?)❤""")
    }
}

object SlayerTracker {

    private val slayers = LinkedHashMap<LivingEntity, Slayer>()
    private val stands = HashMap<Int, Slayer>()

    val tracked: Collection<Slayer> get() = slayers.values

    operator fun get(entity: Entity): Slayer? = slayers[entity]

    fun ofStand(stand: Entity): Slayer? = stands[stand.id]

    init {
        on<TickEvent.End> {
            if (ModuleManager.modules.values.none { it is SlayerModule && it.enabled }) {
                clear()
                return@on
            }

            val playerName = mc.player?.name?.string
            for (entity in level.entitiesForRendering()) {
                if (entity is ArmorStand) track(level, entity, playerName)
            }

            stands.clear()
            val iterator = slayers.values.iterator()
            while (iterator.hasNext()) {
                val slayer = iterator.next()
                if (slayer.isDead) {
                    if (++slayer.deadTicks > KILL_TIME_TICKS) iterator.remove()
                    continue
                }
                if (slayer.entity.isDeadOrDying) {
                    slayer.info.killTime = slayer.entity.tickCount / 20.0
                    continue
                }
                if (slayer.entity.isRemoved) {
                    iterator.remove()
                    continue
                }
                slayer.update(level)
                for (id in slayer.standIds) stands[id] = slayer
            }
        }

        on<LevelEvent.Load> {
            clear()
        }
    }

    private fun clear() {
        slayers.clear()
        stands.clear()
    }

    private fun track(level: ClientLevel, nameStand: ArmorStand, playerName: String?) {
        val name = nameStand.strippedName ?: return
        if (!name.startsWith("☠") && !name.endsWith("❤") && !name.endsWith("❤ ✯") && !name.endsWith(" Hits")) return
        val type = SlayerType.fromName(name) ?: return

        val entity = level.getEntity(nameStand.id - 1) as? LivingEntity ?: return
        if (entity is ArmorStand || entity in slayers || !entity.isAlive) return

        val owner = level.getEntity(nameStand.id + 2)?.strippedName ?: return
        if (!owner.startsWith(OWNER_PREFIX)) return

        slayers[entity] = Slayer(entity, owner.removePrefix(OWNER_PREFIX).trim() == playerName, SlayerInfo(type, type.tierOf(name)))
    }

    private const val KILL_TIME_TICKS = 100
}

private const val OWNER_PREFIX = "Spawned by:"
