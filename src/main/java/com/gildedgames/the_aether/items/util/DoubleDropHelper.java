package com.gildedgames.the_aether.items.util;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.items.tools.ItemSkyrootTool;

public class DoubleDropHelper {

    public static void dropBlock(EntityPlayer player, int x, int y, int z, Block block, int meta) {
        AetherDropHelper.addMiningStats(player, block);

        int size = meta == 0 ? 2 : 1;
        ItemStack stack = player.inventory.getCurrentItem();

        if (block.canSilkHarvest(player.worldObj, player, x, y, z, meta) && AetherDropHelper.hasSilkTouch(stack)) {
            AetherDropHelper.dropSilkTouch(player, block, x, y, z, meta, stack);

            return;
        }

        if (stack == null || !(stack.getItem() instanceof ItemSkyrootTool)) {
            AetherDropHelper.dropWithFortune(player.worldObj, block, x, y, z, meta, stack);

            return;
        }

        ItemSkyrootTool skyrootTool = (ItemSkyrootTool) stack.getItem();

        if (skyrootTool.getDigSpeed(stack, block, meta) == skyrootTool.getEffectiveSpeed()) {
            for (int i = 0; i < size; ++i) {
                AetherDropHelper.dropWithFortune(player.worldObj, block, x, y, z, meta, stack);
            }
        } else {
            AetherDropHelper.dropWithFortune(player.worldObj, block, x, y, z, meta, stack);
        }
    }
}
