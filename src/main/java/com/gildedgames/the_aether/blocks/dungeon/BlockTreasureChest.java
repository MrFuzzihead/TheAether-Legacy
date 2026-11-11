package com.gildedgames.the_aether.blocks.dungeon;

import java.util.Random;

import net.minecraft.block.BlockChest;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gildedgames.the_aether.Aether;
import com.gildedgames.the_aether.CommonProxy;
import com.gildedgames.the_aether.items.ItemsAether;
import com.gildedgames.the_aether.network.AetherGuiHandler;
import com.gildedgames.the_aether.tileentity.TileEntityTreasureChest;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTreasureChest extends BlockChest {

    public BlockTreasureChest() {
        super(56);

        this.setHardness(-1.0F);
        this.setResistance(2000.0F);
    }

    @Override
    public float getBlockHardness(World worldIn, int x, int y, int z) {
        TileEntity tile = worldIn.getTileEntity(x, y, z);

        if (tile instanceof TileEntityTreasureChest) {
            return ((TileEntityTreasureChest) tile).isLocked() ? this.blockHardness : 5.0F;
        } else {
            return this.blockHardness;
        }
    }

    @Override
    public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX,
        double explosionY, double explosionZ) {
        TileEntity tile = world.getTileEntity(x, y, z);

        if (!(tile instanceof TileEntityTreasureChest)) {
            return this.blockResistance;
        }

        return ((TileEntityTreasureChest) tile).isLocked() ? this.blockResistance : 10.0F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister p_149651_1_) {
        this.blockIcon = p_149651_1_.registerIcon(Aether.find("carved_stone"));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTreasureChest();
    }

    @Override
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX,
        float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        TileEntity tile = worldIn.getTileEntity(x, y, z);

        if (!(tile instanceof TileEntityTreasureChest)) {
            return false;
        }

        TileEntityTreasureChest treasurechest = (TileEntityTreasureChest) tile;
        ItemStack guiID = playerIn.getCurrentEquippedItem();

        if (treasurechest.isLocked()) {
            if (guiID == null || guiID.getItem() != ItemsAether.dungeon_key) {
                return false;
            }

            treasurechest.unlock(guiID.getItemDamage());

            --guiID.stackSize;
        } else {
            playerIn.openGui(Aether.instance, AetherGuiHandler.treasure_chest, worldIn, x, y, z);
        }

        return true;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public int getRenderType() {
        return CommonProxy.treasureChestRenderID;
    }

}
