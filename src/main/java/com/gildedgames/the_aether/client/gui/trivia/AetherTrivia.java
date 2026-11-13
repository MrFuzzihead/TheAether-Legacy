package com.gildedgames.the_aether.client.gui.trivia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import com.gildedgames.the_aether.Aether;

public class AetherTrivia {

    private static final Random random = new Random();

    private static final Map<String, List<String>> entriesByLanguage = new HashMap<String, List<String>>();

    public AetherTrivia() {

    }

    public static String getNewTrivia() {
        String localization = Minecraft.getMinecraft()
            .getLanguageManager()
            .getCurrentLanguage()
            .getLanguageCode();
        List<String> entries = getEntries(localization);

        if (entries.isEmpty()) {
            entries = getEntries("en_US");
        }

        return entries.isEmpty() ? "missingno" : formatEntry(entries);
    }

    public static String getEntriesForLocalization(String localization) {
        List<String> entries = getEntries(localization);

        return entries.isEmpty() ? null : formatEntry(entries);
    }

    private static String formatEntry(List<String> entries) {
        return I18n.format("gui.aether_trivia.pro_tip") + " " + entries.get(random.nextInt(entries.size()));
    }

    private static List<String> getEntries(String localization) {
        List<String> cached = entriesByLanguage.get(localization);

        if (cached != null) {
            return cached;
        }

        List<String> entries = new ArrayList<String>();
        BufferedReader bufferedreader = null;

        try {
            bufferedreader = new BufferedReader(
                new InputStreamReader(
                    Minecraft.getMinecraft()
                        .getResourceManager()
                        .getResource(Aether.locate("texts/" + localization + ".txt"))
                        .getInputStream(),
                    StandardCharsets.UTF_8));
            String line;

            while ((line = bufferedreader.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    entries.add(line);
                }
            }
        } catch (IOException ignore) {} finally {
            if (bufferedreader != null) {
                try {
                    bufferedreader.close();
                } catch (IOException ignore) {}
            }
        }

        entriesByLanguage.put(localization, entries);

        return entries;
    }
}
