package com.gildedgames.the_aether.world.biome;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

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
}
