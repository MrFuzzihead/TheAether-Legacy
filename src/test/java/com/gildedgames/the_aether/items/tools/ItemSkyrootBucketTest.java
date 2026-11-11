package com.gildedgames.the_aether.items.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.Test;
import org.mockito.MockedStatic;

import com.gildedgames.the_aether.items.util.EnumSkyrootBucketType;
import com.gildedgames.the_aether.player.PlayerAether;

public class ItemSkyrootBucketTest {

    @Test
    public void clientDoesNotConsumeTheBucket() {
        ItemSkyrootBucket bucket = new ItemSkyrootBucket();
        ItemStack stack = new ItemStack(bucket, 1, EnumSkyrootBucketType.Water.meta);
        World world = mock(World.class);
        EntityPlayer player = mock(EntityPlayer.class);

        world.isRemote = true;
        player.capabilities = new PlayerCapabilities();

        try (MockedStatic<PlayerAether> playerAether = mockStatic(PlayerAether.class)) {
            playerAether.when(() -> PlayerAether.get(player))
                .thenReturn(mock(PlayerAether.class));

            assertSame(stack, bucket.onBucketUsed(stack, world, player));
            assertEquals(1, stack.stackSize);
        }
    }

    @Test
    public void serverConsumesTheBucket() {
        ItemSkyrootBucket bucket = new ItemSkyrootBucket();
        ItemStack stack = new ItemStack(bucket, 1, EnumSkyrootBucketType.Water.meta);
        World world = mock(World.class);
        EntityPlayer player = mock(EntityPlayer.class);

        player.capabilities = new PlayerCapabilities();

        try (MockedStatic<PlayerAether> playerAether = mockStatic(PlayerAether.class)) {
            playerAether.when(() -> PlayerAether.get(player))
                .thenReturn(mock(PlayerAether.class));

            ItemStack result = bucket.onBucketUsed(stack, world, player);

            assertEquals(0, stack.stackSize);
            assertEquals(EnumSkyrootBucketType.Empty.meta, result.getItemDamage());
        }
    }
}
