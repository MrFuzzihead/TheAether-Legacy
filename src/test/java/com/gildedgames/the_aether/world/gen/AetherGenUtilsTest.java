package com.gildedgames.the_aether.world.gen;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.Test;

import com.gildedgames.the_aether.blocks.BlocksAether;

public class AetherGenUtilsTest {

    private static final class AirStructure extends AetherStructure {

        private final Block air;
        private final Block grass;
        private int placed;

        private AirStructure(Block air, Block grass) {
            this.air = air;
            this.grass = grass;
        }

        @Override
        public Block getBlockStateWithOffset(int x, int y, int z) {
            return y < 0 ? this.grass : this.air;
        }

        @Override
        public void setBlockWithOffset(int x, int y, int z, Block state, int meta) {
            this.placed++;
        }

        @Override
        public boolean generate() {
            return true;
        }
    }

    @Test
    public void flowerGenerationPlacesOnAirAboveGrass() {
        Block oldGrass = BlocksAether.aether_grass;
        Block air = mock(Block.class);
        Block grass = mock(Block.class);
        when(air.getMaterial()).thenReturn(Material.air);
        BlocksAether.aether_grass = grass;

        try {
            AirStructure structure = new AirStructure(air, grass);
            structure.random = new Random(0L);

            AetherGenUtils.generateFlower(structure, mock(Block.class), 0, 0, 0, 0);

            assertTrue(structure.placed > 0);
        } finally {
            BlocksAether.aether_grass = oldGrass;
        }
    }
}
