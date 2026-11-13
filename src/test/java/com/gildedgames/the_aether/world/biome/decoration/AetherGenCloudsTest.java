package com.gildedgames.the_aether.world.biome.decoration;

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

import org.junit.Test;

import com.gildedgames.the_aether.blocks.BlocksAether;

public class AetherGenCloudsTest {

    @Test
    public void generatesCloudBlocksWithTheConfiguredMetadata() {
        Block oldCloud = BlocksAether.aercloud;
        BlocksAether.aercloud = mock(Block.class);

        try {
            World world = mock(World.class);
            when(world.isAirBlock(anyInt(), anyInt(), anyInt())).thenReturn(true);

            AetherGenClouds generator = new AetherGenClouds();
            generator.setCloudAmount(2);
            generator.setCloudMeta(2);

            assertTrue(generator.generate(world, new Random(0L), 0, 64, 0));
            verify(world, atLeastOnce()).setBlock(anyInt(), anyInt(), anyInt(), any(Block.class), eq(2), anyInt());
        } finally {
            BlocksAether.aercloud = oldCloud;
        }
    }
}
