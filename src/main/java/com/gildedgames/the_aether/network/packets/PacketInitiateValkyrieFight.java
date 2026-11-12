package com.gildedgames.the_aether.network.packets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.entities.bosses.valkyrie_queen.EntityValkyrieQueen;
import com.gildedgames.the_aether.items.ItemsAether;
import com.gildedgames.the_aether.player.PlayerAether;

import io.netty.buffer.ByteBuf;

public class PacketInitiateValkyrieFight extends AetherPacket<PacketInitiateValkyrieFight> {

    public int slotId, entityId;

    public PacketInitiateValkyrieFight() {

    }

    public PacketInitiateValkyrieFight(int slotId, int entityId) {
        this.slotId = slotId;
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slotId = buf.readInt();
        this.entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotId);
        buf.writeInt(this.entityId);
    }

    @Override
    public void handleClient(PacketInitiateValkyrieFight message, EntityPlayer player) {

    }

    @Override
    public void handleServer(PacketInitiateValkyrieFight message, EntityPlayer player) {
        if (player == null) {
            return;
        }

        // Validate the total medal count before consuming anything. The fee is
        // ten medals total, not necessarily ten in one stack.
        if (countMedals(player.inventory, ItemsAether.victory_medal) < 10) {
            return;
        }

        Entity entity = player.worldObj.getEntityByID(message.entityId);

        if (entity instanceof EntityValkyrieQueen) {
            EntityValkyrieQueen queen = (EntityValkyrieQueen) entity;

            // Don't restart an already-started duel or consume medals twice.
            if (queen.isBossReady()) {
                return;
            }

            // The dialogue is only opened by interacting with the Queen, so she
            // must be near the player; reject attempts against distant entities.
            double distance = player.getDistanceSqToEntity(queen);

            if (distance > 64.0D) {
                return;
            }

            // Consume exactly 10 medals (the entry fee), taking from as many
            // stacks as needed.
            consumeMedals(player.inventory, ItemsAether.victory_medal, 10);

            queen.setBossReady(true);
            PlayerAether.get(player)
                .setFocusedBoss(queen);
        }
    }

    static int countMedals(InventoryPlayer inventory, Item medal) {
        int count = 0;

        for (ItemStack stack : inventory.mainInventory) {
            if (stack != null && stack.getItem() == medal) {
                count += stack.stackSize;
            }
        }

        return count;
    }

    static void consumeMedals(InventoryPlayer inventory, Item medal, int amount) {
        for (int i = 0; i < inventory.mainInventory.length && amount > 0; ++i) {
            ItemStack stack = inventory.mainInventory[i];

            if (stack != null && stack.getItem() == medal) {
                int taken = Math.min(amount, stack.stackSize);
                inventory.decrStackSize(i, taken);
                amount -= taken;
            }
        }
    }

}
