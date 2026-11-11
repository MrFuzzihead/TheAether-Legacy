package com.gildedgames.the_aether.blocks.dungeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.Test;

import com.gildedgames.the_aether.tileentity.TileEntityTreasureChest;

public class BlockTreasureChestTest {

    private static final int X = 1;
    private static final int Y = 2;
    private static final int Z = 3;

    @Test
    public void missingTileEntityDoesNotCrashHardnessOrExplosionChecks() {
        World world = mock(World.class);
        BlockTreasureChest block = new BlockTreasureChest();

        when(world.getTileEntity(X, Y, Z)).thenReturn(null);

        assertEquals(-1.0F, block.getBlockHardness(world, X, Y, Z), 0.0F);
        assertEquals(6000.0F, block.getExplosionResistance(null, world, X, Y, Z, 0, 0, 0), 0.0F);
        assertFalse(block.onBlockActivated(world, X, Y, Z, mock(EntityPlayer.class), 0, 0, 0, 0));
    }

    @Test
    public void lockedAndUnlockedChestsUseTheirExpectedHardness() {
        World world = mock(World.class);
        BlockTreasureChest block = new BlockTreasureChest();
        TileEntityTreasureChest chest = mock(TileEntityTreasureChest.class);

        when(world.getTileEntity(X, Y, Z)).thenReturn(chest);
        when(chest.isLocked()).thenReturn(true);

        assertEquals(-1.0F, block.getBlockHardness(world, X, Y, Z), 0.0F);
        assertEquals(6000.0F, block.getExplosionResistance(null, world, X, Y, Z, 0, 0, 0), 0.0F);

        when(chest.isLocked()).thenReturn(false);

        assertEquals(5.0F, block.getBlockHardness(world, X, Y, Z), 0.0F);
        assertEquals(10.0F, block.getExplosionResistance(null, world, X, Y, Z, 0, 0, 0), 0.0F);
    }

    @Test
    public void wrongTileEntityTypeIsIgnored() {
        World world = mock(World.class);
        BlockTreasureChest block = new BlockTreasureChest();

        when(world.getTileEntity(X, Y, Z)).thenReturn(mock(TileEntity.class));

        assertEquals(-1.0F, block.getBlockHardness(world, X, Y, Z), 0.0F);
        assertEquals(6000.0F, block.getExplosionResistance(null, world, X, Y, Z, 0, 0, 0), 0.0F);
        assertFalse(block.onBlockActivated(world, X, Y, Z, mock(EntityPlayer.class), 0, 0, 0, 0));
    }
}
