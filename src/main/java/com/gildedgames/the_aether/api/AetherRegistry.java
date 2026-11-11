package com.gildedgames.the_aether.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Typed registry with the key rules used by the Aether API. Item-backed
 * registries use damage only for non-damageable stacks, so tools/armor share a
 * single entry regardless of durability.
 */
public final class AetherRegistry<T extends RegistryEntry> {

    private final Map<ResourceLocation, T> entries;

    public AetherRegistry(boolean ordered) {
        this.entries = ordered ? new LinkedHashMap<ResourceLocation, T>() : new HashMap<ResourceLocation, T>();
    }

    public static ResourceLocation key(ItemStack stack) {
        return new ResourceLocation(
            stack.getItem()
                .getUnlocalizedName()
                .toString() + "_meta_"
                + (stack.isItemStackDamageable() ? 0 : stack.getItemDamage()));
    }

    public T register(ItemStack stack, T entry) {
        return this.register(key(stack), entry);
    }

    public T register(ResourceLocation key, T entry) {
        entry.setRegistryName(key);
        this.entries.put(key, entry);

        return entry;
    }

    public boolean contains(ItemStack stack) {
        return this.entries.containsKey(key(stack));
    }

    public T get(ItemStack stack) {
        return this.entries.get(key(stack));
    }

    public List<T> values() {
        return new ArrayList<T>(this.entries.values());
    }

    public int size() {
        return this.entries.size();
    }
}
