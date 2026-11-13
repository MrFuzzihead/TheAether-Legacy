package com.gildedgames.the_aether.world.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import org.junit.Test;

public class AetherStructureTest {

    private static final Block STONE = mock(Block.class);
    private static final Block DIRT = mock(Block.class);

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    private static final class RecordingStructure extends AetherStructure {

        private final Map<Long, Block> blocks = new HashMap<Long, Block>();
        private final Map<Long, Integer> metadata = new HashMap<Long, Integer>();

        @Override
        public boolean generate() {
            return true;
        }

        @Override
        protected Block getBlockAtCurrentPosition(World world, int x, int y, int z, StructureBoundingBox box) {
            return this.blocks.get(key(x, y, z));
        }

        @Override
        protected void placeBlockAtCurrentPosition(World world, Block block, int meta, int x, int y, int z,
            StructureBoundingBox box) {
            this.blocks.put(key(x, y, z), block);
            this.metadata.put(key(x, y, z), meta);
        }

        Block blockAt(int x, int y, int z) {
            return this.blocks.get(key(x, y, z));
        }

        int metaAt(int x, int y, int z) {
            Integer meta = this.metadata.get(key(x, y, z));
            return meta == null ? 0 : meta;
        }

        int placedCount() {
            return this.blocks.size();
        }

        int nonAirCount() {
            int count = 0;

            for (Block block : this.blocks.values()) {
                if (block != null) {
                    count++;
                }
            }

            return count;
        }
    }

    private static RecordingStructure structure() {
        RecordingStructure structure = new RecordingStructure();
        structure.setStructureOffset(10, 20, 30);
        structure.replaceAir = true;
        structure.replaceSolid = true;
        structure.random = new Random(0L);
        structure.setBlocks(STONE);
        return structure;
    }

    @Test
    public void lineGeometryUsesTheStructureOffset() {
        RecordingStructure structure = structure();

        structure.addLineX(0, 0, 0, 3);
        structure.addLineY(5, 0, 5, 2);
        structure.addLineZ(6, 7, 0, 2);

        assertEquals(STONE, structure.blockAt(10, 20, 30));
        assertEquals(STONE, structure.blockAt(12, 20, 30));
        assertEquals(STONE, structure.blockAt(15, 21, 35));
        assertEquals(STONE, structure.blockAt(16, 27, 31));
        assertEquals(7, structure.placedCount());
    }

    @Test
    public void planeAndBoxGeometryPlaceTheExpectedNumberOfBlocks() {
        RecordingStructure plane = structure();
        plane.addPlaneY(0, 0, 0, 2, 3);
        assertEquals(6, plane.placedCount());

        RecordingStructure box = structure();
        box.addSolidBox(0, 0, 0, 2, 2, 2);
        assertEquals(8, box.placedCount());

        RecordingStructure hollow = structure();
        hollow.setBlocks(STONE, STONE, 1);
        hollow.addHollowBox(0, 0, 0, 3, 3, 3);
        assertEquals(26, hollow.nonAirCount());
    }

    @Test
    public void boxQueriesUseTheRecordedBlocks() {
        RecordingStructure structure = structure();

        assertTrue(structure.isBoxEmpty(0, 0, 0, 2, 2, 2));
        assertFalse(structure.isBoxSolid(0, 0, 0, 2, 2, 2));

        structure.addSolidBox(0, 0, 0, 2, 2, 2);

        assertTrue(structure.isBoxSolid(0, 0, 0, 2, 2, 2));
        assertFalse(structure.isBoxEmpty(0, 0, 0, 2, 2, 2));
    }

    @Test
    public void chanceUsesExtraBlockWhenRollIsZero() {
        RecordingStructure structure = structure();
        Random random = mock(Random.class);
        when(random.nextInt(2)).thenReturn(1, 0);
        structure.random = random;
        structure.setBlocks(STONE, DIRT, 2);

        structure.setBlockWithOffset(0, 0, 0);
        structure.setBlockWithOffset(1, 0, 0);

        assertEquals(STONE, structure.blockAt(10, 20, 30));
        assertEquals(DIRT, structure.blockAt(11, 20, 30));
    }

    @Test
    public void metadataIsRecordedForPlacedBlocks() {
        RecordingStructure structure = structure();
        structure.random = new Random(0L);
        structure.setBlocks(STONE, 4, STONE, 4, 1);

        structure.setBlockWithOffset(2, 3, 4);

        assertEquals(STONE, structure.blockAt(12, 23, 34));
        assertEquals(4, structure.metaAt(12, 23, 34));
    }
}
