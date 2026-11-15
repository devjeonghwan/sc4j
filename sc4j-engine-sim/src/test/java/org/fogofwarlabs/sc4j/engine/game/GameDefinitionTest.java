package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.engine.entity.EntityKind;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnReset;
import org.fogofwarlabs.sc4j.engine.entity.column.IntColumn;
import org.fogofwarlabs.sc4j.engine.entity.pool.StandardSlotPolicy;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.random.PcgRandom;
import org.fogofwarlabs.sc4j.engine.random.RandomState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the game definition and setup: engine defaults, closing on build, validation and what a game takes from each.
 */
final class GameDefinitionTest {
    private static final @NotNull ColumnKey<IntColumn> LEVEL = ColumnKey.ofInt("level", 1, ColumnReset.ON_ALLOCATE);
    private static final @NotNull SystemKey<IdlePhaseSystem> IDLE = SystemKey.of("idle", IdlePhaseSystem.class);
    private static final @NotNull GameOption<Integer> START_RESOURCES = GameOption.of("startResources", Integer.class, 50);
    private static final int UNIT_LIMIT = 12;

    /**
     * A system that is a frame phase and does nothing.
     */
    private static final class IdlePhaseSystem implements GameSystem, FramePhase {
        @Override
        public void attach(@NotNull Game game) {
        }

        @Override
        public void run(@NotNull Game game) {
        }
    }

    @Test
    void empty_definition_should_have_the_engine_defaults() {
        GameDefinition definition = new GameDefinition.Builder().build();
        Game game = Game.create(definition, createSetup().build());

        Assertions.assertInstanceOf(PcgRandom.class, game.getRandom());
        Assertions.assertSame(EngineLimits.EMPTY, game.getLimits());
        Assertions.assertSame(StandardSlotPolicy.REUSE_RECENT, definition.getSlotPolicy(EntityKind.UNIT));
        Assertions.assertEquals(0, game.getUnits().getCapacity());
        Assertions.assertEquals(0, definition.getSystemCount());
        Assertions.assertEquals(0, definition.getHookCount());
        Assertions.assertEquals(0, definition.getFramePhaseCount());
    }

    @Test
    void build_should_close_the_builder() {
        GameDefinition.Builder builder = new GameDefinition.Builder();
        GameDefinition definition = builder.build();

        Assertions.assertSame(definition, builder.build());
        Assertions.assertThrows(IllegalStateException.class, () -> builder.setLimits(EngineLimits.EMPTY));
        Assertions.assertThrows(IllegalStateException.class, () -> builder.addColumn(EntityKind.UNIT, LEVEL));
        Assertions.assertThrows(IllegalStateException.class, () -> builder.useSystem(IDLE, context -> new IdlePhaseSystem()));
    }

    @Test
    void build_should_reject_a_phase_of_an_unregistered_system_and_a_column_added_twice() {
        GameDefinition.Builder builder = new GameDefinition.Builder().addFramePhase(IDLE);

        Assertions.assertThrows(IllegalStateException.class, builder::build);

        GameDefinition.Builder columns = new GameDefinition.Builder().addColumn(EntityKind.UNIT, LEVEL);

        Assertions.assertThrows(IllegalArgumentException.class, () -> columns.addColumn(EntityKind.UNIT, LEVEL));
        Assertions.assertDoesNotThrow(() -> columns.addColumn(EntityKind.SPRITE, LEVEL));
    }

    @Test
    void game_should_take_the_setup_limits_options_and_random_state() {
        EngineLimits definitionLimits = new EngineLimits.Builder().setUnits(4).build();
        EngineLimits setupLimits = new EngineLimits.Builder().setUnits(9).setSprites(2).setOrders(30).build();
        GameDefinition definition = new GameDefinition.Builder().setLimits(definitionLimits)
                                                                .setSlotPolicy(EntityKind.SPRITE, StandardSlotPolicy.REUSE_OLDEST)
                                                                .addColumn(EntityKind.UNIT, LEVEL)
                                                                .useSystem(IDLE, context -> new IdlePhaseSystem())
                                                                .addFramePhase(IDLE)
                                                                .build();
        RandomState saved = new PcgRandom(77L, PcgRandom.DEFAULT_STREAM, PcgRandom.DEFAULT_SOURCE_COUNT).saveState();
        GameSetup setup = createSetup().setLimits(setupLimits)
                                       .setInitialRandomState(saved)
                                       .setOptions(new GameOptions.Builder().set(START_RESOURCES, 75).build())
                                       .build();
        Game game = Game.create(definition, setup);

        Assertions.assertEquals(4, Game.create(definition, createSetup().build()).getUnits().getCapacity());
        Assertions.assertEquals(9, game.getUnits().getCapacity());
        Assertions.assertEquals(2, game.getSprites().getCapacity());
        Assertions.assertSame(StandardSlotPolicy.REUSE_OLDEST, game.getSprites().getPolicy());
        Assertions.assertSame(IDLE, definition.getSystemKey(0));
        Assertions.assertEquals(30, game.getLimits().getOrders());
        Assertions.assertEquals(1, game.getUnits().getColumn(LEVEL).get(9));
        Assertions.assertEquals(saved, game.getRandom().saveState());
        Assertions.assertEquals(75, game.getSetup().getOptions().get(START_RESOURCES));
        Assertions.assertEquals(50, GameOptions.DEFAULTS.get(START_RESOURCES));
        Assertions.assertSame(game.getModSystem(IDLE), game.getFrameRunner().getPhases().get(0));
        Assertions.assertEquals(2, game.getPlayers().size());
    }

    @Test
    void game_should_adopt_the_setup_generator_and_leave_mod_pools_empty() {
        EngineLimits limits = new EngineLimits.Builder().setUnits(UNIT_LIMIT).setSprites(UNIT_LIMIT).build();
        GameDefinition definition = new GameDefinition.Builder().setLimits(limits)
                                                                .useModPool(EntityKind.UNIT)
                                                                .build();
        PcgRandom random = new PcgRandom(1L, PcgRandom.DEFAULT_STREAM, PcgRandom.DEFAULT_SOURCE_COUNT);
        Game game = Game.create(definition, createSetup().setRandom(random).build());

        Assertions.assertTrue(definition.isModPool(EntityKind.UNIT));
        Assertions.assertSame(random, game.getRandom());
        Assertions.assertEquals(0, game.getUnits().getCapacity());
        Assertions.assertEquals(UNIT_LIMIT, game.getSprites().getCapacity());
        Assertions.assertEquals(UNIT_LIMIT, game.getLimits().getUnits());
    }

    @Test
    void required_option_should_have_no_default_and_fail_when_not_set() {
        GameOption<String> required = GameOption.required("scenario", String.class);

        Assertions.assertTrue(required.isRequired());
        Assertions.assertNull(required.getDefaultValue());
        Assertions.assertThrows(IllegalStateException.class, () -> GameOptions.DEFAULTS.get(required));
        Assertions.assertEquals("set", new GameOptions.Builder().set(required, "set").build().get(required));
        Assertions.assertFalse(START_RESOURCES.isRequired());
        Assertions.assertThrows(IllegalArgumentException.class, () -> GameOption.required("count", int.class));
    }

    @Test
    void setup_and_limits_should_reject_invalid_values() {
        GameSetup.Builder setup = createSetup();
        PlayerSlot first = new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> setup.addPlayer(first));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PlayerSlot(-1, ControllerKind.HUMAN, 0, 0, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setUnits(-1).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setImages(0x10000).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setPaths(-1).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> GameOption.of("count", int.class, 1));
    }

    private static GameSetup.@NotNull Builder createSetup() {
        return new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                            .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, null));
    }
}
