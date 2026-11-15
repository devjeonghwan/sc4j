package org.fogofwarlabs.sc4j.engine.game;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

/**
 * Tests the wire form of a game setup: the map reference, the slots with their factions, the seed, the options and the pool sizes survive it,
 * and a setup the codec cannot write is refused.
 */
final class GameSetupCodecTest {
    private static final @NotNull GameOption<Integer> START_RESOURCES = GameOption.of("startResources", Integer.class, 50);
    private static final @NotNull GameOption<String> SCENARIO = GameOption.required("scenario", String.class);
    private static final @NotNull GameOption<Double> SPEED = GameOption.of("speed", Double.class, 1.0);
    private static final long SEED = 0x1234_5678_9ABCL;
    private static final int UNITS = 300;

    @Test
    void read_should_return_the_setup_that_write_wrote() throws IOException {
        SkirmishScenario scenario = new SkirmishScenario();
        GameSetupCodec codec = new GameSetupCodec(List.of(scenario.getWardens(), scenario.getRaiders()), List.of(START_RESOURCES, SCENARIO));
        GameSetup setup = new GameSetup.Builder(new MapReference("Twin Rocks", new byte[]{1, 2, 3}))
                .addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, scenario.getRaiders()))
                .addPlayer(new PlayerSlot(2, ControllerKind.OBSERVER, 1, 4, null))
                .setSeed(SEED)
                .setOptions(new GameOptions.Builder().set(SCENARIO, "duel").set(START_RESOURCES, 75).build())
                .setLimits(new EngineLimits.Builder().setUnits(UNITS).build())
                .build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        codec.write(setup, new DataOutputStream(bytes));

        GameSetup read = codec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        EngineLimits limits = read.getLimits();

        Assertions.assertEquals("Twin Rocks", read.getMap().getName());
        Assertions.assertArrayEquals(new byte[]{1, 2, 3}, read.getMap().getDigest());
        Assertions.assertEquals(setup.getPlayers(), read.getPlayers());
        Assertions.assertSame(scenario.getRaiders(), read.getPlayers().getFirst().getFaction());
        Assertions.assertEquals(SEED, read.getSeed());
        Assertions.assertEquals("duel", read.getOptions().get(SCENARIO));
        Assertions.assertEquals(75, read.getOptions().get(START_RESOURCES));
        Assertions.assertNotNull(limits);
        Assertions.assertEquals(UNITS, limits.getUnits());
    }

    @Test
    void write_should_refuse_a_faction_or_option_value_without_wire_form() {
        SkirmishScenario scenario = new SkirmishScenario();
        GameSetupCodec codec = new GameSetupCodec(List.of(scenario.getWardens()), List.of(SPEED));
        GameSetup otherFaction = new GameSetup.Builder(new MapReference("map", new byte[0]))
                .addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, scenario.getRaiders()))
                .build();
        GameSetup doubleValue = new GameSetup.Builder(new MapReference("map", new byte[0]))
                .setOptions(new GameOptions.Builder().set(SPEED, 2.0).build())
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> codec.write(otherFaction, new DataOutputStream(new ByteArrayOutputStream())));
        Assertions.assertThrows(IllegalArgumentException.class, () -> codec.write(doubleValue, new DataOutputStream(new ByteArrayOutputStream())));
    }
}
