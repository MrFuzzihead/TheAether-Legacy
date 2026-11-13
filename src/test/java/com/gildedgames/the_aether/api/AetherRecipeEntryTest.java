package com.gildedgames.the_aether.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.gildedgames.the_aether.api.enchantments.AetherEnchantment;
import com.gildedgames.the_aether.api.enchantments.AetherEnchantmentFuel;
import com.gildedgames.the_aether.api.freezables.AetherFreezable;
import com.gildedgames.the_aether.api.freezables.AetherFreezableFuel;

public class AetherRecipeEntryTest {

    @Test
    public void enchantmentsCompareTheirInputAndOutputStacks() {
        Item input = new Item();
        Item output = new Item();
        AetherEnchantment first = new AetherEnchantment(new ItemStack(input, 1, 2), new ItemStack(output), 100);
        AetherEnchantment same = new AetherEnchantment(new ItemStack(input, 1, 2), new ItemStack(output), 999);

        assertEquals(first, same);
        assertNotEquals(first, new AetherEnchantment(new ItemStack(input, 1, 3), new ItemStack(output), 100));
        assertNotEquals(first, new AetherEnchantment(new ItemStack(input, 1, 2), new ItemStack(input), 100));
        assertFalse(first.equals("not an enchantment"));
    }

    @Test
    public void freezablesCompareTheirInputAndOutputStacks() {
        Item input = new Item();
        Item output = new Item();
        AetherFreezable first = new AetherFreezable(new ItemStack(input, 1, 4), new ItemStack(output), 50);
        AetherFreezable same = new AetherFreezable(new ItemStack(input, 1, 4), new ItemStack(output), 999);

        assertEquals(first, same);
        assertNotEquals(first, new AetherFreezable(new ItemStack(input, 1, 5), new ItemStack(output), 50));
        assertNotEquals(first, new AetherFreezable(new ItemStack(input, 1, 4), new ItemStack(input), 50));
        assertFalse(first.equals("not a freezable"));
    }

    @Test
    public void fuelEntriesCompareTheirFuelStacks() {
        Item fuel = new Item();
        AetherEnchantmentFuel enchantmentFuel = new AetherEnchantmentFuel(fuel, 100);
        AetherFreezableFuel freezableFuel = new AetherFreezableFuel(fuel, 100);

        assertEquals(enchantmentFuel, new AetherEnchantmentFuel(fuel, 999));
        assertEquals(freezableFuel, new AetherFreezableFuel(fuel, 999));
        assertNotEquals(enchantmentFuel, new AetherEnchantmentFuel(new Item(), 100));
        assertNotEquals(freezableFuel, new AetherFreezableFuel(new Item(), 100));
        assertTrue(enchantmentFuel.getTimeGiven() > 0);
        assertTrue(freezableFuel.getTimeGiven() > 0);
    }
}
