package com.gildedgames.the_aether.blocks.natural;

import java.util.Random;

import net.minecraft.block.BlockLog;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.gildedgames.the_aether.blocks.BlocksAether;
import com.gildedgames.the_aether.items.ItemsAether;
import com.gildedgames.the_aether.items.tools.ItemAetherTool;
import com.gildedgames.the_aether.items.tools.ItemGravititeTool;
import com.gildedgames.the_aether.items.tools.ItemSkyrootTool;
import com.gildedgames.the_aether.items.tools.ItemValkyrieTool;
import com.gildedgames.the_aether.items.tools.ItemZaniteTool;
import com.gildedgames.the_aether.items.util.AetherDropHelper;
import com.gildedgames.the_aether.items.util.EnumAetherToolType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockAetherLog extends BlockLog {

    public BlockAetherLog() {
        super();
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, int x, int y, int z, int meta) {
        AetherDropHelper.addMiningStats(player, this);

        int size = this == BlocksAether.skyroot_log ? 2 : 1;

        ItemStack stack = player.getCurrentEquippedItem();

        if (this.canSilkHarvest(worldIn, player, x, y, z, meta) && AetherDropHelper.hasSilkTouch(stack)) {
            AetherDropHelper.dropSilkTouch(player, this, x, y, z, meta, stack);
        } else {
            if (stack != null && ((stack.getItem() instanceof ItemAetherTool
                && ((ItemAetherTool) stack.getItem()).toolType == EnumAetherToolType.AXE)
                || stack.getItem() == Items.diamond_axe)) {
                if (stack.getItem() instanceof ItemZaniteTool || stack.getItem() instanceof ItemGravititeTool
                    || stack.getItem() instanceof ItemValkyrieTool
                    || stack.getItem() == Items.diamond_axe) {
                    if (this == BlocksAether.golden_oak_log) {
                        this.dropBlockAsItem(
                            worldIn,
                            x,
                            y,
                            z,
                            new ItemStack(ItemsAether.golden_amber, 1 + worldIn.rand.nextInt(2)));
                    }

                    AetherDropHelper.dropWithFortune(player.worldObj, this, x, y, z, meta, stack);
                } else if (stack.getItem() instanceof ItemSkyrootTool) {
                    for (int i = 0; i < size; ++i) {
                        AetherDropHelper.dropWithFortune(player.worldObj, this, x, y, z, meta, stack);
                    }
                } else {
                    AetherDropHelper.dropWithFortune(player.worldObj, this, x, y, z, meta, stack);
                }
            } else {
                super.harvestBlock(worldIn, player, x, y, z, meta);
            }
        }
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        // Golden oak logs only drop themselves through the silk-touch path above;
        // their normal drop is a skyroot log (plus golden amber from a suitable axe).
        return Item.getItemFromBlock(BlocksAether.skyroot_log);
    }

    @Override
    public int damageDropped(int meta) {
        return 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister registry) {
        this.field_150167_a = new IIcon[1];
        this.field_150166_b = new IIcon[1];

        for (int i = 0; i < this.field_150167_a.length; ++i) {
            this.field_150167_a[i] = registry.registerIcon(this.getTextureName() + "_side");
            this.field_150166_b[i] = registry.registerIcon(this.getTextureName() + "_top");
        }
    }

}
