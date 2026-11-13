package com.gildedgames.the_aether.compatibility.nei;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.api.moa.AetherMoaType;
import com.gildedgames.the_aether.blocks.BlocksAether;
import com.gildedgames.the_aether.client.gui.GuiIncubator;
import com.gildedgames.the_aether.entities.passive.mountable.EntityMoa;
import com.gildedgames.the_aether.items.ItemMoaEgg;
import com.gildedgames.the_aether.items.ItemsAether;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class AetherIncubatorRecipeHandler extends TemplateRecipeHandler {

    private static final String GUI_TEXTURE = "aether_legacy:textures/gui/incubator.png";

    @Override
    public String getRecipeName() {
        return I18n.format("container.aether_legacy.incubator");
    }

    @Override
    public String getGuiTexture() {
        return GUI_TEXTURE;
    }

    @Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiIncubator.class;
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(74, 23, 24, 18), "aether_incubator"));
    }

    @Override
    public String getOverlayIdentifier() {
        return "aether_incubator";
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (AetherMoaType type : AetherAPI.instance()
            .getMoaTypeValues()) {
            ItemStack egg = ItemMoaEgg.getStackFromType(type);

            if (result != null && result.getItem() == egg.getItem()) {
                arecipes.add(new IncubatorPair(egg));
            }
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals("aether_incubator")) {
            for (AetherMoaType type : AetherAPI.instance()
                .getMoaTypeValues()) {
                arecipes.add(new IncubatorPair(ItemMoaEgg.getStackFromType(type)));
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

        if (ingredient.getItem() == ItemsAether.moa_egg
            || ingredient.getItem() == net.minecraft.item.Item.getItemFromBlock(BlocksAether.ambrosium_torch)) {
            loadCraftingRecipes("aether_incubator");
        }
    }

    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients) {
        if (inputId.equals("aether_incubator")) {
            loadCraftingRecipes("aether_incubator");
            return;
        }

        super.loadUsageRecipes(inputId, ingredients);
    }

    @Override
    public void drawExtras(int recipe) {
        IncubatorPair pair = (IncubatorPair) arecipes.get(recipe);

        // Flame animation from the incubator GUI.
        int flame = (cycleticks % 40) * 13 / 40;
        GuiDraw.drawTexturedModalRect(69, 36 - flame, 176, 12 - flame, 14, flame + 2);

        // Growing progress bar from the incubator GUI.
        int progress = (cycleticks % 80) * 54 / 80;
        GuiDraw.drawTexturedModalRect(98, 59 - progress, 179, 70 - progress, 10, progress);

        GuiDraw.drawString(pair.typeLabel + " - " + I18n.format("gui.incubator.moa_hatches"), 8, 56, 0x404040, false);

        pair.renderMoa(122, 58);
    }

    public class IncubatorPair extends CachedRecipe {

        private final PositionedStack egg;
        private final PositionedStack fuel;
        private final String typeLabel;
        private final AetherMoaType type;
        private EntityMoa previewMoa;

        public IncubatorPair(ItemStack egg) {
            this.egg = new PositionedStack(egg, 68, 6);
            this.fuel = new PositionedStack(new ItemStack(BlocksAether.ambrosium_torch), 68, 42);
            this.typeLabel = egg.getDisplayName();
            this.type = ((ItemMoaEgg) egg.getItem()).getMoaTypeFromItemStack(egg);
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return Arrays.asList(this.egg, this.fuel);
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }

        private void renderMoa(int x, int y) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || this.type == null) {
                return;
            }

            if (this.previewMoa == null || this.previewMoa.worldObj != mc.theWorld) {
                this.previewMoa = new EntityMoa(mc.theWorld, this.type);
                this.previewMoa.setGrowingAge(0);
                this.previewMoa.setPosition(0.0D, 0.0D, 0.0D);
            }

            float time = (cycleticks % 80) / 20.0F;
            this.previewMoa.rotationYaw = (cycleticks / 2.0F) % 360.0F;
            this.previewMoa.prevRotationYaw = this.previewMoa.rotationYaw;
            this.previewMoa.renderYawOffset = this.previewMoa.rotationYaw;
            this.previewMoa.prevRenderYawOffset = this.previewMoa.rotationYaw;
            this.previewMoa.ticksExisted = cycleticks / 2;
            this.previewMoa.onGround = true;
            this.previewMoa.limbSwing = time;
            this.previewMoa.limbSwingAmount = 0.25F;
            this.previewMoa.prevLimbSwingAmount = 0.25F;
            this.previewMoa.wingRotation = time;
            this.previewMoa.prevWingRotation = time;
            this.previewMoa.destPos = 1.0F;
            this.previewMoa.prevDestPos = 1.0F;

            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 50.0F);
            GL11.glScalef(-11.0F, 11.0F, 11.0F);
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
            RenderHelper.enableStandardItemLighting();
            RenderManager.instance.renderEntityWithPosYaw(this.previewMoa, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
            RenderHelper.disableStandardItemLighting();
            GL11.glPopMatrix();
        }
    }
}
