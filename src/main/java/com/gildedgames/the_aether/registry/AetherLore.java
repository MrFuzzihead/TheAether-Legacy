package com.gildedgames.the_aether.registry;

import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.items.block.ItemBlockEnchanter;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

public class AetherLore {

    public static String getLoreEntryKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }

        UniqueIdentifier identifier;
        try {
            identifier = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        } catch (RuntimeException exception) {
            // GameRegistry throws for some unregistered/custom items instead
            // of returning null. An unknown item simply has no lore entry.
            return null;
        }

        if (identifier == null) {
            return null;
        }

        if (stack.getItem() instanceof ItemBlockEnchanter) {
            return "lore." + identifier.modId + ".enchanter";
        }

        return "lore." + identifier.modId
            + "."
            + stack.getUnlocalizedName()
                .replace("item.", "")
                .replace("tile.", "")
                .replace(".name", "")
                .replace(".", "_");
    }
}
