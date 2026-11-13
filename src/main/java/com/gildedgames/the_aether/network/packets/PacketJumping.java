package com.gildedgames.the_aether.network.packets;

import net.minecraft.entity.player.EntityPlayer;

import com.gildedgames.the_aether.player.PlayerAether;

import io.netty.buffer.ByteBuf;

public class PacketJumping extends AetherPacket<PacketJumping> {

    boolean jumping;

    public PacketJumping() {

    }

    public PacketJumping(boolean jumping) {
        this.jumping = jumping;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.jumping = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.jumping);
    }

    @Override
    public void handleClient(PacketJumping message, EntityPlayer player) {

    }

    @Override
    public void handleServer(PacketJumping message, EntityPlayer player) {
        if (player != null && player.worldObj != null && !player.worldObj.isRemote) {
            PlayerAether.get(player)
                .setJumping(message.jumping);
        }
    }
}
