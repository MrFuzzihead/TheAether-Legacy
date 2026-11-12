package com.gildedgames.the_aether;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AetherConfigTest {

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
