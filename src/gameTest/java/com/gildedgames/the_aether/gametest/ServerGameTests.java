package com.gildedgames.the_aether.gametest;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.gildedgames.the_aether.AetherConfig;
import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.blocks.BlocksAether;
import com.gildedgames.the_aether.entities.EntitiesAether;
import com.gildedgames.the_aether.items.ItemsAether;
import com.gildedgames.the_aether.registry.AetherLore;
import com.gildedgames.the_aether.registry.creative_tabs.AetherCreativeTabs;

/**
 * In-game assertions for The Aether Legacy.
 *
 * <p>
 * These checks run against a real dedicated server so they exercise the Block/Item
 * registries, the Forge dimension registry, recipe mutation and a live World.
 * Pure-Java behavior belongs in {@code src/test/java}; this class only covers
 * things that cannot boot headlessly.
 * </p>
 */
public final class ServerGameTests {

    private static final String FAIL = "[AETHER-GAMETEST] FAIL ";
    private static final String PASS = "[AETHER-GAMETEST] ok   ";

    private static int failures;
    private static int checks;

    private ServerGameTests() {}

    public static int runAll() {
        failures = 0;
        checks = 0;

        checkBlockRegistries();
        checkItemRegistries();
        checkCreativeTabs();
        checkDimensionRegistration();
        checkEntityRegistry();
        checkAetherAPIRegistries();
        checkLoreLookup();
        checkRecipes();
        checkBlockPlacement();

        return failures;
    }

    public static int checkCount() {
        return checks;
    }

    private static void checkBlockRegistries() {
        Block[] blocks = { BlocksAether.aether_grass, BlocksAether.aether_dirt, BlocksAether.holystone,
            BlocksAether.aercloud, BlocksAether.ambrosium_ore, BlocksAether.zanite_ore, BlocksAether.gravitite_ore,
            BlocksAether.skyroot_log, BlocksAether.golden_oak_log, BlocksAether.skyroot_planks, BlocksAether.enchanter,
            BlocksAether.freezer, BlocksAether.incubator, BlocksAether.aether_portal, BlocksAether.sun_altar,
            BlocksAether.treasure_chest, BlocksAether.carved_stone, BlocksAether.angelic_stone,
            BlocksAether.hellfire_stone };

        for (Block block : blocks) {
            check("block reference is non-null: " + block, block != null);
            if (block == null) {
                continue;
            }

            Object name = Block.blockRegistry.getNameForObject(block);
            check(
                "block is registered: " + name,
                name != null && name.toString()
                    .startsWith("aether_legacy:"));
        }
    }

    private static void checkItemRegistries() {
        Item[] items = { ItemsAether.zanite_gemstone, ItemsAether.ambrosium_shard, ItemsAether.blueberry,
            ItemsAether.skyroot_pickaxe, ItemsAether.zanite_sword, ItemsAether.dungeon_key,
            ItemsAether.aether_spawn_egg, ItemsAether.lore_book, ItemsAether.golden_parachute };

        for (Item item : items) {
            check("item reference is non-null: " + item, item != null);
            if (item == null) {
                continue;
            }

            Object name = Item.itemRegistry.getNameForObject(item);
            check(
                "item is registered: " + name,
                name != null && name.toString()
                    .startsWith("aether_legacy:"));
        }
    }

    private static void checkCreativeTabs() {
        check("block creative tab has an icon", AetherCreativeTabs.blocks.getIconItemStack() != null);
        check("tool creative tab has an icon", AetherCreativeTabs.tools.getIconItemStack() != null);
        check("weapon creative tab has an icon", AetherCreativeTabs.weapons.getIconItemStack() != null);
        check("armor creative tab has an icon", AetherCreativeTabs.armor.getIconItemStack() != null);
        check("accessory creative tab has an icon", AetherCreativeTabs.accessories.getIconItemStack() != null);
        check("material creative tab has an icon", AetherCreativeTabs.material.getIconItemStack() != null);
        check("misc creative tab has an icon", AetherCreativeTabs.misc.getIconItemStack() != null);
    }

    private static void checkDimensionRegistration() {
        int dimensionId = AetherConfig.getAetherDimensionID();
        check("Aether dimension id is positive", dimensionId > 0);
        check("Aether dimension is registered", DimensionManager.isDimensionRegistered(dimensionId));
        check("Aether dimension provider type is registered", DimensionManager.getProviderType(dimensionId) != 0);
        check(
            "Aether biome id is in range",
            AetherConfig.getAetherBiomeID() >= 0 && AetherConfig.getAetherBiomeID() < 256);
    }

    private static void checkEntityRegistry() {
        check("Aether entity id map is populated", EntitiesAether.IDtoClassMapping.size() > 0);
        check("Aether entity class map is populated", EntitiesAether.classToStringMapping.size() > 0);
        check("Aether moa id resolves", EntitiesAether.getClassFromID(0) != null);
        check("Aether moa name resolves", "moa".equals(EntitiesAether.getStringFromID(0)));
    }

    private static void checkAetherAPIRegistries() {
        check(
            "accessory registry is populated",
            AetherAPI.instance()
                .isAccessory(new ItemStack(ItemsAether.repulsion_shield)));
        check(
            "enchantment registry is populated",
            AetherAPI.instance()
                .hasEnchantment(new ItemStack(ItemsAether.zanite_pickaxe)));
        check(
            "enchantment fuel registry is populated",
            AetherAPI.instance()
                .isEnchantmentFuel(new ItemStack(ItemsAether.ambrosium_shard)));
        check(
            "freezable registry is populated",
            AetherAPI.instance()
                .hasFreezable(new ItemStack(Items.water_bucket)));
        check(
            "freezable fuel registry is populated",
            AetherAPI.instance()
                .isFreezableFuel(new ItemStack(BlocksAether.icestone)));
        check(
            "moa type registry is populated",
            AetherAPI.instance()
                .getMoaTypeValues()
                .size() > 0);
        check(
            "first moa type resolves",
            AetherAPI.instance()
                .getMoaType(0) != null);
        check(
            "moa type id round-trips",
            AetherAPI.instance()
                .getMoaTypeId(
                    AetherAPI.instance()
                        .getMoaType(0))
                == 0);
    }

    private static void checkLoreLookup() {
        String key = AetherLore.getLoreEntryKey(new ItemStack(ItemsAether.zanite_gemstone));
        check("lore key is generated", key != null && key.startsWith("lore.aether_legacy."));
    }

    private static void checkRecipes() {
        check(
            "crafting recipe list is populated",
            !CraftingManager.getInstance()
                .getRecipeList()
                .isEmpty());
        check(
            "skyroot log smelts to charcoal",
            FurnaceRecipes.smelting()
                .getSmeltingResult(new ItemStack(BlocksAether.skyroot_log, 1, 1)) != null);
    }

    private static void checkBlockPlacement() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        check("overworld is available", world != null);
        if (world == null) {
            return;
        }

        int x = 0;
        int y = 200;
        int z = 0;
        Block oldBlock = world.getBlock(x, y, z);
        int oldMeta = world.getBlockMetadata(x, y, z);

        world.setBlock(x, y, z, BlocksAether.holystone);
        check("holystone can be placed in a live world", world.getBlock(x, y, z) == BlocksAether.holystone);
        world.setBlock(x, y, z, oldBlock, oldMeta, 3);
    }

    private static void check(String description, boolean condition) {
        checks++;
        if (condition) {
            System.out.println(PASS + description);
        } else {
            failures++;
            System.out.println(FAIL + description);
        }
    }
}
