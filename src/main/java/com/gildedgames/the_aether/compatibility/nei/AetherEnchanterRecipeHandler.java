package com.gildedgames.the_aether.compatibility.nei;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.api.enchantments.AetherEnchantment;
import com.gildedgames.the_aether.client.gui.GuiEnchanter;
import com.gildedgames.the_aether.items.ItemsAether;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class AetherEnchanterRecipeHandler extends TemplateRecipeHandler {

    private static final String GUI_TEXTURE = "aether_legacy:textures/gui/altar.png";

    @Override
    public String getRecipeName() {
        return I18n.format("container.aether_legacy.enchanter");
    }

    @Override
    public String getGuiTexture() {
        return GUI_TEXTURE;
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiEnchanter.class;
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(74, 23, 24, 18), "aether_enchanter"));
    }

    @Override
    public String getOverlayIdentifier() {
        return "aether_enchanter";
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (AetherEnchantment recipe : AetherAPI.instance()
            .getEnchantmentValues()) {
            if (NEIServerUtils.areStacksSameTypeCrafting(recipe.getOutput(), result)) {
                arecipes.add(new EnchanterPair(recipe));
            }
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals("aether_enchanter")) {
            for (AetherEnchantment recipe : AetherAPI.instance()
                .getEnchantmentValues()) {
                arecipes.add(new EnchanterPair(recipe));
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
            .isEnchantmentFuel(ingredient)) {
            loadCraftingRecipes("aether_enchanter");
            return;
        }

        for (AetherEnchantment recipe : AetherAPI.instance()
            .getEnchantmentValues()) {
            if (NEIServerUtils.areStacksSameTypeCrafting(recipe.getInput(), ingredient)) {
                arecipes.add(new EnchanterPair(recipe));
            }
        }
    }

    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients) {
        if (inputId.equals("aether_enchanter")) {
            loadCraftingRecipes("aether_enchanter");
            return;
        }

        super.loadUsageRecipes(inputId, ingredients);
    }

    public class EnchanterPair extends CachedRecipe {

        private final PositionedStack input;
        private final PositionedStack fuel;
        private final PositionedStack result;

        public EnchanterPair(AetherEnchantment recipe) {
            this.input = new PositionedStack(
                recipe.getInput()
                    .copy(),
                51,
                6);
            this.fuel = new PositionedStack(new ItemStack(ItemsAether.ambrosium_shard), 51, 42);
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
