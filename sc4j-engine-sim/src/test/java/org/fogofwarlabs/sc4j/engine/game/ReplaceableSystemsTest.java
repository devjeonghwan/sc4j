package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.economy.StandardEconomySystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.hook.HookComposition;
import org.fogofwarlabs.sc4j.engine.hook.HookList;
import org.fogofwarlabs.sc4j.engine.hook.HookPoint;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.path.PathfindingSystem;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.random.DeterministicRandom;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tests an engine-only game without a mod whose engine systems are registered under their keys, one engine default
 * replaced by a test implementation, with hooks called in registration order inside running frame phases.
 */
final class ReplaceableSystemsTest {
    private static final int FRAME_COUNT = 400;
    private static final long SEED = 0x6a_2026L;
    private static final int UNIT_LIMIT = 12;
    private static final int MAP_SIZE = 256;
    private static final int INCOME = 3;
    private static final int UNIT_COST = 50;

    private static final @NotNull ResourceKind GOLD = new ResourceKind("gold");
    private static final @NotNull SystemKey<WorldSystem> WORLD = SystemKey.of("world", WorldSystem.class);
    private static final @NotNull HookPoint<ArrivalHook> ARRIVAL = HookPoint.of("test.arrival", ArrivalHook.class, HookComposition.EVERY_IN_ORDER);
    private static final @NotNull HookPoint<StepHook> STEP = HookPoint.of("test.step", StepHook.class, HookComposition.CHAINED);

    /**
     * Called when a unit reaches its destination.
     */
    private interface ArrivalHook {
        void onArrived(@NotNull Unit unit);
    }

    /**
     * Adjusts the step length of a unit; each hook receives the result of the one before.
     */
    private interface StepHook {
        int adjustStep(@NotNull Unit unit, int step);
    }

    /**
     * Moves the engine units one step per frame straight towards their destination in its own frame phase; calls the
     * step hooks for the step length and the arrival hooks when a unit arrives.
     */
    private static final class GridMovementSystem implements MovementSystem, FramePhase, StateHashContributor {
        private static final int BASE_STEP = 1;
        private static final int DIRECTION_COUNT = 4;

        private @Nullable EntityPool<Unit> units;
        private @Nullable HookList<ArrivalHook> arrivalHooks;
        private @Nullable HookList<StepHook> stepHooks;
        private int @NotNull [] destinationX;
        private int @NotNull [] destinationY;

        private GridMovementSystem() {
            this.units        = null;
            this.arrivalHooks = null;
            this.stepHooks    = null;
            this.destinationX = new int[0];
            this.destinationY = new int[0];
        }

        @Override
        public void attach(@NotNull Game game) {
            this.units        = game.getUnits();
            this.arrivalHooks = game.getHooks(ARRIVAL);
            this.stepHooks    = game.getHooks(STEP);
            this.destinationX = new int[game.getUnits().getCapacity() + 1];
            this.destinationY = new int[game.getUnits().getCapacity() + 1];
        }

        @Override
        public void setDestination(@NotNull Unit unit, int x, int y) {
            this.destinationX[unit.getIndex()] = x;
            this.destinationY[unit.getIndex()] = y;
        }

        @Override
        public void setTargetUnit(@NotNull Unit unit, @NotNull Unit targetUnit) {
            this.setDestination(unit, targetUnit.getPositionX(), targetUnit.getPositionY());
        }

        @Override
        public void stop(@NotNull Unit unit) {
            this.setDestination(unit, unit.getPositionX(), unit.getPositionY());
        }

        @Override
        public int getDirectionCount() {
            return DIRECTION_COUNT;
        }

        @Override
        public void turnTo(@NotNull Unit unit, int direction) {
        }

        @Override
        public boolean isAtDestination(@NotNull Unit unit) {
            return unit.getPositionX() == this.destinationX[unit.getIndex()] && unit.getPositionY() == this.destinationY[unit.getIndex()];
        }

        @Override
        public void recalculateSpeed(@NotNull Unit unit, int instructionLimit) {
        }

        @Override
        public void run(@NotNull Game game) {
            EntityPool<Unit> pool = requireValue(this.units);
            SlotList used = pool.getUsedSlots();

            for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
                this.step(pool.get(index));
            }
        }

        private void step(@NotNull Unit unit) {
            if (this.isAtDestination(unit)) {
                return;
            }

            HookList<StepHook> steps = requireValue(this.stepHooks);
            HookList<ArrivalHook> arrivals = requireValue(this.arrivalHooks);
            int step = BASE_STEP;

            for (int index = 0; index < steps.size(); index++) {
                step = steps.get(index).adjustStep(unit, step);
            }

            unit.setPosition(approach(unit.getPositionX(), this.destinationX[unit.getIndex()], step),
                             approach(unit.getPositionY(), this.destinationY[unit.getIndex()], step));

            if (this.isAtDestination(unit)) {
                for (int index = 0; index < arrivals.size(); index++) {
                    arrivals.get(index).onArrived(unit);
                }
            }
        }

        private static int approach(int value, int target, int step) {
            return value < target ? Math.min(value + step, target) : Math.max(value - step, target);
        }

        @Override
        public void contributeStateHash(@NotNull StateHash hash) {
            hash.mixArray(this.destinationX);
            hash.mixArray(this.destinationY);
        }
    }

    /**
     * Holds one amount per player and resource kind; supply is not used by the test world and stays {@code 0}.
     */
    private static final class LedgerEconomySystem implements EconomySystem, StateHashContributor {
        private int resourceKindCount;
        private int @NotNull [] amounts;

        private LedgerEconomySystem() {
            this.resourceKindCount = 0;
            this.amounts           = new int[0];
        }

        @Override
        public void attach(@NotNull Game game) {
            this.resourceKindCount = game.getContent().getDefinition().getResourceKinds().getCount();
            this.amounts           = new int[game.getPlayerCount() * this.resourceKindCount];
        }

        @Override
        public int getAmount(@NotNull Player player, @NotNull ResourceKind resourceKind) {
            return this.amounts[player.getIndex() * this.resourceKindCount + resourceKind.getIndex()];
        }

        @Override
        public void setAmount(@NotNull Player player, @NotNull ResourceKind resourceKind, int amount) {
            this.amounts[player.getIndex() * this.resourceKindCount + resourceKind.getIndex()] = amount;
        }

        @Override
        public boolean canAfford(@NotNull Player player, @NotNull ResourceCost cost) {
            for (int index = 0; index < cost.getCount(); index++) {
                if (this.getAmount(player, cost.getKind(index)) < cost.getAmount(index)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean spend(@NotNull Player player, @NotNull ResourceCost cost) {
            if (!this.canAfford(player, cost)) {
                return false;
            }

            this.refund(player, negate(cost));

            return true;
        }

        @Override
        public void refund(@NotNull Player player, @NotNull ResourceCost cost) {
            for (int index = 0; index < cost.getCount(); index++) {
                ResourceKind kind = cost.getKind(index);

                this.setAmount(player, kind, this.getAmount(player, kind) + cost.getAmount(index));
            }
        }

        private static @NotNull ResourceCost negate(@NotNull ResourceCost cost) {
            ResourceCost.Builder builder = new ResourceCost.Builder();

            for (int index = 0; index < cost.getCount(); index++) {
                builder.add(cost.getKind(index), -cost.getAmount(index));
            }

            return builder.build();
        }

        @Override
        public int getSupplyProvided(@NotNull Player player, @NotNull Faction faction) {
            return 0;
        }

        @Override
        public int getSupplyUsed(@NotNull Player player, @NotNull Faction faction) {
            return 0;
        }

        @Override
        public void addSupplyProvided(@NotNull Player player, @NotNull Faction faction, int change) {
        }

        @Override
        public void addSupplyUsed(@NotNull Player player, @NotNull Faction faction, int change) {
        }

        @Override
        public void contributeStateHash(@NotNull StateHash hash) {
            hash.mixArray(this.amounts);
        }
    }

    /**
     * Pays every player an income, buys a unit at a random place when a player can afford it and gives a unit that
     * arrived in an earlier movement phase a new random destination.
     */
    private static final class WorldSystem implements GameSystem, FramePhase {
        private static final int SPAWN_SOURCE = 1;
        private static final int DESTINATION_SOURCE = 2;

        private final int playerCount;
        private final @NotNull ResourceCost unitCost;

        private @Nullable EntityPool<Unit> units;
        private @Nullable MovementSystem movement;
        private @Nullable EconomySystem economy;
        private @Nullable DeterministicRandom random;

        private WorldSystem(@NotNull GameSetup setup) {
            this.playerCount = setup.getPlayers().size();
            this.unitCost    = ResourceCost.of(GOLD, UNIT_COST);

            this.units    = null;
            this.movement = null;
            this.economy  = null;
            this.random   = null;
        }

        @Override
        public void attach(@NotNull Game game) {
            this.units    = game.getUnits();
            this.movement = game.getMovement();
            this.economy  = game.getEconomy();
            this.random   = game.getRandom();
        }

        @Override
        public void run(@NotNull Game game) {
            EntityPool<Unit> pool = requireValue(this.units);
            MovementSystem moving = requireValue(this.movement);
            EconomySystem bank = requireValue(this.economy);
            DeterministicRandom generator = requireValue(this.random);

            for (int index = 0; index < this.playerCount; index++) {
                Player player = game.getPlayer(index);

                bank.setAmount(player, GOLD, bank.getAmount(player, GOLD) + INCOME);

                if (pool.getAllocatedCount() < pool.getCapacity() && bank.spend(player, this.unitCost)) {
                    Unit unit = pool.allocate();

                    if (unit != null) {
                        unit.setOwner(player);
                        unit.setPosition(generator.nextRange(SPAWN_SOURCE, 0, MAP_SIZE - 1), generator.nextRange(SPAWN_SOURCE, 0, MAP_SIZE - 1));
                        this.giveDestination(unit);
                    }
                }
            }

            SlotList used = pool.getUsedSlots();

            for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
                Unit unit = pool.get(index);

                if (moving.isAtDestination(unit)) {
                    this.giveDestination(unit);
                }
            }
        }

        private void giveDestination(@NotNull Unit unit) {
            DeterministicRandom generator = requireValue(this.random);

            requireValue(this.movement).setDestination(unit, generator.nextRange(DESTINATION_SOURCE, 0, MAP_SIZE - 1),
                                                       generator.nextRange(DESTINATION_SOURCE, 0, MAP_SIZE - 1));
        }
    }

    /**
     * Records the arrivals it sees under its name; it is also a system and records when it is attached.
     */
    private static final class RecordingArrivalHook implements ArrivalHook, GameSystem {
        private final @NotNull String name;
        private final @NotNull List<@NotNull String> log;

        private RecordingArrivalHook(@NotNull String name, @NotNull List<@NotNull String> log) {
            this.name = name;
            this.log  = log;
        }

        @Override
        public void attach(@NotNull Game game) {
            this.log.add(this.name + ":attach:" + game.getSystems().isAttached());
        }

        @Override
        public void onArrived(@NotNull Unit unit) {
            this.log.add(this.name + ":" + unit.getIndex());
        }
    }

    private static <T> @NotNull T requireValue(@Nullable T value) {
        Assertions.assertNotNull(value, "The system is not attached.");

        return value;
    }

    @Test
    void engine_systems_resolve_at_creation_and_a_replaced_default_keeps_its_position() {
        Game game = createGame(builder -> {
        });

        Assertions.assertInstanceOf(LedgerEconomySystem.class, game.getEconomy());
        Assertions.assertInstanceOf(GridMovementSystem.class, game.getMovement());
        Assertions.assertSame(EconomySystem.KEY, game.getSystems().getKey(0));
        Assertions.assertSame(game.getEconomy(), game.getSystems().get(EconomySystem.KEY));
        Assertions.assertTrue(game.hasSystem(MovementSystem.KEY));
        Assertions.assertFalse(game.hasSystem(PathfindingSystem.KEY));
        Assertions.assertThrows(IllegalStateException.class, game::getPathfinding);
        Assertions.assertTrue(game.getHooks(ARRIVAL).isEmpty());
    }

    @Test
    void hooks_run_in_registration_order_after_the_systems_attach() {
        List<String> log = new ArrayList<>();
        Game game = createGame(builder -> builder.addHook(ARRIVAL, context -> new RecordingArrivalHook("first", log))
                                                 .addHook(STEP, context -> (unit, step) -> step + 1)
                                                 .addHook(ARRIVAL, context -> new RecordingArrivalHook("second", log))
                                                 .addHook(STEP, context -> (unit, step) -> step * 2));

        Assertions.assertEquals(List.of("first:attach:true", "second:attach:true"), log);
        Assertions.assertEquals(2, game.getHooks(ARRIVAL).size());
        Assertions.assertEquals(4, applySteps(game.getHooks(STEP)), "(1 + 1) * 2 in registration order");

        runFrames(game, FRAME_COUNT);

        List<String> arrivals = log.subList(2, log.size());

        Assertions.assertFalse(arrivals.isEmpty(), "no unit arrived");
        Assertions.assertEquals(0, arrivals.size() % 2);

        for (int index = 0; index < arrivals.size(); index += 2) {
            String unit = arrivals.get(index).substring("first:".length());

            Assertions.assertEquals("first:" + unit, arrivals.get(index));
            Assertions.assertEquals("second:" + unit, arrivals.get(index + 1));
        }
    }

    @Test
    void hooks_that_do_nothing_leave_the_state_hash_unchanged() {
        long[] withoutHooks = runFrames(createGame(builder -> {
        }), FRAME_COUNT);
        long[] withIdleHooks = runFrames(createGame(builder -> builder.addHook(ARRIVAL, context -> unit -> {
                                                                      })
                                                                      .addHook(STEP, context -> (unit, step) -> step)), FRAME_COUNT);
        long[] withLongerSteps = runFrames(createGame(builder -> builder.addHook(STEP, context -> (unit, step) -> step + 1)), FRAME_COUNT);

        Assertions.assertArrayEquals(withoutHooks, withIdleHooks);
        Assertions.assertNotEquals(withoutHooks[FRAME_COUNT - 1], withLongerSteps[FRAME_COUNT - 1]);
        Assertions.assertTrue(Arrays.stream(withoutHooks).distinct().count() > FRAME_COUNT / 2, "the hash hardly changes");
    }

    private static int applySteps(@NotNull HookList<StepHook> hooks) {
        Unit unit = new Unit(1);
        int step = 1;

        for (int index = 0; index < hooks.size(); index++) {
            step = hooks.get(index).adjustStep(unit, step);
        }

        return step;
    }

    private static @NotNull Game createGame(@NotNull Consumer<GameDefinition.@NotNull Builder> hooks) {
        GameDefinition.Builder builder = new GameDefinition.Builder().setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build())
                                                                     .useSystem(EconomySystem.KEY, context -> new StandardEconomySystem())
                                                                     .useSystem(MovementSystem.KEY, context -> new GridMovementSystem())
                                                                     .useSystem(WORLD, context -> new WorldSystem(context.getSetup()))
                                                                     .useSystem(EconomySystem.KEY, context -> new LedgerEconomySystem())
                                                                     .addFramePhase(WORLD)
                                                                     .addFramePhase(MovementSystem.KEY, movement -> (GridMovementSystem) movement);

        builder.getContent().getResourceKinds().register(GOLD);
        hooks.accept(builder);

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, null))
                .setSeed(SEED)
                .build();

        return Game.create(builder.build(), setup);
    }

    private static long @NotNull [] runFrames(@NotNull Game game, int count) {
        long[] hashes = new long[count];

        game.getFrameRunner().setSyncHashListener((frame, hash) -> hashes[frame - 1] = hash, 1);

        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }

        return hashes;
    }
}
