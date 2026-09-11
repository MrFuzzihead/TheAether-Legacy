package com.gildedgames.the_aether.tileentity;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntityChest;

import com.gildedgames.the_aether.world.dungeon.BronzeDungeon;
import com.gildedgames.the_aether.world.gen.components.ComponentGoldenDungeon;
import com.gildedgames.the_aether.world.gen.components.ComponentSilverDungeon;

import cpw.mods.fml.common.FMLCommonHandler;

public class TileEntityTreasureChest extends TileEntityChest {

    private boolean locked = true;

    private int kind = 0;

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.locked = compound.getBoolean("locked");
        this.kind = compound.getInteger("dungeonType");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setBoolean("locked", this.locked);
        compound.setInteger("dungeonType", this.kind);
    }

    public void unlock(int kind) {
        this.kind = kind;
        Random random = new Random();

        int amount = 5 + random.nextInt(5);
        int slots = this.getSizeInventory();

        // Pick distinct random slots so loot never overwrites itself (rolling
        // a slot twice silently discards a drop).
        int[] slotOrder = new int[slots];

        for (int i = 0; i < slots; ++i) {
            slotOrder[i] = i;
        }

        for (int i = slots - 1; i > 0; --i) {
            int j = random.nextInt(i + 1);
            int tmp = slotOrder[i];
            slotOrder[i] = slotOrder[j];
            slotOrder[j] = tmp;
        }

        for (int p = 0; p < amount && p < slots; ++p) {
            ItemStack drop = kind == 0 ? BronzeDungeon.getBronzeLoot(random)
                : kind == 1 ? ComponentSilverDungeon.getSilverLoot(random) : ComponentGoldenDungeon.getGoldLoot(random);

            this.setInventorySlotContents(slotOrder[p], drop);
        }

        this.locked = false;

        if (!this.worldObj.isRemote) {
            this.sendToAllInOurWorld(this.getDescriptionPacket());
        }
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }

    @Override
    public Packet getDescriptionPacket() {
        // Only the lock state and dungeon type are needed on the client (the
        // GUI uses getKind() for its background). Sending the full NBT here
        // would leak the chest's inventory contents to every client that loads
        // the chunk (an x-ray vector vanilla chests avoid by not overriding
        // getDescriptionPacket at all). Contents are synced through the
        // container window instead, as in vanilla.
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setBoolean("locked", this.locked);
        nbt.setInteger("dungeonType", this.kind);

        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbt);
    }

    @Override
    public void openInventory() {
        super.openInventory();
    }

    @Override
    public void closeInventory() {
        --this.numPlayersUsing;
        this.worldObj
            .addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType(), 1, this.numPlayersUsing);
        this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
        this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord - 1, this.zCoord, this.getBlockType());
    }

    private void sendToAllInOurWorld(Packet pkt) {
        ServerConfigurationManager scm = FMLCommonHandler.instance()
            .getMinecraftServerInstance()
            .getConfigurationManager();

        for (Object obj : scm.playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) obj;

            if (this.worldObj == player.worldObj) {
                player.playerNetServerHandler.sendPacket(pkt);
            }
        }
    }

    public boolean isLocked() {
        return this.locked;
    }

    public int getKind() {
        return this.kind;
    }

}
