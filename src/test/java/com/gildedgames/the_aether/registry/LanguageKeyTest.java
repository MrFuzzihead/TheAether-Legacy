package com.gildedgames.the_aether.registry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class LanguageKeyTest {

    private Set<String> loadKeys() throws IOException {
        InputStream stream = getClass().getResourceAsStream("/assets/aether_legacy/lang/en_US.lang");
        assertNotNull("en_US.lang must be on the test runtime classpath", stream);

        Set<String> keys = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                int separator = line.indexOf('=');
                if (separator > 0) {
                    keys.add(line.substring(0, separator));
                }
            }
        } finally {
            reader.close();
        }

        return keys;
    }

    @Test
    public void halfSlabsUseTileLocalizationKeys() throws IOException {
        Set<String> keys = loadKeys();
        String[] slabs = { "skyroot_slab", "carved_slab", "angelic_slab", "hellfire_slab", "holystone_slab",
            "mossy_holystone_slab", "holystone_brick_slab", "aerogel_slab" };

        for (String slab : slabs) {
            assertTrue("missing tile." + slab + ".name", keys.contains("tile." + slab + ".name"));
        }
    }

    @Test
    public void sunAltarStringsAreLocalized() throws IOException {
        Set<String> keys = loadKeys();

        assertTrue("missing sun altar title", keys.contains("gui.sun_altar.title"));
        assertTrue("missing sun altar time selector", keys.contains("gui.sun_altar.select_time"));
    }

    @Test
    public void loreGuiStringsAreLocalized() throws IOException {
        Set<String> keys = loadKeys();

        String[] loreKeys = { "gui.lore.previous", "gui.lore.next", "gui.lore.book", "gui.lore.of_lore",
            "gui.lore.item" };

        for (String key : loreKeys) {
            assertTrue("missing " + key, keys.contains(key));
        }
    }

    @Test
    public void mainMenuStringsAreLocalized() throws IOException {
        Set<String> keys = loadKeys();

        assertTrue("missing main menu mods button", keys.contains("gui.aether_menu.mods"));
        assertTrue("missing main menu info text", keys.contains("gui.aether_menu.info"));
    }

    @Test
    public void perkGuiStringsAreLocalized() throws IOException {
        Set<String> keys = loadKeys();

        String[] perkKeys = { "gui.perks.moa_customizer", "gui.perks.developer_perks", "gui.perks.enable_halo",
            "gui.perks.default_skin", "gui.perks.use_default", "gui.perks.confirm_skin", "gui.perks.donator_options",
            "gui.perks.leg_beak_color", "gui.perks.body_color", "gui.perks.body_marking_color", "gui.perks.eye_color",
            "gui.perks.wing_color", "gui.perks.wing_marking_color" };

        for (String key : perkKeys) {
            assertTrue("missing " + key, keys.contains(key));
        }
    }
}
