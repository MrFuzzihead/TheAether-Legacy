package com.gildedgames.the_aether.items.util;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Shared harvest/drop primitives. The block-specific policy (double drops,
 * golden amber, ore species) stays with the block; this class owns the common
 * stat, fortune, silk-touch and item-spawning rules.
 */
public final class AetherDropHelper {

    private AetherDropHelper() {}

    public static void addMiningStats(EntityPlayer player, Block block) {
        player.addStat(StatList.mineBlockStatArray[Block.getIdFromBlock(block)], 1);
        player.addExhaustion(0.025F);
    }

    public static int fortuneLevel(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, stack);
    }

    public static boolean hasSilkTouch(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, stack) > 0;
    }

    public static void dropWithFortune(World world, Block block, int x, int y, int z, int meta, ItemStack tool) {
        block.dropBlockAsItem(world, x, y, z, meta, fortuneLevel(tool));
    }

    public static void dropSilkTouch(EntityPlayer player, Block block, int x, int y, int z, int meta, ItemStack tool) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        ItemStack itemstack = createStackedBlock(meta, block);

        if (itemstack != null) {
            items.add(itemstack);
        }

        ForgeEventFactory.fireBlockHarvesting(items, player.worldObj, block, x, y, z, meta, 0, 1.0f, true, player);

        for (ItemStack is : items) {
            dropBlockAsItem(player.worldObj, x, y, z, is);
        }
    }

    public static ItemStack createStackedBlock(int meta, Block block) {
        int itemMeta = 0;
        Item item = Item.getItemFromBlock(block);

        if (item != null && item.getHasSubtypes()) {
            itemMeta = meta;
        }

        return item == null ? null : new ItemStack(item, 1, itemMeta);
    }

    public static void dropBlockAsItem(World world, int x, int y, int z, ItemStack stack) {
        if (!world.isRemote && world.getGameRules()
            .getGameRuleBooleanValue("doTileDrops") && !world.restoringBlockSnapshots) {
            float f = 0.7F;
            double d0 = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
            double d1 = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
            double d2 = (double) (world.rand.nextFloat() * f) + (double) (1.0F - f) * 0.5D;
            EntityItem entityitem = new EntityItem(world, (double) x + d0, (double) y + d1, (double) z + d2, stack);
            entityitem.delayBeforeCanPickup = 10;
            world.spawnEntityInWorld(entityitem);
        }
    }
}
