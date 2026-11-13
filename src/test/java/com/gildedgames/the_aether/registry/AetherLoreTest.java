package com.gildedgames.the_aether.registry;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.item.ItemStack;

import org.junit.Test;

public class AetherLoreTest {

    @Test
    public void nullStackHasNoLoreEntry() {
        assertNull(AetherLore.getLoreEntryKey(null));
    }

    @Test
    public void stackWithoutAnItemHasNoLoreEntry() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(null);

        assertNull(AetherLore.getLoreEntryKey(stack));
    }
}
