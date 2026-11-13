package com.gildedgames.the_aether.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.Before;
import org.junit.Test;

import com.gildedgames.the_aether.api.accessories.AccessoryType;
import com.gildedgames.the_aether.api.player.IPlayerAether;
import com.gildedgames.the_aether.items.accessories.ItemAccessory;

public class InventoryAccessoriesTest {

    private IPlayerAether playerAether;
    private EntityPlayer entityPlayer;

    @Before
    public void setUp() {
        playerAether = mock(IPlayerAether.class);
        entityPlayer = mock(EntityPlayer.class);

        World world = mock(World.class);
        world.isRemote = true;
        entityPlayer.worldObj = world;

        when(playerAether.getEntity()).thenReturn(entityPlayer);
    }

    @Test
    public void dropAccessoriesDropsAndClearsEachOccupiedSlot() {
        InventoryAccessories inventory = new InventoryAccessories(playerAether);
        Item item = mock(Item.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        stack.stackSize = 3;

        inventory.setInventorySlotContents(0, stack);
        inventory.dropAccessories();

        verify(entityPlayer).dropItem(item, 3);
        assertNull(inventory.getStackInSlot(0));
    }

    @Test
    public void wearingAccessoryByItemDoesNotAllocateAnItemStack() {
        InventoryAccessories inventory = new InventoryAccessories(playerAether);
        Item item = mock(Item.class);
        ItemStack stack = mock(ItemStack.class);

        when(stack.getItem()).thenReturn(item);
        inventory.setInventorySlotContents(0, stack);

        org.junit.Assert.assertTrue(inventory.wearingAccessory(item));
    }

    @Test
    public void zaniteBonusScalesWithDamage() {
        Item item = new Item().setMaxDamage(300);
        ItemStack stack = new ItemStack(item);
        stack.setItemDamage(150);

        assertEquals(1.1666666F, InventoryAccessories.applyZaniteBonus(1.0F, stack), 0.0001F);
    }

    @Test
    public void decrStackSizeSplitsAndClearsStacks() {
        InventoryAccessories inventory = new InventoryAccessories(playerAether);
        Item item = new Item();
        inventory.setInventorySlotContents(0, new ItemStack(item, 3));

        ItemStack split = inventory.decrStackSize(0, 2);

        assertEquals(2, split.stackSize);
        assertEquals(1, inventory.getStackInSlot(0).stackSize);

        ItemStack rest = inventory.decrStackSize(0, 2);

        assertEquals(1, rest.stackSize);
        assertNull(inventory.getStackInSlot(0));
    }

    @Test
    public void closingLookupRemovesTheStack() {
        InventoryAccessories inventory = new InventoryAccessories(playerAether);
        ItemStack stack = new ItemStack(new Item());

        inventory.setInventorySlotContents(0, stack);

        assertSame(stack, inventory.getStackInSlotOnClosing(0));
        assertNull(inventory.getStackInSlot(0));
    }

    @Test
    public void accessoryCountsAndLookupsIgnoreEmptySlots() {
        InventoryAccessories inventory = new InventoryAccessories(playerAether);
        Item item = new Item();
        ItemStack stack = new ItemStack(item);

        inventory.setInventorySlotContents(0, stack);
        inventory.setInventorySlotContents(1, stack);

        assertEquals(2, inventory.getAccessoryCount(stack));
        assertTrue(inventory.wearingAccessory(item));
        assertFalse(inventory.wearingAccessory(new Item()));
        assertFalse(inventory.wearingAccessory((ItemStack) null));
    }

    @Test
    public void setAccessorySlotPlacesAnAccessoryInItsFirstValidSlot() {
        InventoryAccessories inventory = new InventoryAccessories(playerAether);
        ItemAccessory accessory = new ItemAccessory(AccessoryType.RING);
        ItemStack stack = new ItemStack(accessory);

        assertTrue(inventory.setAccessorySlot(stack));
        assertSame(stack, inventory.getStackInSlot(AccessoryType.RING));
        assertTrue(inventory.setAccessorySlot(stack));
        assertSame(stack, inventory.getStackInSlot(AccessoryType.EXTRA_RING));
        assertFalse(inventory.setAccessorySlot(stack));
    }

    @Test
    public void removeStackFromAccessorySlotClearsTheConfiguredSlot() {
        InventoryAccessories inventory = new InventoryAccessories(playerAether);
        ItemStack stack = mock(ItemStack.class);
        int slot = AccessoryType.createCompleteList()
            .func_148747_b(AccessoryType.RING);

        inventory.setInventorySlotContents(slot, stack);
        ItemStack removed = inventory.removeStackFromAccessorySlot(AccessoryType.RING);

        assertSame(stack, removed);
        assertNull(inventory.getStackInSlot(AccessoryType.RING));
    }
}
