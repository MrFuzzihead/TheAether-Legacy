package com.gildedgames.the_aether.mixins.early;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gildedgames.the_aether.client.AetherClientEvents;
import com.gildedgames.the_aether.client.gui.button.GuiAccessoryButton;

@Mixin(GuiContainerCreative.class)
public abstract class GuiContainerCreativeMixin extends GuiContainer {

    protected GuiContainerCreativeMixin(Container container) {
        super(container);
    }

    @Inject(method = "setCurrentCreativeTab", at = @At("TAIL"), require = 1)
    private void aether$updateAccessoryButton(CreativeTabs tab, CallbackInfo callback) {
        GuiAccessoryButton button = AetherClientEvents.getAccessoryButton();

        if (tab == CreativeTabs.tabInventory) {
            if (!this.buttonList.contains(button)) {
                this.buttonList.add(button.setPosition((this.width - 195) / 2 + 28, (this.height - 136) / 2 + 38));
            }
        } else {
            this.buttonList.remove(button);
        }
    }
}
