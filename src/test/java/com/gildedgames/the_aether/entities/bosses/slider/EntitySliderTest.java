package com.gildedgames.the_aether.entities.bosses.slider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.world.World;

import org.junit.Test;

public class EntitySliderTest {

    @Test
    public void singleBlockRegionDoesNotIndexPastTheList() {
        World world = mock(World.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(mock(net.minecraft.block.Block.class));
        when(world.isAirBlock(0, 0, 0)).thenReturn(true);

        assertTrue(EntitySlider.checkIsAir(world, 0, 0, 0, 0, 0, 0));
    }

    @Test
    public void nonAirRegionIsNotAir() {
        World world = mock(World.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(mock(net.minecraft.block.Block.class));
        when(world.isAirBlock(0, 0, 0)).thenReturn(false);

        assertFalse(EntitySlider.checkIsAir(world, 0, 0, 0, 0, 0, 0));
    }
}
