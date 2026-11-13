package com.gildedgames.the_aether.compatibility.nei;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.DefaultOverlayHandler;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * Maps the first two machine slots to the recipe inputs. The enchanter, freezer,
 * and incubator all keep their two inputs in slots 0 and 1; the remaining slots
 * are output or player inventory.
 */
public class AetherMachineOverlayHandler extends DefaultOverlayHandler {

    @Override
    public Slot[][] mapIngredSlots(GuiContainer gui, List<PositionedStack> ingredients) {
        return new Slot[][] { { gui.inventorySlots.getSlot(0) }, { gui.inventorySlots.getSlot(1) } };
    }

    @Override
    protected Set<Slot> getCraftMatrixSlots(GuiContainer gui, IRecipeHandler handler) {
        Set<Slot> slots = new HashSet<Slot>();
        slots.add(gui.inventorySlots.getSlot(0));
        slots.add(gui.inventorySlots.getSlot(1));
        return slots;
    }
}
