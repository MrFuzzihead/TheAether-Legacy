package com.gildedgames.the_aether.world.gen.components;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gildedgames.the_aether.blocks.BlocksAether;

public class ComponentGoldenIslandTest {

    private Block air;
    private Block grass;
    private Block dirt;
    private Block holystone;
    private Block lockedAngelicStone;
    private Block goldenOakLog;
    private Block goldenOakLeaves;

    private Block oldAercloud;
    private Block oldGrass;
    private Block oldDirt;
    private Block oldHolystone;
    private Block oldLockedAngelicStone;
    private Block oldGoldenOakLog;
    private Block oldGoldenOakLeaves;

    @Before
    public void installTestBlocks() {
        this.air = mock(Block.class);
        this.grass = mock(Block.class);
        this.dirt = mock(Block.class);
        this.holystone = mock(Block.class);
        this.lockedAngelicStone = mock(Block.class);
        this.goldenOakLog = mock(Block.class);
        this.goldenOakLeaves = mock(Block.class);

        this.oldAercloud = BlocksAether.aercloud;
        this.oldGrass = BlocksAether.aether_grass;
        this.oldDirt = BlocksAether.aether_dirt;
        this.oldHolystone = BlocksAether.holystone;
        this.oldLockedAngelicStone = BlocksAether.locked_angelic_stone;
        this.oldGoldenOakLog = BlocksAether.golden_oak_log;
        this.oldGoldenOakLeaves = BlocksAether.golden_oak_leaves;

        BlocksAether.aercloud = this.air;
        BlocksAether.aether_grass = this.grass;
        BlocksAether.aether_dirt = this.dirt;
        BlocksAether.holystone = this.holystone;
        BlocksAether.locked_angelic_stone = this.lockedAngelicStone;
        BlocksAether.golden_oak_log = this.goldenOakLog;
        BlocksAether.golden_oak_leaves = this.goldenOakLeaves;
    }

    @After
    public void restoreBlocks() {
        BlocksAether.aercloud = this.oldAercloud;
        BlocksAether.aether_grass = this.oldGrass;
        BlocksAether.aether_dirt = this.oldDirt;
        BlocksAether.holystone = this.oldHolystone;
        BlocksAether.locked_angelic_stone = this.oldLockedAngelicStone;
        BlocksAether.golden_oak_log = this.oldGoldenOakLog;
        BlocksAether.golden_oak_leaves = this.oldGoldenOakLeaves;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    private static class RecordingIsland extends ComponentGoldenIsland {

        private final Map<Long, Block> blocks = new HashMap<Long, Block>();

        RecordingIsland() {
            super(0, 0);
        }

        @Override
        public Block getBlockStateWithOffset(int x, int y, int z) {
            Block block = this.blocks.get(key(x, y, z));
            return block == null ? BlocksAether.aercloud : block;
        }

        @Override
        public void setBlockWithOffset(int x, int y, int z, Block state, int meta) {
            this.blocks.put(key(x, y, z), state);
        }

        boolean hasBlock(Block block) {
            return this.blocks.containsValue(block);
        }
    }

    private static class RecordingStub extends ComponentGoldenIslandStub {

        private final Map<Long, Block> blocks = new HashMap<Long, Block>();

        RecordingStub() {
            super(0, 0, 0, 0, 0, 8);
        }

        @Override
        public Block getBlockStateWithOffset(int x, int y, int z) {
            Block block = this.blocks.get(key(x, y, z));
            return block == null ? BlocksAether.aercloud : block;
        }

        @Override
        public void setBlockWithOffset(int x, int y, int z, Block state, int meta) {
            this.blocks.put(key(x, y, z), state);
        }

        boolean hasBlock(Block block) {
            return this.blocks.containsValue(block);
        }
    }

    @Test
    public void mainIslandGeneratesGoldenOakTrees() {
        RecordingIsland island = new RecordingIsland();
        island.random = new Random(0L);

        island.generateBaseIsland();

        assertTrue("expected golden oak logs", island.hasBlock(this.goldenOakLog));
        assertTrue("expected golden oak leaves", island.hasBlock(this.goldenOakLeaves));
    }

    @Test
    public void stubIslandGeneratesGoldenOakTrees() {
        RecordingStub island = new RecordingStub();
        island.random = new Random(0L);

        island.generate();

        assertTrue("expected golden oak logs", island.hasBlock(this.goldenOakLog));
        assertTrue("expected golden oak leaves", island.hasBlock(this.goldenOakLeaves));
    }
}
