package com.gildedgames.the_aether.gametest;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.block.Block;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.gildedgames.the_aether.Aether;
import com.gildedgames.the_aether.AetherConfig;
import com.gildedgames.the_aether.AetherEventHandler;
import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.blocks.BlocksAether;
import com.gildedgames.the_aether.entities.EntitiesAether;
import com.gildedgames.the_aether.entities.bosses.sun_spirit.EntitySunSpirit;
import com.gildedgames.the_aether.entities.effects.EffectInebriation;
import com.gildedgames.the_aether.entities.effects.PotionInebriation;
import com.gildedgames.the_aether.entities.passive.EntityMiniCloud;
import com.gildedgames.the_aether.entities.passive.mountable.EntitySwet;
import com.gildedgames.the_aether.entities.projectile.crystals.EntityCrystal;
import com.gildedgames.the_aether.entities.projectile.crystals.EnumCrystalType;
import com.gildedgames.the_aether.entities.util.AetherMoaTypes;
import com.gildedgames.the_aether.items.ItemMoaEgg;
import com.gildedgames.the_aether.items.ItemsAether;
import com.gildedgames.the_aether.items.util.EnumSkyrootBucketType;
import com.gildedgames.the_aether.items.weapons.ItemZaniteSword;
import com.gildedgames.the_aether.registry.AetherLore;
import com.gildedgames.the_aether.registry.creative_tabs.AetherCreativeTabs;
import com.gildedgames.the_aether.tileentity.TileEntityFreezer;
import com.gildedgames.the_aether.world.AetherData;
import com.gildedgames.the_aether.world.AetherWorldProvider;
import com.google.common.collect.Multimap;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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
        checkToolAttackDamage();
        checkRemedyBucket();
        checkLeafDrops();
        checkLoreLookup();
        checkRecipes();
        checkBlockPlacement();
        checkAetherTimeTick();
        checkSpawnDataSafety();
        checkCrystalSafety();
        checkMoaAndLoreSafety();
        checkZaniteSwordAttributeMap();
        checkSwetBehavior();
        checkFreezerBonus();
        checkMoaTypeOrder();
        checkLogDrops();
        checkSunSpiritDefeatState();
        checkGeneratedVersion();

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

    private static void checkToolAttackDamage() {
        ItemStack pickaxe = new ItemStack(ItemsAether.skyroot_pickaxe);
        Multimap<String, AttributeModifier> modifiers = pickaxe.getAttributeModifiers();
        Collection<AttributeModifier> attackModifiers = modifiers
            .get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName());

        double attackDamage = 0.0D;
        for (AttributeModifier modifier : attackModifiers) {
            attackDamage += modifier.getAmount();
        }

        check("Aether tools contribute positive attack damage", attackDamage > 0.0D);
    }

    private static void checkRemedyBucket() {
        EffectInebriation effect = new EffectInebriation(PotionInebriation.inebriation.id, 500, 0);

        check(
            "remedy bucket is registered as a cure",
            effect.isCurativeItem(new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Remedy.meta)));
        check(
            "empty bucket is not a cure",
            !effect.isCurativeItem(new ItemStack(ItemsAether.skyroot_bucket, 1, EnumSkyrootBucketType.Empty.meta)));
        check(
            "skyroot bucket keeps its container item",
            ItemsAether.skyroot_bucket.getContainerItem() == ItemsAether.skyroot_bucket);
    }

    private static void checkLeafDrops() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            check("overworld is available for leaf drops", false);
            return;
        }

        Block[] leaves = { BlocksAether.skyroot_leaves, BlocksAether.golden_oak_leaves, BlocksAether.crystal_leaves,
            BlocksAether.crystal_fruit_leaves, BlocksAether.holiday_leaves, BlocksAether.decorated_holiday_leaves };

        for (Block leaf : leaves) {
            ArrayList<ItemStack> drops = leaf.getDrops(world, 0, 100, 0, 0, 0);
            boolean invalid = false;

            for (ItemStack drop : drops) {
                if (drop == null || drop.getItem() == null) {
                    invalid = true;
                }
            }

            check("leaf drops never contain a null item: " + leaf, !invalid);
        }
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

    private static void checkSpawnDataSafety() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            check("overworld is available for spawn-data checks", false);
            return;
        }

        EntityMiniCloud cloud = new EntityMiniCloud(world);
        cloud.owner = null;

        ByteBuf buffer = Unpooled.buffer();
        cloud.writeSpawnData(buffer);

        EntityMiniCloud decoded = new EntityMiniCloud(world);
        decoded.readSpawnData(buffer);
        check("mini cloud spawn data handles a missing owner", decoded.owner == null);
        check("mini cloud spawn data is fully consumed", buffer.readableBytes() == 0);
    }

    private static void checkCrystalSafety() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            check("overworld is available for crystal checks", false);
            return;
        }

        EntityCrystal crystal = new EntityCrystal(world, 0.0D, 100.0D, 0.0D, EnumCrystalType.CLOUD);
        NBTTagCompound tag = new NBTTagCompound();
        crystal.writeEntityToNBT(tag);

        EntityCrystal loaded = new EntityCrystal(world);
        loaded.readEntityFromNBT(tag);
        check("crystal type survives NBT persistence", loaded.getCrystalType() == EnumCrystalType.CLOUD);

        EntityCrystal thunder = new EntityCrystal(world, 0.0D, 100.0D, 0.0D, EnumCrystalType.THUNDER);
        ByteBuf buffer = Unpooled.buffer();
        thunder.writeSpawnData(buffer);

        EntityCrystal decoded = new EntityCrystal(world);
        decoded.readSpawnData(buffer);
        check("thunder crystal spawn data handles a missing target", decoded.getAttackTarget() == null);
    }

    private static void checkMoaAndLoreSafety() {
        ItemMoaEgg egg = (ItemMoaEgg) ItemsAether.moa_egg;
        ItemStack noTag = new ItemStack(egg);

        check("moa egg without NBT falls back to a valid type", egg.getMoaTypeFromItemStack(noTag) != null);

        ItemStack invalidTag = new ItemStack(egg);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("typeId", Integer.MAX_VALUE);
        invalidTag.setTagCompound(tag);
        check("moa egg with an invalid type id falls back", egg.getMoaTypeFromItemStack(invalidTag) != null);

        check(
            "invalid moa type ids return null",
            AetherAPI.instance()
                .getMoaType(-1) == null
                && AetherAPI.instance()
                    .getMoaType(
                        AetherAPI.instance()
                            .getMoaTypeSize())
                    == null);

        check("lore lookup handles a null stack", AetherLore.getLoreEntryKey(null) == null);
    }

    private static void checkMoaTypeOrder() {
        check(
            "moa type 0 is blue",
            AetherAPI.instance()
                .getMoaType(0) == AetherMoaTypes.blue);
        check(
            "moa type 1 is orange",
            AetherAPI.instance()
                .getMoaType(1) == AetherMoaTypes.orange);
        check(
            "moa type 2 is white",
            AetherAPI.instance()
                .getMoaType(2) == AetherMoaTypes.white);
        check(
            "moa type 3 is black",
            AetherAPI.instance()
                .getMoaType(3) == AetherMoaTypes.black);
    }

    private static void checkLogDrops() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            check("overworld is available for log drop checks", false);
            return;
        }

        check(
            "skyroot logs drop skyroot logs",
            BlocksAether.skyroot_log.getItemDropped(0, world.rand, 0)
                == Item.getItemFromBlock(BlocksAether.skyroot_log));
        check(
            "golden oak logs drop skyroot logs without silk touch",
            BlocksAether.golden_oak_log.getItemDropped(0, world.rand, 0)
                == Item.getItemFromBlock(BlocksAether.skyroot_log));
    }

    private static void checkSunSpiritDefeatState() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            check("overworld is available for Sun Spirit checks", false);
            return;
        }

        EntitySunSpirit spirit = new EntitySunSpirit(world);
        check("Sun Spirit starts undefeated", !spirit.hasDefeated);

        spirit.hasDefeated = true;

        NBTTagCompound tag = new NBTTagCompound();
        spirit.writeEntityToNBT(tag);

        EntitySunSpirit loaded = new EntitySunSpirit(world);
        loaded.readEntityFromNBT(tag);

        check("Sun Spirit defeat state persists", loaded.hasDefeated);
    }

    private static void checkGeneratedVersion() {
        try {
            String declared = Aether.class.getAnnotation(Mod.class)
                .version();
            String generated = (String) Class.forName("com.gildedgames.the_aether.Tags")
                .getField("VERSION")
                .get(null);

            check("mod annotation uses the generated version", generated.equals(declared));
        } catch (Throwable throwable) {
            check("mod annotation uses the generated version: " + throwable, false);
        }
    }

    private static void checkSwetBehavior() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            check("overworld is available for swet checks", false);
            return;
        }

        EntitySwet swet = new EntitySwet(world);
        check("non-friendly swets are allowed to despawn", swet.canDespawn());
    }

    private static void checkFreezerBonus() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            check("overworld is available for freezer checks", false);
            return;
        }

        int x = 6;
        int y = 120;
        int z = 6;
        Block oldBlock = world.getBlock(x, y, z);
        int oldMeta = world.getBlockMetadata(x, y, z);
        Block oldBelow = world.getBlock(x, y - 1, z);
        int oldBelowMeta = world.getBlockMetadata(x, y - 1, z);

        try {
            world.setBlock(x, y - 1, z, BlocksAether.icestone);
            world.setBlock(x, y, z, BlocksAether.freezer);

            TileEntityFreezer freezer = (TileEntityFreezer) world.getTileEntity(x, y, z);
            check("freezer tile entity exists", freezer != null);

            if (freezer != null) {
                freezer.setInventorySlotContents(0, new ItemStack(Items.water_bucket));
                freezer.setInventorySlotContents(1, new ItemStack(BlocksAether.icestone));

                freezer.updateEntity();
                freezer.updateEntity();
                freezer.updateEntity();

                check("icestone below a freezer doubles freeze progress", freezer.progress == 2);
            }
        } finally {
            world.setBlock(x, y, z, oldBlock, oldMeta, 3);
            world.setBlock(x, y - 1, z, oldBelow, oldBelowMeta, 3);
        }
    }

    private static void checkZaniteSwordAttributeMap() {
        ItemZaniteSword sword = (ItemZaniteSword) ItemsAether.zanite_sword;
        check("zanite sword has a non-null attribute modifier map", sword.getItemAttributeModifiers() != null);
    }

    private static void checkAetherTimeTick() {
        WorldServer aetherWorld = MinecraftServer.getServer()
            .worldServerForDimension(AetherConfig.getAetherDimensionID());
        check("Aether world loads", aetherWorld != null && aetherWorld.provider instanceof AetherWorldProvider);

        if (aetherWorld == null || !(aetherWorld.provider instanceof AetherWorldProvider)) {
            return;
        }

        AetherWorldProvider provider = (AetherWorldProvider) aetherWorld.provider;
        AetherData data = AetherData.getInstance(aetherWorld);
        boolean previousEternalDay = data.isEternalDay();
        long previousTime = data.getAetherTime();

        try {
            data.setEternalDay(true);
            data.setAetherTime(100L);

            AetherEventHandler handler = new AetherEventHandler();

            handler.onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.START, aetherWorld));
            check("START phase leaves Aether time unchanged", data.getAetherTime() == 100L);

            handler.onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, aetherWorld));
            check("END phase advances Aether time exactly once", data.getAetherTime() == 101L);

            provider.updateAetherTime();
            check("Aether time advances again", data.getAetherTime() == 102L);
            check("Aether cycle catchup is enabled", data.isShouldCycleCatchup());

            provider.setAetherTime(500L);
            float firstAngle = provider.calculateCelestialAngle(0L, 0.0F);
            float secondAngle = provider.calculateCelestialAngle(0L, 0.0F);
            check(
                "celestial angle calculation is side-effect free",
                firstAngle == secondAngle && provider.getAetherTime() == 500L);
        } finally {
            data.setEternalDay(previousEternalDay);
            data.setAetherTime(previousTime);
        }
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
