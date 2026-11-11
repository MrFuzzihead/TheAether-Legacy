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

    @Test
    public void halfSlabsUseTileLocalizationKeys() throws IOException {
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

        String[] slabs = { "skyroot_slab", "carved_slab", "angelic_slab", "hellfire_slab", "holystone_slab",
            "mossy_holystone_slab", "holystone_brick_slab", "aerogel_slab" };

        for (String slab : slabs) {
            assertTrue("missing tile." + slab + ".name", keys.contains("tile." + slab + ".name"));
        }
    }
}
