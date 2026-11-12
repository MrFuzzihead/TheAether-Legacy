package com.gildedgames.the_aether.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.resources.IResourceManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gildedgames.the_aether.client.renders.AetherEntityRenderer;
import com.gildedgames.the_aether.client.renders.AetherItemRenderer;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Mutable
    @Shadow
    @Final
    public ItemRenderer itemRenderer;

    @Inject(method = "<init>", at = @At("TAIL"), require = 1)
    private void aether$useAetherItemRenderer(Minecraft mc, IResourceManager resourceManager, CallbackInfo callback) {
        if ((Object) this instanceof AetherEntityRenderer) {
            this.itemRenderer = new AetherItemRenderer(mc);
        }
    }
}
