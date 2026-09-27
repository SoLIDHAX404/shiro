// Adapted from Nebulune (https://github.com/skies-starred/Nebulune), BSD 3-Clause License.
package org.solidhax.shiro.features.impl.mining.crystalhollows

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.events.LevelEvent
import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.impl.mining.CrystalHollowsMap
import org.solidhax.shiro.utils.logError
import org.solidhax.shiro.utils.skyblock.Island
import org.solidhax.shiro.utils.skyblock.LocationUtils
import java.util.concurrent.Executors

/**
 * Scans loaded Crystal Hollows chunks for structures off the client thread. Results are applied on the client thread,
 * so [found] is only ever touched there.
 */
object StructureScanner {

    class FoundStructure(val structure: Structure, val pos: BlockPos)

    var found: List<FoundStructure> = emptyList()
        private set

    private val structures = LinkedHashMap<Structure, FoundStructure>()
    private val grottoChunks = HashMap<Long, GrottoChunk>()
    private var grottos: List<Grotto> = emptyList()
    private val scannedChunks = HashSet<Long>()

    private var level: ClientLevel? = null
    private var generation = 0

    /**
     * Called on the client thread for every newly found structure.
     */
    var onFound: (FoundStructure) -> Unit = {}

    private val executor = Executors.newSingleThreadExecutor { Thread(it, "Shiro Structure Scanner").apply { isDaemon = true } }

    init {
        on<TickEvent.End> {
            if (CrystalHollowsMap.enabled && CrystalHollowsMap.scanStructures && LocationUtils.isCurrentArea(Island.CrystalHollows)) queueLoadedChunks(level)
        }

        on<LevelEvent.Load> {
            clear()
        }
    }

    /**
     * Queues every loaded chunk around the player that has not been scanned yet.
     */
    private fun queueLoadedChunks(level: ClientLevel) {
        if (level !== this.level) {
            clear()
            this.level = level
        }
        val player = mc.player ?: return
        val centerX = player.blockX shr 4
        val centerZ = player.blockZ shr 4
        val radius = mc.options.renderDistance().get()

        for (chunkX in maxOf(centerX - radius, MIN_CHUNK)..minOf(centerX + radius, MAX_CHUNK)) {
            for (chunkZ in maxOf(centerZ - radius, MIN_CHUNK)..minOf(centerZ + radius, MAX_CHUNK)) {
                val key = ChunkPos.pack(chunkX, chunkZ)
                if (key in scannedChunks) continue
                val chunk = level.chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) ?: continue
                scannedChunks.add(key)
                queue(chunk, chunkX, chunkZ, Structure.scannable.filter { it !in structures })
            }
        }
    }

    private fun clear() {
        generation++
        level = null
        structures.clear()
        grottoChunks.clear()
        grottos = emptyList()
        scannedChunks.clear()
        found = emptyList()
    }

    private fun queue(chunk: LevelChunk, chunkX: Int, chunkZ: Int, remaining: List<Structure>) {
        val queuedGeneration = generation
        executor.execute {
            val result = runCatching { scan(chunk, chunkX, chunkZ, remaining) }.onFailure { logError(it, this) }.getOrNull() ?: return@execute
            mc.execute { if (queuedGeneration == generation) apply(chunkX, chunkZ, result) }
        }
    }

    private fun scan(chunk: LevelChunk, chunkX: Int, chunkZ: Int, remaining: List<Structure>): ChunkScan {
        val pending = remaining.toMutableList()
        val matches = ArrayList<FoundStructure>()
        val cursor = BlockPos.MutableBlockPos()
        var grottoBlocks = 0
        var grottoX = 0L
        var grottoY = 0L
        var grottoZ = 0L

        for (x in 0..15) {
            for (z in 0..15) {
                val worldX = (chunkX shl 4) + x
                val worldZ = (chunkZ shl 4) + z
                for (y in MIN_Y until MAX_Y) {
                    val iterator = pending.iterator()
                    while (iterator.hasNext()) {
                        val structure = iterator.next()
                        if (!structure.area.contains(worldX, y, worldZ)) continue
                        if (structure.pattern?.matches(chunk, x, y, z, cursor) != true) continue
                        matches.add(FoundStructure(structure, BlockPos(worldX, y, worldZ).offset(structure.offset)))
                        iterator.remove()
                    }

                    val state = chunk.getBlockState(cursor.set(x, y, z))
                    if ((state.`is`(Blocks.STAINED_GLASS.magenta()) || state.`is`(Blocks.STAINED_GLASS_PANE.magenta())) && !HollowsArea.NUCLEUS.contains(worldX, y, worldZ)) {
                        grottoBlocks++
                        grottoX += worldX
                        grottoY += y
                        grottoZ += worldZ
                    }
                }
            }
        }

        val grotto = if (grottoBlocks == 0) null else {
            GrottoChunk(BlockPos((grottoX / grottoBlocks).toInt(), (grottoY / grottoBlocks).toInt(), (grottoZ / grottoBlocks).toInt()))
        }
        return ChunkScan(matches, grotto?.takeUnless { HollowsArea.NUCLEUS.contains(it.center) })
    }

    private fun apply(chunkX: Int, chunkZ: Int, result: ChunkScan) {
        val newlyFound = ArrayList<FoundStructure>()
        for (match in result.structures) {
            if (structures.putIfAbsent(match.structure, match) == null) newlyFound.add(match)
        }
        if (result.grotto != null) {
            val key = ChunkPos.pack(chunkX, chunkZ)
            val previousCount = grottos.size
            grottoChunks[key] = result.grotto
            grottos = mergeGrottos()
            if (grottos.size > previousCount) grottos.firstOrNull { key in it.chunks }?.let { newlyFound.add(it.found) }
        }
        found = structures.values + grottos.map { it.found }
        newlyFound.forEach(onFound)
    }

    /**
     * Joins grotto chunks that touch (including diagonally) into one grotto at the average of their centers.
     */
    private fun mergeGrottos(): List<Grotto> {
        val visited = HashSet<Long>()
        val merged = ArrayList<Grotto>()

        for (start in grottoChunks.keys) {
            if (!visited.add(start)) continue
            val chunks = HashSet<Long>()
            val cluster = ArrayList<BlockPos>()
            val queue = ArrayDeque<Long>().apply { add(start) }

            while (queue.isNotEmpty()) {
                val key = queue.removeFirst()
                chunks.add(key)
                cluster.add(grottoChunks.getValue(key).center)
                val pos = ChunkPos.unpack(key)
                for (dx in -1..1) {
                    for (dz in -1..1) {
                        val neighbor = ChunkPos.pack(pos.x() + dx, pos.z() + dz)
                        if (neighbor in grottoChunks && visited.add(neighbor)) queue.add(neighbor)
                    }
                }
            }

            val center = BlockPos(cluster.sumOf { it.x } / cluster.size, cluster.sumOf { it.y } / cluster.size, cluster.sumOf { it.z } / cluster.size)
            merged.add(Grotto(chunks, FoundStructure(Structure.FAIRY_GROTTO, center)))
        }
        return merged
    }

    private class GrottoChunk(val center: BlockPos)

    private class Grotto(val chunks: Set<Long>, val found: FoundStructure)

    private class ChunkScan(val structures: List<FoundStructure>, val grotto: GrottoChunk?)

    private const val MIN_CHUNK = 202 shr 4
    private const val MAX_CHUNK = 823 shr 4
    private const val MIN_Y = 0
    private const val MAX_Y = 170
}
