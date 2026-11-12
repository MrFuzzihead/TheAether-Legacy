package com.gildedgames.the_aether.world.biome.decoration;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.junit.Test;

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
    public void skipsScanWhenHeightIsTooHigh() {
        World world = mock(World.class);
        when(world.getHeight()).thenReturn(64);

        AetherGenFloatingIsland generator = new AetherGenFloatingIsland();

        assertFalse(generator.generate(world, new Random(0L), 0, 64, 0));
        verify(world, never()).getBlock(anyInt(), anyInt(), anyInt());
    }
}
