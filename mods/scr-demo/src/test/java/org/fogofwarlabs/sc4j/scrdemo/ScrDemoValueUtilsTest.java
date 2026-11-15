package org.fogofwarlabs.sc4j.scrdemo;

import org.fogofwarlabs.sc4j.scr.content.ScrUnitIds;
import org.fogofwarlabs.sc4j.scr.content.ScrWeaponIds;
import org.fogofwarlabs.sc4j.scr.core.test.ExternalInput;
import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.data.format.dat.UnitsDat;
import org.fogofwarlabs.sc4j.scr.data.format.dat.WeaponsDat;
import org.fogofwarlabs.sc4j.scr.game.test.GameTestResources;
import org.fogofwarlabs.sc4j.scr.game.test.RunningGameScenario;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tests the value changes of demo 2 with the SC:R installation: the game that the clone's replay runtime creates from a melee replay holds the
 * demo's unit and weapon values in its own tables and the installation's values everywhere else, while the same game of the SC:R mod holds the
 * installation's values only. Both mods run from their built jars in their own mod class loaders ({@link ModJarScenario}).
 */
@RequiresInstallation
final class ScrDemoValueUtilsTest {
    private static final @NotNull String SCR_ID = "scr";
    private static final @NotNull String DEMO_ID = "scr-demo";

    private static final @NotNull String GAME_DATA = "org.fogofwarlabs.sc4j.scr.game.state.GameData";
    private static final @NotNull String REPLAY_FILE = "org.fogofwarlabs.sc4j.scr.data.format.replay.ReplayFile";
    private static final @NotNull String CHK_FILE = "org.fogofwarlabs.sc4j.scr.data.format.chk.ChkFile";
    private static final @NotNull String REPLAY_RUNTIME = "org.fogofwarlabs.sc4j.scr.game.replay.ReplayRuntime";

    /**
     * The values of the mod class {@code ScrDemoValueUtils}, which the test class path does not hold.
     */
    private static final int DEMO_MARINE_MINERAL_COST = 45;
    private static final int DEMO_MARINE_BUILD_TIME = 300;
    private static final int DEMO_GAUSS_RIFLE_DAMAGE = 7;

    private static final @NotNull String UNITS = "units/";
    private static final @NotNull String WEAPONS = "weapons/";
    /**
     * The compared columns: the demo's changed fields and fields it leaves alone.
     */
    private static final @NotNull String @NotNull [] UNIT_FIELDS = {UnitsDat.MINERAL_COST, UnitsDat.GAS_COST, UnitsDat.BUILD_TIME,
                                                                    UnitsDat.HIT_POINTS};
    private static final @NotNull String @NotNull [] WEAPON_FIELDS = {WeaponsDat.DAMAGE_AMOUNT, WeaponsDat.DAMAGE_BONUS, WeaponsDat.COOLDOWN};

    /**
     * The compared columns of one mod's run: of the installed tables it loaded, and of the game's own tables.
     */
    private static final class ModTables {
        private final @NotNull Map<@NotNull String, int @NotNull []> installed;
        private final @NotNull Map<@NotNull String, int @NotNull []> game;

        private ModTables(@NotNull Map<@NotNull String, int @NotNull []> installed, @NotNull Map<@NotNull String, int @NotNull []> game) {
            this.installed = installed;
            this.game      = game;
        }
    }

    /**
     * Loads the installation with one mod's classes, creates the replay's game with the mod's replay runtime and reads the compared columns.
     */
    private static @NotNull ModTables readTables(@NotNull String modId, @NotNull Path jar, byte @NotNull [] replayData) throws ReflectiveOperationException, IOException {
        Path installation = ExternalInput.INSTALLATION.getPath();

        try (ModJarScenario mod = new ModJarScenario(modId, jar)) {
            Object gameData = ModJarScenario.require(mod.callStatic(GAME_DATA, "loadFrom", installation));

            try {
                Object replay = ModJarScenario.require(mod.callStatic(REPLAY_FILE, "parse", replayData));
                Object mapData = ModJarScenario.require(mod.invoke(replay, "getMapData"));
                Object map = ModJarScenario.require(mod.callStatic(CHK_FILE, "parseTolerant", mapData));
                Object runtime = mod.construct(REPLAY_RUNTIME, replay, map, gameData,
                                               RunningGameScenario.VIEWPORT_WIDTH, RunningGameScenario.VIEWPORT_HEIGHT,
                                               RunningGameScenario.INSTRUCTION_LIMIT);
                Object game = ModJarScenario.require(mod.invoke(runtime, "getGame"));

                return new ModTables(readColumns(mod, gameData), readColumns(mod, ModJarScenario.require(mod.invoke(game, "requireGameData"))));
            } finally {
                mod.invoke(gameData, "close");
            }
        }
    }

    private static @NotNull Map<@NotNull String, int @NotNull []> readColumns(@NotNull ModJarScenario mod, @NotNull Object gameData) throws ReflectiveOperationException {
        Map<String, int[]> columns = new TreeMap<>();
        Object units = ModJarScenario.require(mod.invoke(gameData, "getUnits"));
        Object weapons = ModJarScenario.require(mod.invoke(gameData, "getWeapons"));

        for (String field : UNIT_FIELDS) {
            columns.put(UNITS + field, (int[]) ModJarScenario.require(mod.invoke(units, "copyColumn", field)));
        }

        for (String field : WEAPON_FIELDS) {
            columns.put(WEAPONS + field, (int[]) ModJarScenario.require(mod.invoke(weapons, "copyColumn", field)));
        }

        return columns;
    }

    private static void assertColumns(@NotNull Map<@NotNull String, int @NotNull []> expected, @NotNull Map<@NotNull String, int @NotNull []> actual,
                                      @NotNull String message) {
        Assertions.assertEquals(expected.keySet(), actual.keySet(), message);

        for (Map.Entry<String, int[]> column : expected.entrySet()) {
            Assertions.assertArrayEquals(column.getValue(), actual.get(column.getKey()), () -> message + ": " + column.getKey());
        }
    }


    @Test
    void a_demo_game_holds_the_demo_values_and_a_sc_r_game_the_installed_ones() throws ReflectiveOperationException, IOException {
        byte[] replay = GameTestResources.readAllBytes(GameTestResources.REPLAY_TVT_PYTHON);
        ModTables scr = readTables(SCR_ID, ScrDemoTestResources.getScrModJar(), replay);
        ModTables demo = readTables(DEMO_ID, ScrDemoTestResources.getModJar(), replay);
        Map<String, int[]> expected = new TreeMap<>();

        for (Map.Entry<String, int[]> column : scr.installed.entrySet()) {
            expected.put(column.getKey(), column.getValue().clone());
        }

        expected.get(UNITS + UnitsDat.MINERAL_COST)[ScrUnitIds.TERRAN_MARINE]      = DEMO_MARINE_MINERAL_COST;
        expected.get(UNITS + UnitsDat.BUILD_TIME)[ScrUnitIds.TERRAN_MARINE]        = DEMO_MARINE_BUILD_TIME;
        expected.get(WEAPONS + WeaponsDat.DAMAGE_AMOUNT)[ScrWeaponIds.GAUSS_RIFLE] = DEMO_GAUSS_RIFLE_DAMAGE;

        assertColumns(scr.installed, scr.game, "the SC:R game holds the installed values");
        assertColumns(scr.installed, demo.installed, "the demo loads the same installed tables");
        assertColumns(expected, demo.game, "the demo game holds the demo values and the installed ones elsewhere");
        Assertions.assertNotEquals(DEMO_MARINE_MINERAL_COST, scr.installed.get(UNITS + UnitsDat.MINERAL_COST)[ScrUnitIds.TERRAN_MARINE]);
        Assertions.assertNotEquals(DEMO_MARINE_BUILD_TIME, scr.installed.get(UNITS + UnitsDat.BUILD_TIME)[ScrUnitIds.TERRAN_MARINE]);
        Assertions.assertNotEquals(DEMO_GAUSS_RIFLE_DAMAGE, scr.installed.get(WEAPONS + WeaponsDat.DAMAGE_AMOUNT)[ScrWeaponIds.GAUSS_RIFLE]);
    }
}
