package com.gildedgames.the_aether.world.dungeon.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.junit.Test;

public class AetherDungeonTest {

    private static final class TestDungeon extends AetherDungeon {

        @Override
        public boolean generate(World world, Random random, int x, int y, int z) {
            return true;
        }
    }

    private static TestDungeon dungeon(World world) {
        TestDungeon dungeon = new TestDungeon();
        dungeon.replaceAir = true;
        dungeon.replaceSolid = false;
        dungeon.setBlocks(Blocks.stone);

        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(Blocks.air);

        return dungeon;
    }

    @Test
    public void chanceZeroLinePlacesEveryBlock() {
        World world = mock(World.class);
        TestDungeon dungeon = dungeon(world);

        dungeon.addLineX(world, new Random(0L), new PositionData(0, 0, 0), 3);

        verify(world, times(3)).setBlock(anyInt(), anyInt(), anyInt(), eq(Blocks.stone), eq(0), anyInt());
    }

    @Test
    public void chanceZeroPlanePlacesEveryBlock() {
        World world = mock(World.class);
        TestDungeon dungeon = dungeon(world);

        dungeon.addPlaneY(world, new Random(0L), new PositionData(0, 0, 0), new PositionData(2, 1, 3));

        verify(world, times(6)).setBlock(anyInt(), anyInt(), anyInt(), eq(Blocks.stone), eq(0), anyInt());
    }

    @Test
    public void chanceZeroSolidBoxPlacesEveryBlock() {
        World world = mock(World.class);
        TestDungeon dungeon = dungeon(world);

        dungeon.addSolidBox(world, new Random(0L), new PositionData(0, 0, 0), new PositionData(2, 2, 2));

        verify(world, times(8)).setBlock(anyInt(), anyInt(), anyInt(), eq(Blocks.stone), eq(0), anyInt());
    }

    @Test
    public void verticalAndDepthLinesPlaceEveryBlock() {
        World world = mock(World.class);
        TestDungeon dungeon = dungeon(world);

        dungeon.addLineY(world, new Random(0L), new PositionData(5, 0, 0), 3);
        dungeon.addLineZ(world, new Random(0L), new PositionData(6, 0, 0), 3);

        verify(world, times(3)).setBlock(eq(5), anyInt(), eq(0), eq(Blocks.stone), eq(0), anyInt());
        verify(world, times(3)).setBlock(eq(6), eq(0), anyInt(), eq(Blocks.stone), eq(0), anyInt());
    }

    @Test
    public void boxQueriesUseTheWorldBlocks() {
        World world = mock(World.class);
        TestDungeon dungeon = new TestDungeon();
        Block stone = mock(Block.class);
        PositionData start = new PositionData(0, 0, 0);
        PositionData size = new PositionData(2, 2, 2);

        assertTrue(dungeon.isBoxEmpty(world, start, size));
        assertFalse(dungeon.isBoxSolid(world, start, size));

        when(world.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(stone);

        assertTrue(dungeon.isBoxSolid(world, start, size));
        assertFalse(dungeon.isBoxEmpty(world, start, size));
        assertTrue(dungeon.hasBlock(world, start, size, stone));
        assertFalse(dungeon.hasBlock(world, start, size, Blocks.dirt));
    }

    @Test
    public void chanceUsesExtraBlockOnSuccessfulRoll() {
        World world = mock(World.class);
        TestDungeon dungeon = dungeon(world);
        Block normal = mock(Block.class);
        Block extra = mock(Block.class);
        Random random = mock(Random.class);
        when(random.nextInt(2)).thenReturn(0, 1);

        dungeon.setBlocks(normal, extra, 2);
        dungeon.addLineX(world, random, new PositionData(0, 0, 0), 2);

        verify(world).setBlock(anyInt(), anyInt(), anyInt(), eq(extra), eq(0), anyInt());
        verify(world).setBlock(anyInt(), anyInt(), anyInt(), eq(normal), eq(0), anyInt());
    }
}
