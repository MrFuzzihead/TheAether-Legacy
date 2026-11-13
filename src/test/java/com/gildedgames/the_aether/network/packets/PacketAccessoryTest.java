package com.gildedgames.the_aether.network.packets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.Test;

import com.gildedgames.the_aether.api.player.IPlayerAether;
import com.gildedgames.the_aether.api.player.util.IAccessoryInventory;
import com.gildedgames.the_aether.inventory.InventoryAccessories;
import com.gildedgames.the_aether.player.PlayerAether;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketAccessoryTest {

    @Test
    public void decodingSurvivesTheNettyBufferBeingReleased() {
        int entityId = 7;

        IAccessoryInventory accessories = mock(IAccessoryInventory.class);
        IPlayerAether playerAether = mock(IPlayerAether.class);
        EntityPlayer entity = mock(EntityPlayer.class);

        when(playerAether.getAccessoryInventory()).thenReturn(accessories);
        when(playerAether.getEntity()).thenReturn(entity);
        when(entity.getEntityId()).thenReturn(entityId);

        doAnswer(invocation -> {
            invocation.getArgument(0, ByteBuf.class)
                .writeInt(0);
            return null;
        }).when(accessories)
            .writeData(any(ByteBuf.class));

        PacketAccessory packet = new PacketAccessory(playerAether);
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes(buffer);

        PacketAccessory decoded = new PacketAccessory();
        decoded.fromBytes(buffer);
        buffer.release();

        World world = mock(World.class);
        EntityPlayer receiver = mock(EntityPlayer.class);
        receiver.worldObj = world;

        PlayerAether target = mock(PlayerAether.class);
        when(world.getEntityByID(entityId)).thenReturn(entity);
        when(entity.getExtendedProperties("aether_legacy:player_aether")).thenReturn(target);

        decoded.handleClient(decoded, receiver);

        verify(target).setAccessoryInventory(any(InventoryAccessories.class));
    }
}
