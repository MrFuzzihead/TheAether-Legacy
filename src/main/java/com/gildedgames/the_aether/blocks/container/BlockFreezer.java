package com.gildedgames.the_aether.blocks.container;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gildedgames.the_aether.Aether;
import com.gildedgames.the_aether.blocks.BlocksAether;
import com.gildedgames.the_aether.network.AetherGuiHandler;
import com.gildedgames.the_aether.tileentity.TileEntityFreezer;
import com.gildedgames.the_aether.tileentity.util.AetherTileEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockFreezer extends BlockAetherContainer {

    @SideOnly(Side.CLIENT)
    private IIcon blockIconTop;

    @SideOnly(Side.CLIENT)
    private IIcon blockIconBottom;

    public BlockFreezer() {
        super(Material.rock);

        this.setHardness(2.5F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister registry) {
        this.blockIcon = registry.registerIcon(Aether.find("freezer_side"));
        this.blockIconTop = registry.registerIcon(Aether.find("freezer_top"));
        this.blockIconBottom = registry.registerIcon(Aether.find("enchanter_bottom"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side == 1 ? this.blockIconTop : (side == 0 ? this.blockIconBottom : this.blockIcon);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 1) {
            return this.blockIconTop;
        } else if (side == 0) {
            return this.blockIconBottom;
        }

        return this.blockIcon;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFreezer();
    }

    @Override
    public Item getItemDropped(int meta, Random rand, int fortune) {
        return Item.getItemFromBlock(BlocksAether.freezer);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        player.openGui(Aether.instance, AetherGuiHandler.freezer, world, x, y, z);

        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (world.getBlockMetadata(x, y, z) == 1) {
            float f = (float) x + 0.5F;
            float f1 = (float) y + 1.0F + (random.nextFloat() * 6F) / 16F;
            float f2 = (float) z + 0.5F;

            world.spawnParticle("smoke", f, f1, f2, 0.0D, 0.0D, 0.0D);
            world.spawnParticle("snowshovel", f, f1, f2, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void breakBlock(World worldIn, int x, int y, int z, Block block, int meta) {
        TileEntity tileentity = worldIn.getTileEntity(x, y, z);

        if (tileentity instanceof AetherTileEntity) {
            dropContainerItems(worldIn, x, y, z, (AetherTileEntity) tileentity);
            worldIn.func_147453_f(x, y, z, this);
        }

        super.breakBlock(worldIn, x, y, z, block, meta);
    }

}
