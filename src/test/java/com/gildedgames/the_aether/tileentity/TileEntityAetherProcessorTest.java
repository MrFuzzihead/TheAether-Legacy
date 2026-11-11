package com.gildedgames.the_aether.tileentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.Test;

import com.gildedgames.the_aether.tileentity.util.AetherTileEntity;

public class TileEntityAetherProcessorTest {

    private static final class TestProcessor extends TileEntityAetherProcessor<String> {

        private final Item input;

        private final Item fuel;

        private final Item output;

        private boolean processed;

        private TestProcessor(Item input, Item fuel, Item output) {
            super("test");

            this.input = input;
            this.fuel = fuel;
            this.output = output;
        }

        @Override
        protected String getRecipe(ItemStack stack) {
            return stack.getItem() == this.input ? "recipe" : null;
        }

        @Override
        protected ItemStack getRecipeOutput(String recipe) {
            return new ItemStack(this.output);
        }

        @Override
        protected int getRecipeTime(String recipe) {
            return 1;
        }

        @Override
        protected int getRequiredProgress(String recipe) {
            return 1;
        }

        @Override
        protected boolean isFuel(ItemStack stack) {
            return stack.getItem() == this.fuel;
        }

        @Override
        protected int getFuelTime(ItemStack stack) {
            return 10;
        }

        @Override
        protected boolean hasBonusBlock() {
            return false;
        }

        @Override
        protected int getBonusProgress() {
            return 2;
        }

        @Override
        protected int onSetTime(String recipe, int time) {
            return time;
        }

        @Override
        protected void onProcessed(String recipe) {
            this.processed = true;
        }

        @Override
        public boolean isActive() {
            return this.powerRemaining > 0;
        }

        @Override
        public boolean isValidSlotItem(int slot, ItemStack stackInSlot) {
            return true;
        }
    }

    @Test
    public void processorConsumesFuelAndProducesTheRecipeOutput() {
        Item input = new Item();
        Item fuel = new Item();
        Item output = new Item();
        TestProcessor processor = new TestProcessor(input, fuel, output);

        World world = mock(World.class);
        processor.setWorldObj(world);

        processor.setInventorySlotContents(0, new ItemStack(input));
        processor.setInventorySlotContents(1, new ItemStack(fuel));

        processor.updateEntity(); // discovers the recipe
        processor.updateEntity(); // consumes fuel
        processor.updateEntity(); // advances progress and finishes

        assertTrue(processor.processed);
        assertNull(processor.getStackInSlot(0));
        assertEquals(
            output,
            processor.getStackInSlot(2)
                .getItem());
        assertNull(processor.getStackInSlot(1));
    }

    @Test
    public void processorIsStillAnAetherTileEntity() {
        assertTrue(new TestProcessor(new Item(), new Item(), new Item()) instanceof AetherTileEntity);
    }
}
