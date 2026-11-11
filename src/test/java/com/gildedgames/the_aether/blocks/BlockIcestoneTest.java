package com.gildedgames.the_aether.blocks;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.junit.Test;

public class BlockIcestoneTest {

    @Test
    public void serverConvertsTheWholeArea() {
        World world = mock(World.class);
        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(Blocks.water);

        new BlockIcestone().onBlockAdded(world, 0, 0, 0);

        verify(world, times(343)).setBlock(anyInt(), anyInt(), anyInt(), eq(Blocks.ice));
    }

    @Test
    public void clientDoesNotMutateTheWorld() {
        World world = mock(World.class);
        world.isRemote = true;

        new BlockIcestone().onBlockAdded(world, 0, 0, 0);

        verify(world, never()).setBlock(anyInt(), anyInt(), anyInt(), eq(Blocks.ice));
        verify(world, never()).setBlock(anyInt(), anyInt(), anyInt(), eq(Blocks.obsidian));
    }
}
