package com.gildedgames.the_aether.compatibility.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;

import mods.battlegear2.client.utils.BattlegearRenderHelper;

/**
 * Keeps every Battlegear class reference out of the common item renderer.
 * This class is only loaded when Battlegear is present and its render hook is
 * actually invoked.
 */
public final class BattlegearRenderCompat {

    private BattlegearRenderCompat() {}

    public static void renderItemInFirstPerson(float partialTicks, Minecraft mc, ItemRenderer renderer) {
        BattlegearRenderHelper.renderItemInFirstPerson(partialTicks, mc, renderer);
    }
}
