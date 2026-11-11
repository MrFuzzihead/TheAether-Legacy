package com.gildedgames.the_aether.inventory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
