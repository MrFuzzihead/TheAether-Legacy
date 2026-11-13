package com.gildedgames.the_aether.world.biome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenerator;

import org.junit.Test;

import com.gildedgames.the_aether.blocks.BlocksAether;
import com.gildedgames.the_aether.world.biome.decoration.AetherGenOakTree;
import com.gildedgames.the_aether.world.biome.decoration.AetherGenSkyrootTree;

public class AetherBiomeDecoratorTest {

    private static final class ThrowingDecorator extends AetherBiomeDecorator {

        @Override
        protected void genDecorations(BiomeGenBase biome) {
            throw new IllegalStateException("decoration failed");
        }
    }

    @Test
    public void failedDecorationReleasesTheDecoratorState() {
        AetherBiomeDecorator decorator = new ThrowingDecorator();

        try {
            decorator.decorateChunk(mock(World.class), new Random(0L), null, 0, 0);
            fail("expected decoration failure");
        } catch (IllegalStateException expected) {
            // expected
        }

        assertNull(decorator.world);
        assertNull(decorator.rand);
    }

    @Test
    public void goldenIslandSurfaceRequiresMetaThree() {
        Block oldGrass = BlocksAether.aether_grass;
        Block grass = mock(Block.class);
        Block other = mock(Block.class);
        BlocksAether.aether_grass = grass;

        try {
            World world = mock(World.class);
            AetherBiomeDecorator decorator = new AetherBiomeDecorator();
            decorator.world = world;

            when(world.getBlock(1, 2, 3)).thenReturn(grass);
            when(world.getBlockMetadata(1, 2, 3)).thenReturn(3);
            assertTrue(decorator.isGoldenIslandSurface(1, 3, 3));

            when(world.getBlockMetadata(1, 2, 3)).thenReturn(0);
            assertFalse(decorator.isGoldenIslandSurface(1, 3, 3));

            when(world.getBlock(1, 2, 3)).thenReturn(other);
            when(world.getBlockMetadata(1, 2, 3)).thenReturn(3);
            assertFalse(decorator.isGoldenIslandSurface(1, 3, 3));
        } finally {
            BlocksAether.aether_grass = oldGrass;
        }
    }

    @Test
    public void getTreeReusesGoldenOakGenerator() {
        AetherBiomeDecorator decorator = new AetherBiomeDecorator();
        Random random = mock(Random.class);
        when(random.nextInt(30)).thenReturn(0);
        decorator.rand = random;

        WorldGenerator first = decorator.getTree();

        assertTrue(first instanceof AetherGenOakTree);
        assertSame(first, decorator.getTree());
    }

    @Test
    public void getTreeReusesSkyrootGenerator() {
        AetherBiomeDecorator decorator = new AetherBiomeDecorator();
        Random random = mock(Random.class);
        when(random.nextInt(30)).thenReturn(1);
        decorator.rand = random;

        WorldGenerator first = decorator.getTree();

        assertTrue(first instanceof AetherGenSkyrootTree);
        assertSame(first, decorator.getTree());
    }
}
