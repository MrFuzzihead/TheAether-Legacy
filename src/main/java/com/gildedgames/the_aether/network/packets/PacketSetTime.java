package com.gildedgames.the_aether.network.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import com.gildedgames.the_aether.AetherConfig;
import com.gildedgames.the_aether.world.AetherData;

import cpw.mods.fml.common.FMLCommonHandler;
import io.netty.buffer.ByteBuf;

public class PacketSetTime extends AetherPacket<PacketSetTime> {

    public float timeVariable;

    public int dimensionId;

    public PacketSetTime() {

    }

    public PacketSetTime(float timeVariable, int dimensionId) {
        this.dimensionId = dimensionId;
        this.timeVariable = timeVariable;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.dimensionId = buf.readInt();
        this.timeVariable = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.dimensionId);
        buf.writeFloat(this.timeVariable);
    }

    @Override
    public void handleClient(PacketSetTime message, EntityPlayer player) {

    }

    @Override
    public void handleServer(PacketSetTime message, EntityPlayer player) {
        if (player == null || player.worldObj == null) {
            return;
        }

        // Only the Aether dimension may be changed, and only by players in it.
        // The player's world on the server is authoritative (never trust the
        // client-sent dimensionId).
        if (player.dimension != AetherConfig.getAetherDimensionID()
            || player.worldObj.provider.dimensionId != AetherConfig.getAetherDimensionID()) {
            return;
        }

        // Time control must be unlocked by defeating the Sun Spirit (gold
        // dungeon boss): eternal day active. The altar GUI itself only opens
        // after the cycle catch-up completes (see BlockSunAltar), so the
        // packet need not re-check shouldCycleCatchup here — requiring it made
        // the slider silently reject while the catch-up was still running
        // (which, with the fixed catch-up math, now completes in seconds).
        if (AetherConfig.eternalDayDisabled()) {
            return;
        }

        AetherData data = AetherData.getInstance(player.worldObj);

        if (!data.isEternalDay()) {
            return;
        }

        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();

        // Same permission rules as the Sun Altar block: ops, or everyone when
        // the multiplayer config allows it (dedicated servers only restrict
        // otherwise). Integrated servers always allow.
        boolean permitted = !server.isDedicatedServer() || server.getConfigurationManager()
            .func_152596_g(player.getGameProfile()) || AetherConfig.sunAltarMultiplayer();

        if (!permitted) {
            return;
        }

        setTime(message.timeVariable, (WorldServer) player.worldObj);
    }

    /**
     * Snaps the time-of-day of {@code world} (the server player's live Aether
     * world) to the slider value. Targeting the player's own world is
     * important: dimension lookups via the dimension id can return a
     * stale/secondary WorldServer whose WorldInfo is not the one the world
     * tick and sky rendering actually read.
     */
    public void setTime(float sliderValue, WorldServer aetherServer) {
        long shouldTime = (long) (24000L * sliderValue);
        long worldTime = aetherServer.getWorldInfo()
            .getWorldTime();
        long remainder = worldTime % 24000L;
        long add = shouldTime > remainder ? shouldTime - remainder : shouldTime + 24000 - remainder;

        // Best effort on the world's own clock (helps F3 / S03 sync where it
        // works).
        aetherServer.setWorldTime(worldTime + add);

        // The authoritative Aether sky time lives in AetherData; write the
        // requested time-of-day there directly so the next tick's sky update
        // reflects it (in this environment the WorldInfo write does not stick,
        // so this is the layer that actually drives the sun).
        AetherData.getInstance(aetherServer)
            .setAetherTime(shouldTime % 24000L);
    }

}
