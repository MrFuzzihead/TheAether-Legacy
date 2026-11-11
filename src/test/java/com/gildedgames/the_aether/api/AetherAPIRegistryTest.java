package com.gildedgames.the_aether.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.gildedgames.the_aether.api.accessories.AccessoryType;
import com.gildedgames.the_aether.api.accessories.AetherAccessory;
import com.gildedgames.the_aether.api.enchantments.AetherEnchantment;
import com.gildedgames.the_aether.api.enchantments.AetherEnchantmentFuel;
import com.gildedgames.the_aether.api.freezables.AetherFreezable;
import com.gildedgames.the_aether.api.freezables.AetherFreezableFuel;

public class AetherAPIRegistryTest {

    private static final AtomicInteger NAME_COUNTER = new AtomicInteger();

    private static Item mockItem() {
        Item item = mock(Item.class);
        when(item.getUnlocalizedName()).thenReturn("item.aether_test_" + NAME_COUNTER.incrementAndGet());
        return item;
    }

    private static ItemStack stack(Item item) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        when(stack.isItemStackDamageable()).thenReturn(false);
        when(stack.getItemDamage()).thenReturn(0);
        return stack;
    }

    @Test
    public void accessoryRegistryRoundTrips() {
        Item item = mockItem();
        ItemStack stack = stack(item);

        AetherAccessory accessory = new AetherAccessory(stack, AccessoryType.RING);
        AetherAPI.instance()
            .register(accessory);

        assertTrue(
            AetherAPI.instance()
                .isAccessory(stack));
        assertSame(
            accessory,
            AetherAPI.instance()
                .getAccessory(stack));
        assertEquals(AccessoryType.RING, accessory.getAccessoryType());
        assertEquals(AccessoryType.EXTRA_RING, accessory.getExtraType());
    }

    @Test
    public void enchantmentRegistryRoundTrips() {
        ItemStack input = stack(mockItem());
        ItemStack output = stack(mockItem());

        AetherEnchantment enchantment = new AetherEnchantment(input, output, 123);
        AetherAPI.instance()
            .register(enchantment);

        assertTrue(
            AetherAPI.instance()
                .hasEnchantment(input));
        assertSame(
            enchantment,
            AetherAPI.instance()
                .getEnchantment(input));
        assertEquals(123, enchantment.getTimeRequired());
        assertTrue(
            AetherAPI.instance()
                .getEnchantmentValues()
                .contains(enchantment));
    }

    @Test
    public void enchantmentFuelRegistryRoundTrips() {
        ItemStack fuel = stack(mockItem());

        AetherEnchantmentFuel enchantmentFuel = new AetherEnchantmentFuel(fuel, 500);
        AetherAPI.instance()
            .register(enchantmentFuel);

        assertTrue(
            AetherAPI.instance()
                .isEnchantmentFuel(fuel));
        assertSame(
            enchantmentFuel,
            AetherAPI.instance()
                .getEnchantmentFuel(fuel));
        assertEquals(500, enchantmentFuel.getTimeGiven());
    }

    @Test
    public void freezableRegistryRoundTrips() {
        ItemStack input = stack(mockItem());
        ItemStack output = stack(mockItem());

        AetherFreezable freezable = new AetherFreezable(input, output, 250);
        AetherAPI.instance()
            .register(freezable);

        assertTrue(
            AetherAPI.instance()
                .hasFreezable(input));
        assertSame(
            freezable,
            AetherAPI.instance()
                .getFreezable(input));
        assertEquals(250, freezable.getTimeRequired());
        assertTrue(
            AetherAPI.instance()
                .getFreezableValues()
                .contains(freezable));
    }

    @Test
    public void freezableFuelRegistryRoundTrips() {
        ItemStack fuel = stack(mockItem());

        AetherFreezableFuel freezableFuel = new AetherFreezableFuel(fuel, 500);
        AetherAPI.instance()
            .register(freezableFuel);

        assertTrue(
            AetherAPI.instance()
                .isFreezableFuel(fuel));
        assertSame(
            freezableFuel,
            AetherAPI.instance()
                .getFreezableFuel(fuel));
        assertEquals(500, freezableFuel.getTimeGiven());
    }

    @Test
    public void unknownStacksAreNotRegistered() {
        ItemStack stack = stack(mockItem());

        assertFalse(
            AetherAPI.instance()
                .isAccessory(stack));
        assertFalse(
            AetherAPI.instance()
                .hasEnchantment(stack));
        assertFalse(
            AetherAPI.instance()
                .isEnchantmentFuel(stack));
        assertFalse(
            AetherAPI.instance()
                .hasFreezable(stack));
        assertFalse(
            AetherAPI.instance()
                .isFreezableFuel(stack));
    }

    @Test
    public void enchantmentEqualityComparesInputAndOutput() {
        ItemStack inputA = stack(mockItem());
        ItemStack outputA = stack(mockItem());
        AetherEnchantment a = new AetherEnchantment(inputA, outputA, 1);

        ItemStack inputB = stack(inputA.getItem());
        ItemStack outputB = stack(outputA.getItem());
        AetherEnchantment b = new AetherEnchantment(inputB, outputB, 2);

        assertEquals(a, b);
        assertNotNull(a.getInput());
        assertNotNull(a.getOutput());
    }
}
