package com.gildedgames.the_aether.gametest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStartedEvent;

/**
 * Test-only mod for The Aether Legacy.
 *
 * <p>
 * It lives in the {@code gameTest} source set and is only added to the classpath by
 * {@code ./gradlew gameTestServer} (or {@code -PgameTest}), so it is never part of
 * the published mod jar. The assertions run once the dedicated server is fully
 * started and then exit with a machine-readable marker.
 * </p>
 */
@Mod(
    modid = GameTestMod.MOD_ID,
    name = "The Aether Legacy Game Tests",
    version = "1.0",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:aether_legacy")
public final class GameTestMod {

    public static final String MOD_ID = "aether_legacy_gametest";
    public static final String OK_MARKER = "AETHER_GAMETEST_OK";
    public static final String FAIL_MARKER = "AETHER_GAMETEST_FAILED";

    private boolean ran;

    @EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        if (ran) {
            return;
        }
        ran = true;

        int failures;
        int checks;
        try {
            failures = ServerGameTests.runAll();
            checks = ServerGameTests.checkCount();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.out.println(FAIL_MARKER + ": " + throwable);
            exit(1);
            return;
        }

        System.out.println("[AETHER-GAMETEST] " + (checks - failures) + "/" + checks + " checks passed");
        if (failures > 0) {
            System.out.println(FAIL_MARKER + ": " + failures + " assertion(s) failed, see above");
            exit(1);
        } else {
            System.out.println(OK_MARKER);
            exit(0);
        }
    }

    private static void exit(int status) {
        System.out.flush();
        FMLCommonHandler.instance()
            .exitJava(status, false);
    }
}
