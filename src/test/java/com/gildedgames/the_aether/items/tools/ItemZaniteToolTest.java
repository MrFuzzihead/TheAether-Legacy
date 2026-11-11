package com.gildedgames.the_aether.items.tools;

import static org.junit.Assert.assertEquals;

import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.gildedgames.the_aether.items.util.EnumAetherToolType;

public class ItemZaniteToolTest {

    @Test
    public void damageIncreasesMiningSpeed() {
        ItemZaniteTool tool = new ItemZaniteTool(0.0F, EnumAetherToolType.PICKAXE);
        ItemStack stack = new ItemStack(tool);

        stack.setItemDamage(0);
        assertEquals(2.0F, ItemZaniteTool.calculateIncrease(stack, 4.0F), 0.0001F);

        stack.setItemDamage(stack.getMaxDamage() - 1);
        assertEquals(12.0F, ItemZaniteTool.calculateIncrease(stack, 4.0F), 0.0001F);
    }

    @Test
    public void nonMatchingOriginalSpeedIsNotChanged() {
        ItemZaniteTool tool = new ItemZaniteTool(0.0F, EnumAetherToolType.PICKAXE);
        ItemStack stack = new ItemStack(tool);

        stack.setItemDamage(stack.getMaxDamage() - 1);
        assertEquals(1.0F, ItemZaniteTool.calculateIncrease(stack, 1.0F), 0.0001F);
    }
}
