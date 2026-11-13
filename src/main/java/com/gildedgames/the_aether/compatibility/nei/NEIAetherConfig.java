package com.gildedgames.the_aether.compatibility.nei;

import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.Tags;
import com.gildedgames.the_aether.blocks.BlocksAether;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class NEIAetherConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        AetherEnchanterRecipeHandler enchanter = new AetherEnchanterRecipeHandler();
        API.registerRecipeHandler(enchanter);
        API.registerUsageHandler(enchanter);
        API.addRecipeCatalyst(new ItemStack(BlocksAether.enchanter), enchanter);

        AetherFreezerRecipeHandler freezer = new AetherFreezerRecipeHandler();
        API.registerRecipeHandler(freezer);
        API.registerUsageHandler(freezer);
        API.addRecipeCatalyst(new ItemStack(BlocksAether.freezer), freezer);
    }

    @Override
    public String getName() {
        return "Aether Legacy NEI Integration";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
