package com.gildedgames.the_aether.compatibility.nei;

import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.Tags;
import com.gildedgames.the_aether.blocks.BlocksAether;
import com.gildedgames.the_aether.client.gui.GuiEnchanter;
import com.gildedgames.the_aether.client.gui.GuiFreezer;
import com.gildedgames.the_aether.client.gui.GuiIncubator;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.HandlerInfo;
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
        API.registerGuiOverlay(GuiEnchanter.class, "aether_enchanter");
        API.registerGuiOverlayHandler(GuiEnchanter.class, new AetherMachineOverlayHandler(), "aether_enchanter");
        registerHandlerInfo(AetherEnchanterRecipeHandler.class, new ItemStack(BlocksAether.enchanter));

        AetherFreezerRecipeHandler freezer = new AetherFreezerRecipeHandler();
        API.registerRecipeHandler(freezer);
        API.registerUsageHandler(freezer);
        API.addRecipeCatalyst(new ItemStack(BlocksAether.freezer), freezer);
        API.registerGuiOverlay(GuiFreezer.class, "aether_freezer");
        API.registerGuiOverlayHandler(GuiFreezer.class, new AetherMachineOverlayHandler(), "aether_freezer");
        registerHandlerInfo(AetherFreezerRecipeHandler.class, new ItemStack(BlocksAether.freezer));

        AetherIncubatorRecipeHandler incubator = new AetherIncubatorRecipeHandler();
        API.registerRecipeHandler(incubator);
        API.registerUsageHandler(incubator);
        API.addRecipeCatalyst(new ItemStack(BlocksAether.incubator), incubator);
        API.registerGuiOverlay(GuiIncubator.class, "aether_incubator");
        API.registerGuiOverlayHandler(GuiIncubator.class, new AetherMachineOverlayHandler(), "aether_incubator");
        registerHandlerInfo(AetherIncubatorRecipeHandler.class, new ItemStack(BlocksAether.incubator));
    }

    private static void registerHandlerInfo(Class<?> handlerClass, ItemStack icon) {
        HandlerInfo info = new HandlerInfo.Builder(handlerClass.getName(), "Aether Legacy", "aether_legacy")
            .setDisplayStack(icon)
            .setWidth(166)
            .setHeight(65)
            .setMultipleWidgetsAllowed(false)
            .build();

        GuiRecipeTab.handlerMap.put(handlerClass.getName(), info);
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
