package com.gildedgames.the_aether.api;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.gildedgames.the_aether.api.accessories.AetherAccessory;
import com.gildedgames.the_aether.api.enchantments.AetherEnchantment;
import com.gildedgames.the_aether.api.enchantments.AetherEnchantmentFuel;
import com.gildedgames.the_aether.api.freezables.AetherFreezable;
import com.gildedgames.the_aether.api.freezables.AetherFreezableFuel;
import com.gildedgames.the_aether.api.moa.AetherMoaType;
import com.gildedgames.the_aether.api.player.IPlayerAether;

public class AetherAPI {

    private static final AetherRegistry<AetherAccessory> iAccessoryRegistry = new AetherRegistry<AetherAccessory>(
        false);
    private static final AetherRegistry<AetherEnchantment> iEnchantmentRegistry = new AetherRegistry<AetherEnchantment>(
        false);
    private static final AetherRegistry<AetherEnchantmentFuel> iEnchantmentFuelRegistry = new AetherRegistry<AetherEnchantmentFuel>(
        false);
    private static final AetherRegistry<AetherFreezable> iFreezableRegistry = new AetherRegistry<AetherFreezable>(
        false);
    private static final AetherRegistry<AetherFreezableFuel> iFreezableFuelRegistry = new AetherRegistry<AetherFreezableFuel>(
        false);
    private static final AetherRegistry<AetherMoaType> iMoaTypeRegistry = new AetherRegistry<AetherMoaType>(true);

    private static final AetherAPI instance = new AetherAPI();

    public static final IPlayerAether get(EntityPlayer playerIn) {
        return (IPlayerAether) playerIn.getExtendedProperties("aether_legacy:player_aether");
    }

    public AetherAccessory register(AetherAccessory type) {
        return iAccessoryRegistry.register(type.getAccessoryStack(), type);
    }

    public AetherEnchantment register(AetherEnchantment type) {
        return iEnchantmentRegistry.register(type.getInput(), type);
    }

    public AetherEnchantmentFuel register(AetherEnchantmentFuel type) {
        return iEnchantmentFuelRegistry.register(type.getFuelStack(), type);
    }

    public AetherFreezable register(AetherFreezable type) {
        return iFreezableRegistry.register(type.getInput(), type);
    }

    public AetherFreezableFuel register(AetherFreezableFuel type) {
        return iFreezableFuelRegistry.register(type.getFuelStack(), type);
    }

    public AetherMoaType register(String modId, String name, AetherMoaType type) {
        return iMoaTypeRegistry.register(new ResourceLocation(modId, name), type);
    }

    public AetherMoaType register(ResourceLocation registryName, AetherMoaType type) {
        return iMoaTypeRegistry.register(registryName, type);
    }

    public boolean isAccessory(ItemStack stack) {
        return iAccessoryRegistry.contains(stack);
    }

    public AetherAccessory getAccessory(ItemStack stack) {
        return iAccessoryRegistry.get(stack);
    }

    public boolean hasEnchantment(ItemStack stack) {
        return iEnchantmentRegistry.contains(stack);
    }

    public AetherEnchantment getEnchantment(ItemStack stack) {
        return iEnchantmentRegistry.get(stack);
    }

    public boolean isEnchantmentFuel(ItemStack stack) {
        return iEnchantmentFuelRegistry.contains(stack);
    }

    public AetherEnchantmentFuel getEnchantmentFuel(ItemStack stack) {
        return iEnchantmentFuelRegistry.get(stack);
    }

    public boolean hasFreezable(ItemStack stack) {
        return iFreezableRegistry.contains(stack);
    }

    public AetherFreezable getFreezable(ItemStack stack) {
        return iFreezableRegistry.get(stack);
    }

    public boolean isFreezableFuel(ItemStack stack) {
        return iFreezableFuelRegistry.contains(stack);
    }

    public AetherFreezableFuel getFreezableFuel(ItemStack stack) {
        return iFreezableFuelRegistry.get(stack);
    }

    public List<AetherEnchantment> getEnchantmentValues() {
        return iEnchantmentRegistry.values();
    }

    public List<AetherFreezable> getFreezableValues() {
        return iFreezableRegistry.values();
    }

    public List<AetherMoaType> getMoaTypeValues() {
        return iMoaTypeRegistry.values();
    }

    public int getMoaTypeId(AetherMoaType type) {
        return this.getMoaTypeValues()
            .indexOf(type);
    }

    public AetherMoaType getMoaType(int id) {
        List<AetherMoaType> values = this.getMoaTypeValues();

        return id >= 0 && id < values.size() ? values.get(id) : null;
    }

    public AetherMoaType getRandomMoaType() {
        return this.getMoaTypeValues()
            .get(new Random().nextInt(this.getMoaTypeSize()));
    }

    public int getMoaTypeSize() {
        return iMoaTypeRegistry.size();
    }

    public static AetherAPI instance() {
        return instance;
    }

}
