package com.gildedgames.the_aether.network.packets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

public class PacketInitiateValkyrieFightTest {

    @Test
    public void medalCountSumsAcrossStacks() {
        Item medal = new Item();
        InventoryPlayer inventory = new InventoryPlayer(null);
        inventory.mainInventory[0] = new ItemStack(medal, 4);
        inventory.mainInventory[1] = new ItemStack(medal, 7);

        assertEquals(11, PacketInitiateValkyrieFight.countMedals(inventory, medal));
    }

    @Test
    public void medalConsumptionSpansStacks() {
        Item medal = new Item();
        InventoryPlayer inventory = new InventoryPlayer(null);
        inventory.mainInventory[0] = new ItemStack(medal, 4);
        inventory.mainInventory[1] = new ItemStack(medal, 7);

        PacketInitiateValkyrieFight.consumeMedals(inventory, medal, 10);

        assertNull(inventory.mainInventory[0]);
        assertEquals(1, inventory.mainInventory[1].stackSize);
        assertEquals(1, PacketInitiateValkyrieFight.countMedals(inventory, medal));
    }
}
