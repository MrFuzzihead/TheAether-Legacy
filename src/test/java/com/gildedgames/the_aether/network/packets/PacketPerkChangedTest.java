package com.gildedgames.the_aether.network.packets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gildedgames.the_aether.player.PlayerAether;
import com.gildedgames.the_aether.player.perks.AetherRankings;
import com.gildedgames.the_aether.player.perks.util.EnumAetherPerkType;

public class PacketPerkChangedTest {

    private static final UUID RANKED = UUID.fromString("5f820c39-5883-4392-b174-3125ac05e38c");

    private static final UUID DEVELOPER = UUID.fromString("6a0e8505-1556-4ee9-bec0-6af32f05888d");

    private static final UUID UNRANKED = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeClass
    public static void initializeRanks() {
        AetherRankings.initialization();
    }

    private static PlayerAether player(UUID uuid, boolean donator) {
        PlayerAether player = mock(PlayerAether.class);
        EntityPlayer entity = mock(EntityPlayer.class);

        when(player.getEntity()).thenReturn(entity);
        when(entity.getUniqueID()).thenReturn(uuid);
        when(player.isDonator()).thenReturn(donator);

        return player;
    }

    @Test
    public void haloRequiresRankedPlayer() {
        assertTrue(PacketPerkChanged.canUsePerk(player(RANKED, false), EnumAetherPerkType.Halo));
        assertFalse(PacketPerkChanged.canUsePerk(player(UNRANKED, false), EnumAetherPerkType.Halo));
    }

    @Test
    public void glowRequiresDeveloper() {
        assertTrue(PacketPerkChanged.canUsePerk(player(DEVELOPER, false), EnumAetherPerkType.Glow));
        assertFalse(PacketPerkChanged.canUsePerk(player(RANKED, false), EnumAetherPerkType.Glow));
    }

    @Test
    public void moaSkinRequiresDonatorOrRankedPlayer() {
        assertTrue(PacketPerkChanged.canUsePerk(player(UNRANKED, true), EnumAetherPerkType.Moa));
        assertTrue(PacketPerkChanged.canUsePerk(player(RANKED, false), EnumAetherPerkType.Moa));
        assertFalse(PacketPerkChanged.canUsePerk(player(UNRANKED, false), EnumAetherPerkType.Moa));
    }

    @Test
    public void informationPerkCannotBeChangedByPacket() {
        assertFalse(PacketPerkChanged.canUsePerk(player(RANKED, true), EnumAetherPerkType.Information));
        assertFalse(PacketPerkChanged.canUsePerk(null, EnumAetherPerkType.Halo));
    }
}
