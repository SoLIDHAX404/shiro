// Adapted from Nebulune (https://github.com/skies-starred/Nebulune), BSD 3-Clause License.
package org.solidhax.shiro.features.impl.mining.crystalhollows

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.SlabType
import net.minecraft.world.level.chunk.LevelChunk

enum class Structure(
    val displayName: String,
    val color: Int,
    val area: HollowsArea,
    val offset: Vec3i,
    val pattern: StructurePattern?,
) {
    KING("King", AMBER, HollowsArea.GOBLIN_HOLDOUT, Vec3i(1, -1, 2), pattern {
        block(Blocks.WOOL.red())
        block(Blocks.DARK_OAK_STAIRS, 3)
    }),
    QUEEN("Queen", AMBER, HollowsArea.ANYWHERE, Vec3i(0, 5, 0), pattern {
        block(Blocks.STONE)
        block(Blocks.ACACIA_WOOD, 4)
        block(Blocks.CAULDRON)
    }),
    DIVAN("Divan", JADE, HollowsArea.MITHRIL_DEPOSITS, Vec3i(0, 5, 0), pattern {
        block(Blocks.QUARTZ_PILLAR)
        block(Blocks.QUARTZ_STAIRS)
        block(Blocks.STONE_BRICK_STAIRS)
        block(Blocks.CHISELED_STONE_BRICKS)
    }),
    CITY("City", SAPPHIRE, HollowsArea.PRECURSOR_REMNANTS, Vec3i(24, 0, -17), pattern {
        block(Blocks.STONE_BRICKS)
        block(Blocks.COBBLESTONE, 4)
        block(Blocks.COBBLESTONE_STAIRS)
        block(Blocks.POLISHED_ANDESITE, 2)
        block(Blocks.DARK_OAK_STAIRS)
    }),
    TEMPLE("Temple", AMETHYST, HollowsArea.ANYWHERE, Vec3i(-45, 47, -18), pattern {
        block(Blocks.BEDROCK, 4)
        block(Blocks.STONE)
        block(Blocks.CLAY, 3)
        block(Blocks.OAK_LEAVES, 2)
        block(Blocks.DYED_TERRACOTTA.lime(), 2)
        block(Blocks.DYED_TERRACOTTA.green())
    }),
    BAL("Bal", TOPAZ, HollowsArea.MAGMA_FIELDS, Vec3i(0, 1, 0), pattern {
        block(Blocks.LAVA)
        block(Blocks.BARRIER, 10)
    }),
    CORLEONE_DOCK("Corleone Dock", 0xFFFF5555.toInt(), HollowsArea.MITHRIL_DEPOSITS, Vec3i(23, 11, 17), pattern {
        block(Blocks.STONE_BRICKS, 4)
        skip(20)
        block(Blocks.STONE_BRICKS, 2)
        block(Blocks.FIRE)
        block(Blocks.STONE_BRICKS)
    }),
    CORLEONE_HOLE("Corleone Hole", 0xFFFF5555.toInt(), HollowsArea.MITHRIL_DEPOSITS, Vec3i(-18, -1, 29), pattern {
        slab(Blocks.SMOOTH_STONE_SLAB, SlabType.DOUBLE)
        block(Blocks.POLISHED_ANDESITE)
        block(Blocks.STONE_BRICKS)
        block(Blocks.POLISHED_GRANITE)
        block(Blocks.DYED_TERRACOTTA.lightGray(), 18)
        block(Blocks.JUNGLE_STAIRS)
        block(Blocks.GRANITE)
        block(Blocks.POLISHED_GRANITE)
        block(Blocks.STONE_BRICKS, 2)
    }),
    KEY_GUARDIAN_SPIRAL("Key Guardian Spiral", 0xFFFF55FF.toInt(), HollowsArea.JUNGLE, Vec3i(0, 0, 0), pattern {
        block(Blocks.JUNGLE_STAIRS)
        block(Blocks.JUNGLE_PLANKS)
        block(Blocks.GLOWSTONE)
        block(Blocks.CARPET.brown())
        skip()
        block(Blocks.JUNGLE_SLAB)
        skip()
        block(Blocks.JUNGLE_STAIRS)
        block(Blocks.STONE, 3)
    }),
    KEY_GUARDIAN_TOWER("Key Guardian Tower", 0xFFFF55FF.toInt(), HollowsArea.JUNGLE, Vec3i(0, 0, 0), pattern {
        block(Blocks.STONE)
        block(Blocks.POLISHED_GRANITE)
        repeat(3) {
            slab(Blocks.JUNGLE_SLAB, SlabType.TOP)
            skip()
        }
        slab(Blocks.JUNGLE_SLAB, SlabType.TOP, 2)
        block(Blocks.JUNGLE_PLANKS)
    }),
    XALX("Xalx", 0xFFAAAAAA.toInt(), HollowsArea.GOBLIN_HOLDOUT, Vec3i(-2, 1, -2), pattern {
        block(Blocks.STONE)
        block(Blocks.COAL_BLOCK)
        block(Blocks.FIRE)
        block(Blocks.NETHER_QUARTZ_ORE)
        block(Blocks.AIR, 7)
    }),
    PETE("Pete (3 bears)", 0xFFC98B4F.toInt(), HollowsArea.GOBLIN_HOLDOUT, Vec3i(0, 0, 0), pattern {
        block(Blocks.NETHERRACK)
        block(Blocks.FIRE)
        block(Blocks.IRON_BARS)
        block(Blocks.AIR, 7)
    }),
    ODAWA("Odawa", 0xFF55AA55.toInt(), HollowsArea.JUNGLE, Vec3i(0, 0, 0), pattern {
        block(Blocks.JUNGLE_LOG)
        block(Blocks.SPRUCE_STAIRS, 2)
        block(Blocks.JUNGLE_LOG)
        block(Blocks.SPRUCE_STAIRS, 2)
        block(Blocks.JUNGLE_LOG, 3)
        block(Blocks.HAY_BLOCK)
        block(Blocks.DYED_TERRACOTTA.yellow())
    }),
    GOLDEN_DRAGON("Golden Dragon", 0xFFFFD700.toInt(), HollowsArea.ANYWHERE, Vec3i(0, -3, 5), pattern {
        block(Blocks.STONE)
        block(Blocks.DYED_TERRACOTTA.red(), 3)
        block(Blocks.PLAYER_HEAD)
        block(Blocks.WOOL.red())
    }),
    WORM_FISHING("Worm Fishing", 0xFFFF8C2E.toInt(), HollowsArea.UPPER_PRECURSOR_REMNANTS, Vec3i(0, 0, 0), pattern {
        block(Blocks.LAVA)
        block(Blocks.AIR)
    }),
    FAIRY_GROTTO("Fairy Grotto", 0xFFFF77DD.toInt(), HollowsArea.ANYWHERE, Vec3i(0, 0, 0), null);

    companion object {
        val scannable: List<Structure> = entries.filter { it.pattern != null }
    }
}

enum class HollowsArea(private val predicate: (x: Int, y: Int, z: Int) -> Boolean) {
    ANYWHERE({ _, _, _ -> true }),
    NUCLEUS({ x, _, z -> x in 449..576 && z in 449..576 }),
    JUNGLE({ x, _, z -> x <= 576 && z <= 576 }),
    MITHRIL_DEPOSITS({ x, _, z -> x > 448 && z <= 576 }),
    GOBLIN_HOLDOUT({ x, _, z -> x <= 576 && z > 448 }),
    PRECURSOR_REMNANTS({ x, _, z -> x > 448 && z > 448 }),
    UPPER_PRECURSOR_REMNANTS({ x, y, z -> x >= 513 && y >= 80 && z >= 513 }),
    MAGMA_FIELDS({ _, y, _ -> y < 80 });

    fun contains(x: Int, y: Int, z: Int): Boolean = predicate(x, y, z)

    fun contains(pos: BlockPos): Boolean = predicate(pos.x, pos.y, pos.z)
}

class StructurePattern(private val layers: List<BlockMatcher?>) {

    /**
     * Whether the column starting at the chunk-local ([x], [y], [z]) matches, one layer per block going up.
     */
    fun matches(chunk: LevelChunk, x: Int, y: Int, z: Int, cursor: BlockPos.MutableBlockPos): Boolean {
        for (index in layers.indices) {
            val matcher = layers[index] ?: continue
            if (!matcher.matches(chunk.getBlockState(cursor.set(x, y + index, z)))) return false
        }
        return true
    }

    class Builder {
        private val layers = ArrayList<BlockMatcher?>()

        fun block(block: Block, count: Int = 1) = repeat(count) { layers.add(BlockMatcher(block)) }

        fun slab(block: Block, type: SlabType, count: Int = 1) = repeat(count) { layers.add(BlockMatcher(block, type)) }

        fun skip(count: Int = 1) = repeat(count) { layers.add(null) }

        fun build(): StructurePattern = StructurePattern(layers)
    }
}

class BlockMatcher(private val block: Block, private val slabType: SlabType? = null) {

    fun matches(state: BlockState): Boolean {
        if (!state.`is`(block)) return false
        return slabType == null || (state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == slabType)
    }
}

private fun pattern(builder: StructurePattern.Builder.() -> Unit): StructurePattern =
    StructurePattern.Builder().apply(builder).build()

private const val AMBER = 0xFFFFAA00.toInt()
private const val JADE = 0xFF55FF55.toInt()
private const val SAPPHIRE = 0xFF55FFFF.toInt()
private const val AMETHYST = 0xFFB34DFF.toInt()
private const val TOPAZ = 0xFFFFFF55.toInt()
