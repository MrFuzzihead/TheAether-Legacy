package com.gildedgames.the_aether.items.weapons;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;

import org.junit.Test;

public class ItemVampireBladeTest {

    @Test
    public void nonPlayerAttackerDoesNotCrashOrHeal() {
        ItemVampireBlade blade = new ItemVampireBlade();
        ItemStack stack = new ItemStack(blade);

        EntityLivingBase target = mock(EntityLivingBase.class);
        EntityLivingBase attacker = mock(EntityLivingBase.class);

        assertTrue(blade.hitEntity(stack, target, attacker));
    }

    @Test
    public void playerAttackerHealsWhenDamaged() {
        ItemVampireBlade blade = new ItemVampireBlade();
        ItemStack stack = new ItemStack(blade);

        EntityLivingBase target = mock(EntityLivingBase.class);
        EntityPlayer player = mock(EntityPlayer.class);
        player.capabilities = new PlayerCapabilities();

        when(player.getHealth()).thenReturn(10.0F);
        when(player.getMaxHealth()).thenReturn(20.0F);

        assertTrue(blade.hitEntity(stack, target, player));
        verify(player).heal(1.0F);
    }
}
