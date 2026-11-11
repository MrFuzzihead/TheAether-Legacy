package com.gildedgames.the_aether.network.packets;

import net.minecraft.entity.player.EntityPlayer;

import com.gildedgames.the_aether.api.player.IPlayerAether;
import com.gildedgames.the_aether.api.player.util.IAccessoryInventory;
import com.gildedgames.the_aether.inventory.InventoryAccessories;
import com.gildedgames.the_aether.player.PlayerAether;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketAccessory extends AetherPacket<PacketAccessory> {

    private IAccessoryInventory accessories;

    private byte[] accessoryData;

    private int entityID;

    public PacketAccessory() {

    }

    public PacketAccessory(IPlayerAether player) {
        this.accessories = player.getAccessoryInventory();
        this.entityID = player.getEntity()
            .getEntityId();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt();

        // FML may release the Netty buffer after fromBytes returns. Keep our
        // own copy instead of retaining a reference to the pooled buffer.
        this.accessoryData = new byte[buf.readableBytes()];
        buf.readBytes(this.accessoryData);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityID);
        this.accessories.writeData(buf);
    }

    @Override
    public void handleClient(PacketAccessory message, EntityPlayer player) {
        if (player != null && player.worldObj != null) {
            EntityPlayer parent = (EntityPlayer) player.worldObj.getEntityByID(message.entityID);

            if (parent != null) {
                InventoryAccessories newInventory = new InventoryAccessories(PlayerAether.get(parent));

                newInventory.readData(Unpooled.wrappedBuffer(message.accessoryData));

                PlayerAether.get(parent)
                    .setAccessoryInventory(newInventory);
            }
        }
    }

    @Override
    public void handleServer(PacketAccessory message, EntityPlayer player) {

    }

}
