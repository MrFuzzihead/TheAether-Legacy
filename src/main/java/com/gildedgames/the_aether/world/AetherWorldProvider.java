package com.gildedgames.the_aether.world;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.client.IRenderHandler;

import com.gildedgames.the_aether.AetherConfig;
import com.gildedgames.the_aether.network.AetherNetwork;
import com.gildedgames.the_aether.network.packets.PacketSendTime;
import com.gildedgames.the_aether.player.PlayerAether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class AetherWorldProvider extends WorldProvider {

    private float[] colorsSunriseSunset = new float[4];

    private boolean eternalDay;
    private boolean shouldCycleCatchup;
    private long aetherTime = 6000;

    // Change-detection caches so the server only broadcasts the eternal-day
    // state when it actually changes (it nearly never does) instead of every
    // tick.
    private boolean eternalDaySynced;
    private boolean lastSentEternalDay;
    private boolean shouldCycleSynced;
    private boolean lastSentShouldCycle;

    // Throttles the aether-time sync packet to at most once per second.
    private long lastSentAetherTime = -1;
    private long lastTimeSyncTick = Long.MIN_VALUE;

    public AetherWorldProvider() {
        super();
    }

    @Override
    protected void registerWorldChunkManager() {
        this.worldChunkMgr = new WorldChunkManagerAether();
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        if (!AetherConfig.eternalDayDisabled()) {
            if (!this.worldObj.isRemote) {
                AetherData data = AetherData.getInstance(this.worldObj);

                if (data.isEternalDay()) {
                    long currentAether = data.getAetherTime();

                    if (!data.isShouldCycleCatchup()) {
                        // (worldTime +/- 1) % 24000 must be parenthesized; the
                        // % operator binds tighter than +/-, so the original
                        // code compared against worldTime+1 / worldTime-1
                        // (unmodded), breaking the intended +/-1 tolerance.
                        if (currentAether != Math.floorMod(worldTime, 24000L)
                            && currentAether != Math.floorMod(worldTime + 1L, 24000L)
                            && currentAether != Math.floorMod(worldTime - 1L, 24000L)) {
                            data.setAetherTime(Math.floorMod(currentAether - 1L, 24000L));
                        } else {
                            data.setShouldCycleCatchup(true);
                        }
                    } else {
                        data.setAetherTime(worldTime);
                    }

                    this.aetherTime = data.getAetherTime();

                    // Sync the aether time to the dimension's clients at most
                    // once per 20 ticks (once per second); a second of latency
                    // is imperceptible on a 20-minute day/night cycle. The
                    // joining-player sync happens in the dimension-change
                    // handler.
                    long totalTime = this.worldObj.getTotalWorldTime();

                    if (this.aetherTime != this.lastSentAetherTime && totalTime - this.lastTimeSyncTick >= 20L) {
                        this.lastSentAetherTime = this.aetherTime;
                        this.lastTimeSyncTick = totalTime;
                        AetherNetwork
                            .sendToDimension(new PacketSendTime(this.aetherTime), AetherConfig.getAetherDimensionID());
                    }
                } else if (data.getAetherTime() != 6000L) {
                    data.setAetherTime(6000L);
                }
            }
        }

        int i = (int) (AetherConfig.eternalDayDisabled() ? worldTime : this.aetherTime % 24000L);

        float f = ((float) i + partialTicks) / 24000.0F - 0.25F;

        if (f < 0.0F) {
            ++f;
        }

        if (f > 1.0F) {
            --f;
        }

        float f1 = 1.0F - (float) ((Math.cos((double) f * Math.PI) + 1.0D) / 2.0D);
        f = f + (f1 - f) / 3.0F;
        return f;
    }

    public void setIsEternalDay(boolean set) {
        this.eternalDay = set;
    }

    /**
     * Returns true (once) when the eternal-day value changes from what was
     * last broadcast, so the server only re-broadcasts it on change.
     */
    public boolean needsEternalDaySync(boolean value) {
        if (!this.eternalDaySynced || value != this.lastSentEternalDay) {
            this.lastSentEternalDay = value;
            this.eternalDaySynced = true;

            return true;
        }

        return false;
    }

    /**
     * Returns true (once) when the should-cycle-catchup value changes from
     * what was last broadcast.
     */
    public boolean needsShouldCycleSync(boolean value) {
        if (!this.shouldCycleSynced || value != this.lastSentShouldCycle) {
            this.lastSentShouldCycle = value;
            this.shouldCycleSynced = true;

            return true;
        }

        return false;
    }

    public boolean getIsEternalDay() {
        return this.eternalDay;
    }

    public void setShouldCycleCatchup(boolean set) {
        this.shouldCycleCatchup = set;
    }

    public boolean getShouldCycleCatchup() {
        return this.shouldCycleCatchup;
    }

    public void setAetherTime(long time) {
        this.aetherTime = time;
    }

    public long getAetherTime() {
        return this.aetherTime;
    }

    @Override
    public float[] calcSunriseSunsetColors(float f, float f1) {
        float f2 = 0.4F;
        float f3 = MathHelper.cos(f * 3.141593F * 2.0F) - 0.0F;
        float f4 = -0F;

        if (f3 >= f4 - f2 && f3 <= f4 + f2) {
            float f5 = (f3 - f4) / f2 * 0.5F + 0.5F;
            float f6 = 1.0F - (1.0F - MathHelper.sin(f5 * 3.141593F)) * 0.99F;
            f6 *= f6;
            this.colorsSunriseSunset[0] = f5 * 0.3F + 0.1F;
            this.colorsSunriseSunset[1] = f5 * f5 * 0.7F + 0.2F;
            this.colorsSunriseSunset[2] = f5 * f5 * 0.7F + 0.2F;
            this.colorsSunriseSunset[3] = f6;
            return this.colorsSunriseSunset;
        } else {
            return null;
        }
    }

    @Override
    public int getRespawnDimension(EntityPlayerMP player) {
        return PlayerAether.get(player)
            .getBedLocation() == null ? 0 : AetherConfig.getAetherDimensionID();
    }

    @Override
    public boolean canCoordinateBeSpawn(int i, int j) {
        return false;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public IChunkProvider createChunkGenerator() {
        return new ChunkProviderAether(this.worldObj, this.worldObj.getSeed());
    }

    public boolean canDoLightning(Chunk chunk) {
        return false;
    }

    public boolean canDoRainSnowIce(Chunk chunk) {
        return false;
    }

    @Override
    public Vec3 getFogColor(float f, float f1) {
        int i = 0x9393BC;

        float f2 = MathHelper.cos(f * 3.141593F * 2.0F) * 2.0F + 0.5F;
        if (f2 < 0.0F) {
            f2 = 0.0F;
        }
        if (f2 > 1.0F) {
            f2 = 1.0F;
        }
        float f3 = (i >> 16 & 0xff) / 255F;
        float f4 = (i >> 8 & 0xff) / 255F;
        float f5 = (i & 0xff) / 255F;
        f3 *= f2 * 0.94F + 0.06F;
        f4 *= f2 * 0.94F + 0.06F;
        f5 *= f2 * 0.91F + 0.09F;

        return Vec3.createVectorHelper(f3, f4, f5);
    }

    @Override
    public String getSaveFolder() {
        return "Dim-Aether";
    }

    @Override
    public double getVoidFogYFactor() {
        return 100;
    }

    @Override
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }

    @Override
    public boolean isSkyColored() {
        return false;
    }

    @Override
    public double getHorizon() {
        return 0.0;
    }

    @Override
    public float getCloudHeight() {
        return -5F;
    }

    @Override
    public String getDimensionName() {
        return "the_aether";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean getWorldHasVoidParticles() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public net.minecraftforge.client.IRenderHandler getWeatherRenderer() {
        return new IRenderHandler() {

            @Override
            public void render(float partialTicks, net.minecraft.client.multiplayer.WorldClient world,
                net.minecraft.client.Minecraft mc) {

            }
        };
    }
}
