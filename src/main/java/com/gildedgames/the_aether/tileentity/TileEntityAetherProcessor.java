package com.gildedgames.the_aether.tileentity;

import java.util.List;
import java.util.Map;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gildedgames.the_aether.blocks.container.BlockAetherContainer;
import com.gildedgames.the_aether.tileentity.util.AetherTileEntity;
import com.gildedgames.the_aether.util.FilledList;

/**
 * Shared processing loop for the Enchanter and Freezer. Subclasses supply the
 * recipe/fuel registry hooks; the progress, power, NBT and output-merging rules
 * live here so the two blocks cannot drift apart.
 */
public abstract class TileEntityAetherProcessor<T> extends AetherTileEntity {

    public int progress, ticksRequired, powerRemaining;

    protected final FilledList<ItemStack> itemStacks = new FilledList<ItemStack>(3, null);

    private T currentRecipe;

    protected TileEntityAetherProcessor(String name) {
        super(name);
    }

    protected abstract T getRecipe(ItemStack stack);

    protected abstract ItemStack getRecipeOutput(T recipe);

    protected abstract int getRecipeTime(T recipe);

    /**
     * Progress required to finish the current recipe. The Freezer historically
     * compares against the raw recipe time rather than {@link #ticksRequired};
     * keeping this as a hook preserves that behavior.
     */
    protected abstract int getRequiredProgress(T recipe);

    protected abstract boolean isFuel(ItemStack stack);

    protected abstract int getFuelTime(ItemStack stack);

    protected abstract boolean hasBonusBlock();

    protected abstract int getBonusProgress();

    protected abstract int onSetTime(T recipe, int time);

    protected abstract void onProcessed(T recipe);

    public abstract boolean isActive();

    @Override
    public List<ItemStack> getTileInventory() {
        return this.itemStacks;
    }

    @Override
    public void onSlotChanged(int index) {

    }

    @Override
    public void updateEntity() {
        boolean wasActive = this.isActive();

        if (this.powerRemaining > 0) {
            this.powerRemaining--;

            if (this.currentRecipe != null) {
                this.progress += this.hasBonusBlock() ? this.getBonusProgress() : 1;
            }
        }

        if (this.currentRecipe != null) {
            if (this.progress >= this.getRequiredProgress(this.currentRecipe)) {
                if (!this.worldObj.isRemote) {
                    this.finishRecipe(this.currentRecipe);
                }

                this.progress = 0;
            }

            if (this.getStackInSlot(0) == null || this.getRecipe(this.getStackInSlot(0)) != this.currentRecipe) {
                this.currentRecipe = null;
                this.progress = 0;
            }

            if (this.powerRemaining <= 0) {
                if (this.getStackInSlot(1) != null && this.isFuel(this.getStackInSlot(1))) {
                    this.powerRemaining += this.getFuelTime(this.getStackInSlot(1));

                    if (!this.worldObj.isRemote) {
                        this.decrStackSize(1, 1);
                    }
                } else {
                    this.currentRecipe = null;
                    this.progress = 0;
                }
            }
        } else if (this.getStackInSlot(0) != null) {
            T recipe = this.getRecipe(this.getStackInSlot(0));

            if (recipe != null) {
                ItemStack output = this.getRecipeOutput(recipe);
                ItemStack currentOutput = this.getStackInSlot(2);

                if (currentOutput == null || (output.getItem() == currentOutput.getItem()
                    && output.getItemDamage() == currentOutput.getItemDamage()
                    && currentOutput.isStackable()
                    && currentOutput.stackSize + output.stackSize <= output.getMaxStackSize())) {
                    this.currentRecipe = recipe;
                    this.ticksRequired = this.getRecipeTime(recipe);
                    this.addEnchantmentWeight(this.getStackInSlot(0));
                    this.ticksRequired = this.onSetTime(recipe, this.ticksRequired);
                }
            }
        }

        if (wasActive != this.isActive()) {
            this.markDirty();
            BlockAetherContainer.setState(this.worldObj, this.xCoord, this.yCoord, this.zCoord, this.isActive());
        }
    }

    private void finishRecipe(T recipe) {
        ItemStack input = this.getStackInSlot(0);
        ItemStack result = this.getRecipeOutput(recipe)
            .copy();

        EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(input), result);

        if (input.hasTagCompound()) {
            result.setTagCompound(input.getTagCompound());
        }

        if (this.getStackInSlot(2) != null && this.getStackInSlot(2)
            .isStackable()) {
            ItemStack merged = this.getStackInSlot(2)
                .copy();

            merged.stackSize += result.stackSize;

            this.setInventorySlotContents(2, merged);
        } else {
            this.setInventorySlotContents(2, result);
        }

        if (input.getItem()
            .hasContainerItem(input)) {
            this.setInventorySlotContents(
                0,
                input.getItem()
                    .getContainerItem(input));
        } else {
            this.decrStackSize(0, 1);
        }

        this.onProcessed(recipe);
    }

    protected void addEnchantmentWeight(ItemStack stack) {
        Map<Integer, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);

        if (!enchantments.isEmpty()) {
            for (int levels : enchantments.values()) {
                this.ticksRequired += (levels * 1250);
            }
        }
    }

    protected int getProgressScaled(int scale) {
        if (this.ticksRequired == 0) {
            return 0;
        }

        return (this.progress * scale) / this.ticksRequired;
    }

    protected int getTimeRemainingScaled(int scale) {
        return (this.powerRemaining * scale) / 500;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.progress = compound.getInteger("progress");
        this.powerRemaining = compound.getInteger("powerRemaining");
        this.ticksRequired = compound.getInteger("ticksRequired");
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("progress", this.progress);
        compound.setInteger("powerRemaining", this.powerRemaining);
        compound.setInteger("ticksRequired", this.ticksRequired);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 ? new int[] { 2 } : new int[] { 0, 1 };
    }
}
