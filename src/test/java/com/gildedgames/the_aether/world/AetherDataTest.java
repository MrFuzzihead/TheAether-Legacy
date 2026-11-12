package com.gildedgames.the_aether.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class AetherDataTest {

    private static final String KEY = "aether_legacy";

    @Test
    public void nbtLoadPopulatesCachedFields() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("EternalDay", true);
        data.setBoolean("ShouldCycleCatchup", true);
        data.setLong("Time", 1234L);
        root.setTag(KEY, data);

        AetherData aetherData = new AetherData(KEY);
        aetherData.readFromNBT(root);

        assertTrue(aetherData.isEternalDay());
        assertTrue(aetherData.isShouldCycleCatchup());
        assertEquals(1234L, aetherData.getAetherTime());
    }

    @Test
    public void settersUpdateCachedFieldsAndAreWrittenBack() {
        AetherData aetherData = new AetherData(KEY);
        aetherData.setEternalDay(true);
        aetherData.setShouldCycleCatchup(true);
        aetherData.setAetherTime(4321L);

        assertTrue(aetherData.isEternalDay());
        assertTrue(aetherData.isShouldCycleCatchup());
        assertEquals(4321L, aetherData.getAetherTime());

        NBTTagCompound root = new NBTTagCompound();
        aetherData.writeToNBT(root);
        NBTTagCompound data = root.getCompoundTag(KEY);

        assertTrue(data.getBoolean("EternalDay"));
        assertTrue(data.getBoolean("ShouldCycleCatchup"));
        assertEquals(4321L, data.getLong("Time"));
    }

    @Test
    public void missingStateDefaultsToFalseAndZero() {
        AetherData aetherData = new AetherData(KEY);
        aetherData.readFromNBT(new NBTTagCompound());

        assertFalse(aetherData.isEternalDay());
        assertFalse(aetherData.isShouldCycleCatchup());
        assertEquals(0L, aetherData.getAetherTime());
    }
}
