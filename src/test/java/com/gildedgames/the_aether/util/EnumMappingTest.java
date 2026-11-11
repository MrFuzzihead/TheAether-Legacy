package com.gildedgames.the_aether.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.gildedgames.the_aether.api.accessories.AccessoryType;
import com.gildedgames.the_aether.items.util.EnumAetherToolType;
import com.gildedgames.the_aether.items.util.EnumDartShooterType;
import com.gildedgames.the_aether.items.util.EnumDartType;
import com.gildedgames.the_aether.items.util.EnumDungeonKeyType;
import com.gildedgames.the_aether.items.util.EnumSkyrootBucketType;

public class EnumMappingTest {

    @Test
    public void skyrootBucketMetadataMapsToTheExpectedType() {
        assertSame(EnumSkyrootBucketType.Empty, EnumSkyrootBucketType.getType(0));
        assertSame(EnumSkyrootBucketType.Water, EnumSkyrootBucketType.getType(1));
        assertSame(EnumSkyrootBucketType.Poison, EnumSkyrootBucketType.getType(2));
        assertSame(EnumSkyrootBucketType.Remedy, EnumSkyrootBucketType.getType(3));
        assertSame(EnumSkyrootBucketType.Milk, EnumSkyrootBucketType.getType(4));
        assertSame(EnumSkyrootBucketType.Empty, EnumSkyrootBucketType.getType(99));
    }

    @Test
    public void dungeonKeyMetadataMapsToTheExpectedType() {
        assertSame(EnumDungeonKeyType.Bronze, EnumDungeonKeyType.getType(0));
        assertSame(EnumDungeonKeyType.Silver, EnumDungeonKeyType.getType(1));
        assertSame(EnumDungeonKeyType.Golden, EnumDungeonKeyType.getType(2));
        assertSame(EnumDungeonKeyType.Bronze, EnumDungeonKeyType.getType(-1));
    }

    @Test
    public void dartMetadataMapsToTheExpectedType() {
        assertSame(EnumDartType.Golden, EnumDartType.getType(0));
        assertSame(EnumDartType.Poison, EnumDartType.getType(1));
        assertSame(EnumDartType.Enchanted, EnumDartType.getType(2));
        assertSame(EnumDartType.Golden, EnumDartType.getType(99));

        assertSame(EnumDartShooterType.Golden, EnumDartShooterType.getType(0));
        assertSame(EnumDartShooterType.Poison, EnumDartShooterType.getType(1));
        assertSame(EnumDartShooterType.Enchanted, EnumDartShooterType.getType(2));
    }

    @Test
    public void accessoryTypesHaveTheExpectedSlots() {
        assertEquals(8, AccessoryType.values().length);
        assertNotNull(
            AccessoryType.createCompleteList()
                .func_148745_a(0));
        assertSame(
            AccessoryType.PENDANT,
            AccessoryType.createCompleteList()
                .func_148745_a(0));
        assertSame(
            AccessoryType.CAPE,
            AccessoryType.createCompleteList()
                .func_148745_a(1));
        assertSame(
            AccessoryType.SHIELD,
            AccessoryType.createCompleteList()
                .func_148745_a(2));
        assertSame(
            AccessoryType.MISC,
            AccessoryType.createCompleteList()
                .func_148745_a(3));
        assertSame(
            AccessoryType.RING,
            AccessoryType.createCompleteList()
                .func_148745_a(4));
        assertSame(
            AccessoryType.EXTRA_RING,
            AccessoryType.createCompleteList()
                .func_148745_a(5));
        assertSame(
            AccessoryType.GLOVES,
            AccessoryType.createCompleteList()
                .func_148745_a(6));
        assertSame(
            AccessoryType.EXTRA_MISC,
            AccessoryType.createCompleteList()
                .func_148745_a(7));
    }

    @Test
    public void toolTypesExposeAProperMaterialSpeed() {
        assertEquals(4.0F, EnumAetherToolType.PICKAXE.efficiencyOnProperMaterial, 0.0F);
        assertEquals(4.0F, EnumAetherToolType.AXE.efficiencyOnProperMaterial, 0.0F);
        assertEquals(4.0F, EnumAetherToolType.SHOVEL.efficiencyOnProperMaterial, 0.0F);
    }
}
