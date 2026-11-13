package com.gildedgames.the_aether.world.biome.decoration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.junit.Test;

import com.gildedgames.the_aether.blocks.BlocksAether;

public class AetherGenFloatingIslandTest {

    @Test
    public void stopsAtFirstObstruction() {
        World world = mock(World.class);
        when(world.getHeight()).thenReturn(256);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(mock(Block.class));

        AetherGenFloatingIsland generator = new AetherGenFloatingIsland();

        assertFalse(generator.generate(world, new Random(0L), 0, 64, 0));
        verify(world, times(1)).getBlock(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void generatesIslandWhenTheAreaIsClear() {
        Block oldHolystone = BlocksAether.holystone;
        Block oldGrass = BlocksAether.aether_grass;
        Block oldLog = BlocksAether.skyroot_log;
        Block oldCrystalLeaves = BlocksAether.crystal_leaves;
        Block oldCrystalFruitLeaves = BlocksAether.crystal_fruit_leaves;
        BlocksAether.holystone = mock(Block.class);
        BlocksAether.aether_grass = mock(Block.class);
        BlocksAether.skyroot_log = mock(Block.class);
        BlocksAether.crystal_leaves = mock(Block.class);
        BlocksAether.crystal_fruit_leaves = mock(Block.class);

        try {
            World world = mock(World.class);
            when(world.getHeight()).thenReturn(256);
            when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(Blocks.air);

            assertTrue(new AetherGenFloatingIsland().generate(world, new Random(0L), 0, 64, 0));
            verify(world, atLeastOnce()).setBlock(anyInt(), anyInt(), anyInt(), any(Block.class));
        } finally {
            BlocksAether.holystone = oldHolystone;
            BlocksAether.aether_grass = oldGrass;
            BlocksAether.skyroot_log = oldLog;
            BlocksAether.crystal_leaves = oldCrystalLeaves;
            BlocksAether.crystal_fruit_leaves = oldCrystalFruitLeaves;
        }
    }

    @Test
    public void skipsScanWhenHeightIsTooHigh() {
        World world = mock(World.class);
        when(world.getHeight()).thenReturn(64);

        AetherGenFloatingIsland generator = new AetherGenFloatingIsland();

        assertFalse(generator.generate(world, new Random(0L), 0, 64, 0));
        verify(world, never()).getBlock(anyInt(), anyInt(), anyInt());
    }
}
