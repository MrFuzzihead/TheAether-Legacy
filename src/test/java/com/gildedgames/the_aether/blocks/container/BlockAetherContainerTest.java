package com.gildedgames.the_aether.blocks.container;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.Test;
import org.mockito.MockedConstruction;

import com.gildedgames.the_aether.tileentity.util.AetherTileEntity;

public class BlockAetherContainerTest {

    @Test
    public void containerDropsEachItemStack() {
        World world = mock(World.class);
        world.rand = new Random(0L);

        AetherTileEntity tile = mock(AetherTileEntity.class);
        ItemStack stack = new ItemStack(new Item(), 5);

        when(tile.getSizeInventory()).thenReturn(1);
        when(tile.getStackInSlot(0)).thenReturn(stack);

        try (MockedConstruction<EntityItem> construction = mockConstruction(EntityItem.class)) {
            BlockAetherContainer.dropContainerItems(world, 1, 2, 3, tile);

            assertEquals(
                1,
                construction.constructed()
                    .size());
            verify(world).spawnEntityInWorld(
                construction.constructed()
                    .get(0));
            assertEquals(0, stack.stackSize);
        }
    }

    @Test
    public void nonAetherTileEntityIsIgnoredByBreakBlock() {
        World world = mock(World.class);
        world.rand = new Random(0L);

        when(world.getTileEntity(1, 2, 3)).thenReturn(mock(TileEntity.class));

        BlockAetherContainer block = new BlockAetherContainer(net.minecraft.block.material.Material.rock) {

            @Override
            public TileEntity createNewTileEntity(World worldIn, int meta) {
                return null;
            }
        };

        try (MockedConstruction<EntityItem> construction = mockConstruction(EntityItem.class)) {
            block.breakBlock(world, 1, 2, 3, null, 0);

            assertEquals(
                0,
                construction.constructed()
                    .size());
        }
    }
}
