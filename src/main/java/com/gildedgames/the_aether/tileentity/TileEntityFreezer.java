package com.gildedgames.the_aether.tileentity;

import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.api.events.AetherHooks;
import com.gildedgames.the_aether.api.freezables.AetherFreezable;
import com.gildedgames.the_aether.blocks.BlocksAether;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityFreezer extends TileEntityAetherProcessor<AetherFreezable> {

    public TileEntityFreezer() {
        super("Freezer");
    }

    @Override
    protected AetherFreezable getRecipe(ItemStack stack) {
        return AetherAPI.instance()
            .getFreezable(stack);
    }

    @Override
    protected ItemStack getRecipeOutput(AetherFreezable recipe) {
        return recipe.getOutput();
    }

    @Override
    protected int getRecipeTime(AetherFreezable recipe) {
        return recipe.getTimeRequired();
    }

    @Override
    protected int getRequiredProgress(AetherFreezable recipe) {
        return this.getRecipeTime(recipe);
    }

    @Override
    protected boolean isFuel(ItemStack stack) {
        return AetherAPI.instance()
            .isFreezableFuel(stack);
    }

    @Override
    protected int getFuelTime(ItemStack stack) {
        return AetherAPI.instance()
            .getFreezableFuel(stack)
            .getTimeGiven();
    }

    @Override
    protected boolean hasBonusBlock() {
        return this.worldObj.getBlock(this.xCoord, this.yCoord - 1, this.zCoord) == BlocksAether.icestone;
    }

    @Override
    protected int getBonusProgress() {
        return 2;
    }

    @Override
    protected int onSetTime(AetherFreezable recipe, int time) {
        return AetherHooks.onSetFreezableTime(this, recipe, time);
    }

    @Override
    protected void onProcessed(AetherFreezable recipe) {
        AetherHooks.onItemFreeze(this, recipe);
    }

    @Override
    public boolean isActive() {
        return this.powerRemaining > 0;
    }

    @SideOnly(Side.CLIENT)
    public int getFreezingProgressScaled(int scale) {
        return this.getProgressScaled(scale);
    }

    @SideOnly(Side.CLIENT)
    public int getFreezingTimeRemaining(int scale) {
        return this.getTimeRemainingScaled(scale);
    }

    public boolean isFreezing() {
        return this.isActive();
    }

    @Override
    public boolean isValidSlotItem(int slot, ItemStack stackInSlot) {
        if (stackInSlot != null) {
            if (AetherAPI.instance()
                .hasFreezable(stackInSlot)) {
                return true;
            } else if (slot == 1 && AetherAPI.instance()
                .isFreezableFuel(stackInSlot)) {
                    return true;
                }
        }

        return false;
    }
}
