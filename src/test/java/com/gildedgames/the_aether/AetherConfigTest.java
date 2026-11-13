package com.gildedgames.the_aether;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class AetherConfigTest {

    @Test
    public void configValidationClampsSpawnRates() {
        assertEquals(0, AetherConfig.validateSpawnRate("Test", 0));
        assertEquals(0, AetherConfig.validateSpawnRate("Test", -5));
        assertEquals(25, AetherConfig.validateSpawnRate("Test", 25));
    }

    @Test
    public void zeroSpawnRateDisablesSpawning() {
        Random random = new Random(0L);

        assertFalse(AetherConfig.passesSpawnRate(random, 0));
        assertTrue(AetherConfig.passesSpawnRate(random, 1));
    }

    @Test
    public void configValidationKeepsIdsInRange() {
        assertEquals(4, AetherConfig.validateDimensionId(1));
        assertEquals(5, AetherConfig.validateDimensionId(5));
        assertEquals(127, AetherConfig.validateBiomeId(-1));
        assertEquals(127, AetherConfig.validateBiomeId(256));
        assertEquals(20, AetherConfig.validateBiomeId(20));
        assertEquals(31, AetherConfig.validatePotionId(32));
        assertEquals(3, AetherConfig.validatePotionId(3));
        assertEquals(10, AetherConfig.validateMaxLifeShards(0));
        assertEquals(15, AetherConfig.validateMaxLifeShards(15));
    }

    @Test
    public void menuToggleUpdatesCachedValue() {
        boolean oldValue = AetherConfig.menuEnabled();

        try {
            AetherConfig.setMenuEnabled(!oldValue);
            assertEquals(!oldValue, AetherConfig.menuEnabled());
        } finally {
            AetherConfig.setMenuEnabled(oldValue);
        }
    }
}
