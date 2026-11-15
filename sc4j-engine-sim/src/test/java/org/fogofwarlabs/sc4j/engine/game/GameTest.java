package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.EntityKind;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnReset;
import org.fogofwarlabs.sc4j.engine.entity.column.IntColumn;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.random.DeterministicRandom;
import org.fogofwarlabs.sc4j.engine.random.RandomState;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests an engine-only game without a mod: an empty map, three players, the engine default random generator and one
 * trivial system that spawns, moves and removes units with an extension column, run headless for 1000 frames.
 */
final class GameTest {
    private static final int FRAME_COUNT = 1000;
    private static final int HALF_FRAME_COUNT = FRAME_COUNT / 2;
    private static final long SEED = 0x5eed_2026L;
    private static final int UNIT_LIMIT = 48;
    private static final int MAP_SIZE = 2048;

    private static final @NotNull ColumnKey<IntColumn> ENERGY = ColumnKey.ofInt("energy", 120, ColumnReset.ON_ALLOCATE);
    private static final @NotNull ColumnKey<IntColumn> LIVES = ColumnKey.ofInt("lives", 0, ColumnReset.KEEP);
    private static final @NotNull SystemKey<WanderSystem> WANDER = SystemKey.of("wander", WanderSystem.class);
    private static final @NotNull UnitType @NotNull [] UNIT_TYPES = {new UnitType("first"), new UnitType("second"), new UnitType("third"),
                                                                     new UnitType("fourth")};

    /**
     * Spawns units at random places, moves every unit a random step per frame, drains its energy and removes it when
     * the energy is gone; counts the uses of each slot in a column that survives reuse.
     */
    private static final class WanderSystem implements GameSystem, FramePhase, StateHashContributor {
        private static final int SPAWN_SOURCE = 1;
        private static final int OWNER_SOURCE = 2;
        private static final int POSITION_SOURCE = 3;
        private static final int STEP_SOURCE = 4;
        private static final int DRAIN_SOURCE = 5;
        private static final int SPAWN_CHANCE = 3;
        private static final int MAXIMUM_STEP = 6;
        private static final int MAXIMUM_DRAIN = 4;

        private static <T> @NotNull T requireValue(@Nullable T value) {
            Assertions.assertNotNull(value, "The system is not attached.");

            return value;
        }

        private final int playerCount;
        private final @NotNull List<@NotNull String> lifecycle;

        private @Nullable EntityPool<Unit> units;
        private @Nullable IntColumn energy;
        private @Nullable IntColumn lives;
        private @Nullable DeterministicRandom random;
        private int spawnCount;
        private int removeCount;

        private WanderSystem(@NotNull GameSetup setup) {
            this.playerCount = setup.getPlayers().size();
            this.lifecycle   = new ArrayList<>();

            this.units       = null;
            this.energy      = null;
            this.lives       = null;
            this.random      = null;
            this.spawnCount  = 0;
            this.removeCount = 0;
        }

        @Override
        public void attach(@NotNull Game game) {
            this.units  = game.getUnits();
            this.energy = game.getUnits().getColumn(ENERGY);
            this.lives  = game.getUnits().getColumn(LIVES);
            this.random = game.getRandom();
        }

        @Override
        public void run(@NotNull Game game) {
            EntityPool<Unit> pool = requireValue(this.units);
            IntColumn energyColumn = requireValue(this.energy);
            IntColumn livesColumn = requireValue(this.lives);
            DeterministicRandom generator = requireValue(this.random);

            if (generator.nextRange(SPAWN_SOURCE, 0, SPAWN_CHANCE) == 0) {
                Unit unit = pool.allocate();

                if (unit != null) {
                    unit.setOwner(game.getPlayer(generator.nextRange(OWNER_SOURCE, 0, this.playerCount - 1)));
                    unit.setType(UNIT_TYPES[this.spawnCount % UNIT_TYPES.length]);
                    unit.setPosition(generator.nextRange(POSITION_SOURCE, 0, MAP_SIZE - 1), generator.nextRange(POSITION_SOURCE, 0, MAP_SIZE - 1));
                    livesColumn.set(unit.getIndex(), livesColumn.get(unit.getIndex()) + 1);
                    this.spawnCount++;
                    this.lifecycle.add("+" + unit.getIndex());
                }
            }

            SlotList used = pool.getUsedSlots();
            int index = used.getFirst();

            while (index != SlotList.NONE) {
                int next = used.getNext(index);
                Unit unit = pool.get(index);
                int stepX = generator.nextRange(STEP_SOURCE, -MAXIMUM_STEP, MAXIMUM_STEP);
                int stepY = generator.nextRange(STEP_SOURCE, -MAXIMUM_STEP, MAXIMUM_STEP);
                int remaining = energyColumn.get(index) - generator.nextRange(DRAIN_SOURCE, 1, MAXIMUM_DRAIN);

                unit.setPosition(Math.floorMod(unit.getPositionX() + stepX, MAP_SIZE), Math.floorMod(unit.getPositionY() + stepY, MAP_SIZE));
                energyColumn.set(index, remaining);

                if (remaining <= 0) {
                    pool.free(unit);
                    this.removeCount++;
                    this.lifecycle.add("-" + index);
                }

                index = next;
            }
        }

        @Override
        public void contributeStateHash(@NotNull StateHash hash) {
            hash.mix(this.spawnCount);
            hash.mix(this.removeCount);
        }
    }

    @Test
    void engine_only_game_hash_is_stable_across_two_runs() {
        Game first = createGame(SEED);
        Game second = createGame(SEED);
        long[] firstHashes = runFrames(first, FRAME_COUNT);
        long[] secondHashes = runFrames(second, FRAME_COUNT);
        WanderSystem wander = first.getModSystem(WANDER);

        Assertions.assertArrayEquals(firstHashes, secondHashes);
        Assertions.assertEquals(wander.lifecycle, second.getModSystem(WANDER).lifecycle);
        Assertions.assertEquals(FRAME_COUNT, first.getClock().getFrame());
        Assertions.assertTrue(wander.spawnCount > UNIT_LIMIT, () -> "spawned only " + wander.spawnCount);
        Assertions.assertTrue(wander.removeCount > 0, "no unit was removed");
        Assertions.assertTrue(Arrays.stream(firstHashes).distinct().count() > FRAME_COUNT / 2, "the hash hardly changes");
        Assertions.assertNotEquals(firstHashes[FRAME_COUNT - 1], runFrames(createGame(SEED + 1L), FRAME_COUNT)[FRAME_COUNT - 1]);
    }

    @Test
    void restoring_the_saved_random_state_keeps_the_remaining_frames_equal() {
        long[] expected = runFrames(createGame(SEED), FRAME_COUNT);
        Game restored = createGame(SEED);
        Game disturbed = createGame(SEED);

        runFrames(restored, HALF_FRAME_COUNT);
        runFrames(disturbed, HALF_FRAME_COUNT);

        RandomState saved = restored.getRandom().saveState();

        drawOutsideFrames(restored.getRandom());
        restored.getRandom().restoreState(saved);
        drawOutsideFrames(disturbed.getRandom());

        long[] restoredHashes = runFrames(restored, HALF_FRAME_COUNT);
        long[] disturbedHashes = runFrames(disturbed, HALF_FRAME_COUNT);

        Assertions.assertArrayEquals(Arrays.copyOfRange(expected, HALF_FRAME_COUNT, FRAME_COUNT), restoredHashes);
        Assertions.assertNotEquals(expected[FRAME_COUNT - 1], disturbedHashes[HALF_FRAME_COUNT - 1]);
    }

    @Test
    void state_snapshot_refuses_a_system_that_hashes_state_it_does_not_write() {
        Game game = createGame(SEED);

        runFrames(game, HALF_FRAME_COUNT);

        Assertions.assertThrows(IllegalStateException.class, () -> StateSnapshot.capture(game));
    }

    @Test
    void units_reuse_slots_in_the_fixed_pool_order() {
        Game game = createGame(SEED);
        EntityPool<Unit> units = game.getUnits();
        IntColumn lives = units.getColumn(LIVES);

        runFrames(game, FRAME_COUNT);

        List<String> lifecycle = game.getModSystem(WANDER).lifecycle;
        List<Integer> freeOrder = new ArrayList<>();
        int nextUnusedIndex = 1;

        for (String event : lifecycle) {
            int index = Integer.parseInt(event.substring(1));

            if (event.startsWith("-")) {
                freeOrder.addFirst(index);
            } else if (freeOrder.isEmpty()) {
                Assertions.assertEquals(nextUnusedIndex, index, "a spawn without freed slots takes the lowest unused slot");
                nextUnusedIndex++;
            } else {
                int expectedIndex = freeOrder.removeFirst();

                Assertions.assertEquals(expectedIndex, index, "a spawn reuses the most recently freed slot");
            }
        }

        int totalLives = 0;

        for (int index = 1; index <= units.getCapacity(); index++) {
            totalLives += lives.get(index);
        }

        Assertions.assertEquals(game.getModSystem(WANDER).spawnCount, totalLives);
    }

    @Test
    void every_player_slot_has_one_player_object() {
        Game game = createGame(SEED);

        Assertions.assertEquals(3, game.getPlayerCount());
        Assertions.assertSame(game.getPlayer(2), game.getPlayer(2));
        Assertions.assertEquals(1, game.getPlayer(1).getIndex());
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> game.getPlayer(3));
    }

    private static @NotNull Game createGame(long seed) {
        GameDefinition.Builder builder = new GameDefinition.Builder().setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build())
                                                                     .addColumn(EntityKind.UNIT, ENERGY)
                                                                     .addColumn(EntityKind.UNIT, LIVES)
                                                                     .useSystem(WANDER, context -> new WanderSystem(context.getSetup()))
                                                                     .addFramePhase(WANDER);

        for (UnitType type : UNIT_TYPES) {
            builder.getContent().getUnitTypes().register(type, new UnitDefinition.Builder().build());
        }

        GameDefinition definition = builder.build();
        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, null))
                .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                .setSeed(seed)
                .build();

        return Game.create(definition, setup);
    }

    private static long @NotNull [] runFrames(@NotNull Game game, int count) {
        long[] hashes = new long[count];
        int firstFrame = game.getClock().getFrame();

        game.getFrameRunner().setSyncHashListener((frame, hash) -> hashes[frame - firstFrame - 1] = hash, 1);

        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }

        return hashes;
    }

    private static void drawOutsideFrames(@NotNull DeterministicRandom random) {
        boolean previous = random.setActive(true);

        for (int draw = 0; draw < 10; draw++) {
            random.next(0);
        }

        random.setActive(previous);
    }
}
