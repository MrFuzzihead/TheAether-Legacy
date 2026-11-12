package com.gildedgames.the_aether.registry;

import static org.junit.Assert.assertEquals;
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

    private static final String[] LANGUAGES = { "en_US", "de_DE", "es_MX", "fr_FR", "it_IT", "ja_JP", "ko_KR", "nl_NL",
        "pl_PL", "ro_RO", "ru_RU", "sv_SE", "th_TH", "uk_UA", "zh_CN", "zh_TW" };

    private Set<String> loadKeys() throws IOException {
        return this.loadKeys("en_US");
    }

    private Set<String> loadKeys(String language) throws IOException {
        String path = "/assets/aether_legacy/lang/" + language + ".lang";
        InputStream stream = getClass().getResourceAsStream(path);
        assertNotNull(language + ".lang must be on the test runtime classpath", stream);

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
    public void allLanguageFilesHaveTheSameKeys() throws IOException {
        Set<String> expected = this.loadKeys("en_US");

        for (String language : LANGUAGES) {
            assertEquals("localization keys differ for " + language, expected, this.loadKeys(language));
        }
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
}
