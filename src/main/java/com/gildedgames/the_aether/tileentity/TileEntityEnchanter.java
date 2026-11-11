package com.gildedgames.the_aether.tileentity;

import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.api.enchantments.AetherEnchantment;
import com.gildedgames.the_aether.api.events.AetherHooks;
import com.gildedgames.the_aether.blocks.BlocksAether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityEnchanter extends TileEntityAetherProcessor<AetherEnchantment> {

    public TileEntityEnchanter() {
        super("Altar");
    }

    @Override
    protected AetherEnchantment getRecipe(ItemStack stack) {
        return AetherAPI.instance()
            .getEnchantment(stack);
    }

    @Override
    protected ItemStack getRecipeOutput(AetherEnchantment recipe) {
        return recipe.getOutput();
    }

    @Override
    protected int getRecipeTime(AetherEnchantment recipe) {
        return recipe.getTimeRequired();
    }

    @Override
    protected int getRequiredProgress(AetherEnchantment recipe) {
        return this.ticksRequired;
    }

    @Override
    protected boolean isFuel(ItemStack stack) {
        return AetherAPI.instance()
            .isEnchantmentFuel(stack);
    }

    @Override
    protected int getFuelTime(ItemStack stack) {
        return AetherAPI.instance()
            .getEnchantmentFuel(stack)
            .getTimeGiven();
    }

    @Override
    protected boolean hasBonusBlock() {
        return this.worldObj.getBlock(this.xCoord, this.yCoord - 1, this.zCoord) == BlocksAether.enchanted_gravitite;
    }

    @Override
    protected int getBonusProgress() {
        return 2;
    }

    @Override
    protected int onSetTime(AetherEnchantment recipe, int time) {
        return AetherHooks.onSetEnchantmentTime(this, recipe, time);
    }

    @Override
    protected void onProcessed(AetherEnchantment recipe) {
        AetherHooks.onItemEnchant(this, recipe);
    }

    @Override
    public boolean isActive() {
        return this.powerRemaining > 0;
    }

    @SideOnly(Side.CLIENT)
    public int getEnchantmentProgressScaled(int scale) {
        return this.getProgressScaled(scale);
    }

    @SideOnly(Side.CLIENT)
    public int getEnchantmentTimeRemaining(int scale) {
        return this.getTimeRemainingScaled(scale);
    }

    public boolean isEnchanting() {
        return this.isActive();
    }

    @Override
    public boolean isValidSlotItem(int slot, ItemStack stackInSlot) {
        if (slot == 2) {
            return false;
        } else if (slot == 1 && AetherAPI.instance()
            .isEnchantmentFuel(stackInSlot)) {
                return true;
            } else if (slot == 0 && AetherAPI.instance()
                .hasEnchantment(stackInSlot)) {
                    return true;
                }

        return false;
    }
}
