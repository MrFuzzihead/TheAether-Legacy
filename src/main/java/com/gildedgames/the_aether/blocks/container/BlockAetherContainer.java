package com.gildedgames.the_aether.blocks.container;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gildedgames.the_aether.tileentity.util.AetherTileEntity;

public abstract class BlockAetherContainer extends BlockContainer {

    public BlockAetherContainer(Material materialIn) {
        super(materialIn);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (stack.hasDisplayName()) {
            TileEntity tileentity = worldIn.getTileEntity(x, y, z);

            if (tileentity instanceof AetherTileEntity) {
                ((AetherTileEntity) tileentity).setCustomName(stack.getDisplayName());
            }
        }
    }

    protected static void dropContainerItems(World worldIn, int x, int y, int z, AetherTileEntity tile) {
        for (int i = 0; i < tile.getSizeInventory(); ++i) {
            ItemStack itemstack = tile.getStackInSlot(i);

            if (itemstack != null) {
                float f = worldIn.rand.nextFloat() * 0.8F + 0.1F;
                float f1 = worldIn.rand.nextFloat() * 0.8F + 0.1F;
                float f2 = worldIn.rand.nextFloat() * 0.8F + 0.1F;

                while (itemstack.stackSize > 0) {
                    int j = worldIn.rand.nextInt(21) + 10;

                    if (j > itemstack.stackSize) {
                        j = itemstack.stackSize;
                    }

                    itemstack.stackSize -= j;
                    EntityItem entityitem = new EntityItem(
                        worldIn,
                        (double) ((float) x + f),
                        (double) ((float) y + f1),
                        (double) ((float) z + f2),
                        new ItemStack(itemstack.getItem(), j, itemstack.getItemDamage()));

                    if (itemstack.hasTagCompound()) {
                        entityitem.getEntityItem()
                            .setTagCompound(
                                (NBTTagCompound) itemstack.getTagCompound()
                                    .copy());
                    }

                    float f3 = 0.05F;
                    entityitem.motionX = (double) ((float) worldIn.rand.nextGaussian() * f3);
                    entityitem.motionY = (double) ((float) worldIn.rand.nextGaussian() * f3 + 0.2F);
                    entityitem.motionZ = (double) ((float) worldIn.rand.nextGaussian() * f3);
                    worldIn.spawnEntityInWorld(entityitem);
                }
            }
        }
    }

    public static void setState(World worldIn, int x, int y, int z, boolean isActive) {
        TileEntity tileentity = worldIn.getTileEntity(x, y, z);

        worldIn.setBlockMetadataWithNotify(x, y, z, isActive ? 1 : 0, 3);

        if (tileentity != null) {
            tileentity.validate();
            worldIn.setTileEntity(x, y, z, tileentity);
        }
    }

}
