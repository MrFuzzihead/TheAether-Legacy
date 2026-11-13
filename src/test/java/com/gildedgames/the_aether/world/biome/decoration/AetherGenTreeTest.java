package com.gildedgames.the_aether.world.biome.decoration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gildedgames.the_aether.blocks.BlocksAether;

public class AetherGenTreeTest {

    private Block oldGrass;
    private Block oldDirt;
    private Block oldSkyrootLeaves;
    private Block oldSkyrootLog;
    private Block oldGoldenLeaves;
    private Block oldGoldenLog;

    private Block air;
    private Block grass;
    private Block dirt;
    private Block skyrootLeaves;
    private Block skyrootLog;
    private Block goldenLeaves;
    private Block goldenLog;

    @Before
    public void installBlocks() {
        this.oldGrass = BlocksAether.aether_grass;
        this.oldDirt = BlocksAether.aether_dirt;
        this.oldSkyrootLeaves = BlocksAether.skyroot_leaves;
        this.oldSkyrootLog = BlocksAether.skyroot_log;
        this.oldGoldenLeaves = BlocksAether.golden_oak_leaves;
        this.oldGoldenLog = BlocksAether.golden_oak_log;

        this.air = mock(Block.class);
        this.grass = mock(Block.class);
        this.dirt = mock(Block.class);
        this.skyrootLeaves = mock(Block.class);
        this.skyrootLog = mock(Block.class);
        this.goldenLeaves = mock(Block.class);
        this.goldenLog = mock(Block.class);

        when(this.air.isOpaqueCube()).thenReturn(false);

        BlocksAether.aether_grass = this.grass;
        BlocksAether.aether_dirt = this.dirt;
        BlocksAether.skyroot_leaves = this.skyrootLeaves;
        BlocksAether.skyroot_log = this.skyrootLog;
        BlocksAether.golden_oak_leaves = this.goldenLeaves;
        BlocksAether.golden_oak_log = this.goldenLog;
    }

    @After
    public void restoreBlocks() {
        BlocksAether.aether_grass = this.oldGrass;
        BlocksAether.aether_dirt = this.oldDirt;
        BlocksAether.skyroot_leaves = this.oldSkyrootLeaves;
        BlocksAether.skyroot_log = this.oldSkyrootLog;
        BlocksAether.golden_oak_leaves = this.oldGoldenLeaves;
        BlocksAether.golden_oak_log = this.oldGoldenLog;
    }

    @Test
    public void skyrootTreeRequiresGrassOrDirt() {
        World world = mock(World.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(mock(Block.class));

        assertFalse(new AetherGenSkyrootTree(true).generate(world, new Random(0L), 0, 64, 0));
    }

    @Test
    public void skyrootTreePlacesBlocksOnGrass() {
        World world = mock(World.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(this.air);
        when(world.getBlock(0, 63, 0)).thenReturn(this.grass);

        assertTrue(new AetherGenSkyrootTree(true).generate(world, new Random(0L), 0, 64, 0));
        verify(world, atLeastOnce()).setBlock(anyInt(), anyInt(), anyInt(), any(Block.class), eq(0), anyInt());
    }

    @Test
    public void goldenOakRequiresGrassOrDirt() {
        World world = mock(World.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(mock(Block.class));

        assertFalse(new AetherGenOakTree().generate(world, new Random(0L), 0, 64, 0));
    }

    @Test
    public void goldenOakPlacesLogsAndLeavesOnGrass() {
        World world = mock(World.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(this.air);
        when(world.getBlock(0, 63, 0)).thenReturn(this.grass);
        when(world.isAirBlock(anyInt(), anyInt(), anyInt())).thenReturn(true);

        assertTrue(new AetherGenOakTree().generate(world, new Random(0L), 0, 64, 0));
        verify(world, atLeastOnce()).setBlock(anyInt(), anyInt(), anyInt(), any(Block.class));
    }
}
