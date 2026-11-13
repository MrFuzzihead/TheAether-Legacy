package com.gildedgames.the_aether.compatibility.nei;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.api.freezables.AetherFreezable;
import com.gildedgames.the_aether.blocks.BlocksAether;
import com.gildedgames.the_aether.client.gui.GuiFreezer;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class AetherFreezerRecipeHandler extends TemplateRecipeHandler {

    private static final String GUI_TEXTURE = "aether_legacy:textures/gui/altar.png";

    @Override
    public String getRecipeName() {
        return I18n.format("container.aether_legacy.freezer");
    }

    @Override
    public String getGuiTexture() {
        return GUI_TEXTURE;
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiFreezer.class;
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(74, 23, 24, 18), "aether_freezer"));
    }

    @Override
    public String getOverlayIdentifier() {
        return "aether_freezer";
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (AetherFreezable recipe : AetherAPI.instance()
            .getFreezableValues()) {
            if (NEIServerUtils.areStacksSameTypeCrafting(recipe.getOutput(), result)) {
                arecipes.add(new FreezerPair(recipe));
            }
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals("aether_freezer")) {
            for (AetherFreezable recipe : AetherAPI.instance()
                .getFreezableValues()) {
                arecipes.add(new FreezerPair(recipe));
            }
            return;
        }

        super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (ingredient == null) {
            return;
        }

        if (AetherAPI.instance()
            .isFreezableFuel(ingredient)) {
            loadCraftingRecipes("aether_freezer");
            return;
        }

        for (AetherFreezable recipe : AetherAPI.instance()
            .getFreezableValues()) {
            if (NEIServerUtils.areStacksSameTypeCrafting(recipe.getInput(), ingredient)) {
                arecipes.add(new FreezerPair(recipe));
            }
        }
    }

    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients) {
        if (inputId.equals("aether_freezer")) {
            loadCraftingRecipes("aether_freezer");
            return;
        }

        super.loadUsageRecipes(inputId, ingredients);
    }

    public class FreezerPair extends CachedRecipe {

        private final PositionedStack input;
        private final PositionedStack fuel;
        private final PositionedStack result;

        public FreezerPair(AetherFreezable recipe) {
            this.input = new PositionedStack(
                recipe.getInput()
                    .copy(),
                51,
                6);
            this.fuel = new PositionedStack(new ItemStack(BlocksAether.icestone), 51, 42);
            this.result = new PositionedStack(
                recipe.getOutput()
                    .copy(),
                111,
                24);
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return Arrays.asList(this.input, this.fuel);
        }

        @Override
        public PositionedStack getResult() {
            return this.result;
        }
    }
}
