package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.engine.combat.AttackBehavior;
import org.fogofwarlabs.sc4j.engine.combat.CombatSystem;
import org.fogofwarlabs.sc4j.engine.combat.GuardBehavior;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.CompletedUnitCondition;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.DeclarativeRequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.DeclarativeRequirements;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.Requirement;
import org.fogofwarlabs.sc4j.engine.content.table.PlayerProgressTable;
import org.fogofwarlabs.sc4j.engine.content.type.*;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.economy.StandardEconomySystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.lifecycle.UnitAccountingUtils;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.order.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Tests the engine default production system in an engine-only game: the queue spends the cost, waits for supply and requirements and creates
 * the unit after its build time, holds the mod's number of entries, cancelling refunds the mod's share and the held supply and keeps the order
 * of the other entries, the supply can be held from the enqueue, a producer that leaves the game gives its held supply back, research and
 * upgrades finish after their time, a trained unit walks to its producer's rally point with the phase instruction limit, and a trained unit
 * acquires and kills an enemy with the engine combat default.
 */
final class StandardProductionSystemTest {
    private static final int UNIT_LIMIT = 16;
    private static final int ORDER_LIMIT = 16;
    private static final int INSTRUCTION_LIMIT = 0;
    private static final int PHASE_INSTRUCTION_LIMIT = 7;
    private static final int FRAME_LIMIT = 400;

    private static final int SOLDIER_COST = 50;
    private static final int SOLDIER_TIME = 10;
    private static final int SOLDIER_SUPPLY = 2;
    private static final int HALL_SUPPLY = 10;
    private static final int TECH_COST = 100;
    private static final int TECH_TIME = 20;
    private static final int UPGRADE_BASE_COST = 10;
    private static final int UPGRADE_COST_FACTOR = 10;
    private static final int UPGRADE_TIME = 5;
    private static final int UPGRADE_LEVELS = 3;
    private static final int SMALL_QUEUE_CAPACITY = 3;
    private static final int LARGE_QUEUE_CAPACITY = 8;
    private static final int HALF_REFUND_PERCENT = 50;
    private static final int PERCENT = 100;
    private static final int GOLD_IN_SOLDIERS = 10;

    /**
     * Runs a behavior and records the instruction limit each start of its order gets.
     */
    private static final class InstructionLimitRecorder implements OrderBehavior<OrderContext, Unit> {
        private final @NotNull OrderBehavior<OrderContext, Unit> behavior;
        private final @NotNull List<@NotNull Integer> startLimits;

        private InstructionLimitRecorder(@NotNull OrderBehavior<OrderContext, Unit> behavior, @NotNull List<@NotNull Integer> startLimits) {
            this.behavior    = behavior;
            this.startLimits = startLimits;
        }

        @Override
        public void start(@NotNull OrderContext context, @NotNull Unit unit) {
            this.startLimits.add(context.getInstructionLimit());
            this.behavior.start(context, unit);
        }

        @Override
        public void update(@NotNull OrderContext context, @NotNull Unit unit) {
            this.behavior.update(context, unit);
        }
    }

    private final @NotNull ResourceKind ore;
    private final @NotNull Faction humans;
    private final @NotNull OrderType guard;
    private final @NotNull OrderType attack;
    private final @NotNull OrderType move;
    private final @NotNull MotionProfile walker;
    private final @NotNull WeaponType rifle;
    private final @NotNull UnitType hall;
    private final @NotNull UnitType barracks;
    private final @NotNull UnitType academy;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType knight;
    private final @NotNull UnitType peasant;
    private final @NotNull TechType shieldWall;
    private final @NotNull UpgradeType plating;

    private final @NotNull List<@NotNull String> slotEvents;
    private final @NotNull List<@NotNull Integer> moveStartLimits;

    StandardProductionSystemTest() {
        this.ore        = new ResourceKind("ore");
        this.humans     = new Faction("humans");
        this.guard      = new OrderType("guard");
        this.attack     = new OrderType("attack");
        this.move       = new OrderType("move");
        this.walker     = new MotionProfile("walker");
        this.rifle      = new WeaponType("rifle");
        this.hall       = new UnitType("hall");
        this.barracks   = new UnitType("barracks");
        this.academy    = new UnitType("academy");
        this.soldier    = new UnitType("soldier");
        this.knight     = new UnitType("knight");
        this.peasant    = new UnitType("peasant");
        this.shieldWall = new TechType("shield wall");
        this.plating    = new UpgradeType("plating");

        this.slotEvents      = new ArrayList<>();
        this.moveStartLimits = new ArrayList<>();
    }

    @Test
    void queued_units_spend_their_cost_and_complete_after_their_build_time() {
        Game game = this.createGame(INSTRUCTION_LIMIT);
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createBase(game, player);

        game.getEconomy().setAmount(player, this.ore, 2 * SOLDIER_COST + 20);

        Assertions.assertEquals(0, production.enqueueUnit(producer, this.soldier));
        Assertions.assertEquals(1, production.enqueueUnit(producer, this.soldier));
        Assertions.assertEquals(-1, production.enqueueUnit(producer, this.soldier));
        Assertions.assertEquals(20, game.getEconomy().getAmount(player, this.ore));
        Assertions.assertEquals(2, production.getQueueLength(producer));

        this.runFrames(game, SOLDIER_TIME - 1);

        Assertions.assertEquals(1, production.getRemainingTime(producer));
        Assertions.assertEquals(0, game.getContent().getProgress().getUnitCount(player, this.soldier));

        this.runFrames(game, 1);

        Assertions.assertEquals(1, game.getContent().getProgress().getCompletedUnitCount(player, this.soldier));
        Assertions.assertEquals(1, production.getQueueLength(producer));
        Assertions.assertEquals(List.of("cleared 0", "moved 1 to 0"), this.slotEvents);

        this.runFrames(game, SOLDIER_TIME);

        Assertions.assertEquals(2, game.getContent().getProgress().getUnitCount(player, this.soldier));
        Assertions.assertEquals(2 * SOLDIER_SUPPLY, game.getEconomy().getSupplyUsed(player, this.humans));
        Assertions.assertEquals(0, production.getQueueLength(producer));

        Unit trained = this.findUnit(game, this.soldier);

        Assertions.assertSame(this.guard, trained.getOrderType());
        Assertions.assertEquals(producer.getPositionX(), trained.getPositionX());
        Assertions.assertTrue(trained.getHitPoints() > 0);
    }

    @Test
    void cancel_refunds_the_entry_and_moves_the_later_entries_up() {
        Game game = this.createGame(INSTRUCTION_LIMIT);
        Player player = game.getPlayer(0);
        ProductionSystem production = game.getProduction();
        Unit producer = this.createBase(game, player);

        game.getEconomy().setAmount(player, this.ore, 2 * SOLDIER_COST);
        game.getContent().getProgress().setUnitAvailable(player, this.knight, true);
        production.enqueueUnit(producer, this.soldier);
        this.runFrames(game, 1);

        Assertions.assertTrue(production.cancel(producer, 0, INSTRUCTION_LIMIT));
        Assertions.assertFalse(production.cancel(producer, 0, INSTRUCTION_LIMIT));
        Assertions.assertEquals(2 * SOLDIER_COST, game.getEconomy().getAmount(player, this.ore));
        Assertions.assertEquals(0, game.getEconomy().getSupplyUsed(player, this.humans));

        production.enqueueUnit(producer, this.soldier);
        production.enqueueUnit(producer, this.soldier);
        this.slotEvents.clear();

        Assertions.assertTrue(production.cancel(producer, 0, INSTRUCTION_LIMIT));
        Assertions.assertEquals(List.of("moved 1 to 0"), this.slotEvents);
        Assertions.assertEquals(1, production.getQueueLength(producer));
        Assertions.assertSame(this.soldier, production.getQueuedType(producer, 0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> production.getQueuedType(producer, 1));
    }

    @Test
    void a_full_queue_refuses_the_next_entry_and_takes_nothing() {
        Game game = this.createGame(INSTRUCTION_LIMIT, SMALL_QUEUE_CAPACITY, SMALL_QUEUE_CAPACITY, UnaryOperator.identity());
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createBase(game, player);

        game.getEconomy().setAmount(player, this.ore, GOLD_IN_SOLDIERS * SOLDIER_COST);

        Assertions.assertEquals(SMALL_QUEUE_CAPACITY, production.getQueueCapacity());

        for (int slot = 0; slot < SMALL_QUEUE_CAPACITY; slot++) {
            Assertions.assertEquals(slot, production.enqueueUnit(producer, this.soldier));
        }

        Assertions.assertEquals(-1, production.enqueueUnit(producer, this.soldier));
        Assertions.assertEquals(SMALL_QUEUE_CAPACITY, production.getQueueLength(producer));
        Assertions.assertEquals((GOLD_IN_SOLDIERS - SMALL_QUEUE_CAPACITY) * SOLDIER_COST, game.getEconomy().getAmount(player, this.ore));
    }

    @Test
    void a_queue_capacity_needs_its_entry_columns() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new StandardProductionSystem.Builder(0));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new StandardProductionSystem.Builder(StandardProductionSystem.MAXIMUM_QUEUE_CAPACITY + 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> this.createGame(INSTRUCTION_LIMIT, SMALL_QUEUE_CAPACITY, LARGE_QUEUE_CAPACITY, UnaryOperator.identity()));
    }

    @Test
    void cancel_refunds_the_mod_share_and_keeps_the_order_of_the_other_entries() {
        Game game = this.createGame(INSTRUCTION_LIMIT, SkirmishScenario.QUEUE_CAPACITY, SkirmishScenario.QUEUE_CAPACITY,
                                    settings -> settings.setQueueRefundPercent(HALF_REFUND_PERCENT));
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createBase(game, player);

        game.getEconomy().setAmount(player, this.ore, 3 * SOLDIER_COST);
        game.getContent().getProgress().setUnitAvailable(player, this.knight, true);
        production.createUnit(player, this.academy, 300, 100, INSTRUCTION_LIMIT);
        production.enqueueUnit(producer, this.soldier);
        production.enqueueUnit(producer, this.knight);
        production.enqueueUnit(producer, this.soldier);
        this.runFrames(game, 1);

        Assertions.assertTrue(production.isQueueStarted(producer));
        Assertions.assertEquals(SOLDIER_SUPPLY, game.getEconomy().getSupplyUsed(player, this.humans));
        Assertions.assertTrue(production.cancel(producer, 1, INSTRUCTION_LIMIT));
        Assertions.assertEquals(SOLDIER_COST * HALF_REFUND_PERCENT / PERCENT, game.getEconomy().getAmount(player, this.ore));
        Assertions.assertEquals(SOLDIER_SUPPLY, game.getEconomy().getSupplyUsed(player, this.humans), "a waiting entry holds no supply");
        Assertions.assertEquals(List.of(this.soldier, this.soldier),
                                List.of(production.getQueuedType(producer, 0), production.getQueuedType(producer, 1)));
        Assertions.assertTrue(production.isQueueStarted(producer), "the started front entry goes on");

        Assertions.assertTrue(production.cancel(producer, 0, INSTRUCTION_LIMIT));
        Assertions.assertEquals(0, game.getEconomy().getSupplyUsed(player, this.humans), "the started entry gives its supply back");
        Assertions.assertEquals(SOLDIER_COST, game.getEconomy().getAmount(player, this.ore));
        Assertions.assertFalse(production.isQueueStarted(producer));
        Assertions.assertEquals(1, production.getQueueLength(producer));
    }

    @Test
    void supply_held_when_queued_is_taken_at_the_enqueue_and_given_back_by_a_cancel() {
        Game game = this.createGame(INSTRUCTION_LIMIT, LARGE_QUEUE_CAPACITY, LARGE_QUEUE_CAPACITY, settings -> settings.setSupplyHeldWhenQueued(true));
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createBase(game, player);
        int fitting = HALL_SUPPLY / SOLDIER_SUPPLY;

        game.getEconomy().setAmount(player, this.ore, (fitting + 1) * SOLDIER_COST);

        for (int slot = 0; slot < fitting; slot++) {
            Assertions.assertEquals(slot, production.enqueueUnit(producer, this.soldier));
        }

        Assertions.assertEquals(-1, production.enqueueUnit(producer, this.soldier), "no supply left");
        Assertions.assertFalse(production.isWaitingForSupply(producer), "an entry that holds its supply does not wait for it");
        Assertions.assertEquals(SOLDIER_COST, game.getEconomy().getAmount(player, this.ore));
        Assertions.assertEquals(HALL_SUPPLY, game.getEconomy().getSupplyUsed(player, this.humans));

        Assertions.assertTrue(production.cancel(producer, fitting - 1, INSTRUCTION_LIMIT));
        Assertions.assertEquals(HALL_SUPPLY - SOLDIER_SUPPLY, game.getEconomy().getSupplyUsed(player, this.humans));

        this.runFrames(game, SOLDIER_TIME);

        Assertions.assertEquals(1, game.getContent().getProgress().getUnitCount(player, this.soldier));
        Assertions.assertEquals(HALL_SUPPLY - SOLDIER_SUPPLY, game.getEconomy().getSupplyUsed(player, this.humans),
                                "a made unit keeps the supply its entry held");
    }

    @Test
    void a_producer_that_leaves_the_game_gives_back_the_supply_its_entries_hold() {
        Game game = this.createGame(INSTRUCTION_LIMIT);
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createBase(game, player);

        game.getEconomy().setAmount(player, this.ore, 2 * SOLDIER_COST);
        production.enqueueUnit(producer, this.soldier);
        production.enqueueUnit(producer, this.soldier);
        this.runFrames(game, 1);

        Assertions.assertEquals(SOLDIER_SUPPLY, game.getEconomy().getSupplyUsed(player, this.humans));

        UnitAccountingUtils.removeUnit(game, producer, true);

        Assertions.assertEquals(0, game.getEconomy().getSupplyUsed(player, this.humans));
        Assertions.assertEquals(0, game.getEconomy().getAmount(player, this.ore), "the spent cost is lost");

        this.runFrames(game, 2 * SOLDIER_TIME);

        Assertions.assertEquals(0, game.getContent().getProgress().getUnitCount(player, this.soldier));
    }

    @Test
    void requirements_and_supply_hold_production_back() {
        Game game = this.createGame(INSTRUCTION_LIMIT);
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = production.createUnit(player, this.barracks, 100, 100, INSTRUCTION_LIMIT);

        Assertions.assertNotNull(producer);

        game.getEconomy().setAmount(player, this.ore, 10 * SOLDIER_COST);
        game.getContent().getProgress().setUnitAvailable(player, this.knight, true);

        Assertions.assertEquals(-1, production.enqueueUnit(producer, this.knight));
        Assertions.assertEquals(0, production.enqueueUnit(producer, this.soldier));

        this.runFrames(game, 2 * SOLDIER_TIME);

        Assertions.assertEquals(0, game.getContent().getProgress().getUnitCount(player, this.soldier));
        Assertions.assertEquals(0, production.getRemainingTime(producer));
        Assertions.assertTrue(production.isWaitingForSupply(producer), "the front entry waits for supply");

        production.createUnit(player, this.hall, 200, 100, INSTRUCTION_LIMIT);
        production.createUnit(player, this.academy, 300, 100, INSTRUCTION_LIMIT);

        Assertions.assertFalse(production.isWaitingForSupply(producer), "the supply is there");
        Assertions.assertEquals(1, production.enqueueUnit(producer, this.knight));

        this.runFrames(game, 2 * SOLDIER_TIME);

        Assertions.assertEquals(1, game.getContent().getProgress().getUnitCount(player, this.soldier));
        Assertions.assertEquals(1, game.getContent().getProgress().getUnitCount(player, this.knight));
    }

    @Test
    void research_and_upgrades_finish_after_their_time() {
        Game game = this.createGame(INSTRUCTION_LIMIT);
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        PlayerProgressTable progress = game.getContent().getProgress();
        Unit producer = this.createBase(game, player);

        game.getEconomy().setAmount(player, this.ore, TECH_COST + UPGRADE_BASE_COST + UPGRADE_BASE_COST + UPGRADE_COST_FACTOR);
        progress.setTechAvailable(player, this.shieldWall, true);
        progress.setMaximumUpgradeLevel(player, this.plating, UPGRADE_LEVELS);

        Assertions.assertTrue(production.startResearch(producer, this.shieldWall, INSTRUCTION_LIMIT));
        Assertions.assertFalse(production.startUpgrade(producer, this.plating, INSTRUCTION_LIMIT));
        Assertions.assertTrue(production.isResearching(producer));

        this.runFrames(game, TECH_TIME);

        Assertions.assertTrue(progress.isTechResearched(player, this.shieldWall));
        Assertions.assertFalse(production.startResearch(producer, this.shieldWall, INSTRUCTION_LIMIT));
        Assertions.assertTrue(production.startUpgrade(producer, this.plating, INSTRUCTION_LIMIT));

        this.runFrames(game, UPGRADE_TIME);

        Assertions.assertEquals(1, progress.getUpgradeLevel(player, this.plating));
        Assertions.assertTrue(production.startUpgrade(producer, this.plating, INSTRUCTION_LIMIT));
        Assertions.assertEquals(0, game.getEconomy().getAmount(player, this.ore));

        this.runFrames(game, UPGRADE_TIME + UPGRADE_TIME);

        Assertions.assertEquals(2, progress.getUpgradeLevel(player, this.plating));
        Assertions.assertFalse(production.isResearching(producer));
    }

    @Test
    void trained_units_walk_to_the_rally_point_of_their_producer() {
        Game game = this.createGame(INSTRUCTION_LIMIT);
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createBase(game, player);
        MutablePoint point = new MutablePoint();

        Assertions.assertFalse(production.getRallyPoint(producer, point));

        production.setRallyPoint(producer, 100, 220);

        Assertions.assertTrue(production.getRallyPoint(producer, point));
        Assertions.assertEquals(220, point.getY());

        game.getEconomy().setAmount(player, this.ore, SOLDIER_COST);
        production.enqueueUnit(producer, this.soldier);
        this.runFrames(game, SOLDIER_TIME);

        Unit trained = this.findUnit(game, this.soldier);

        Assertions.assertSame(this.move, trained.getOrderType());

        this.runFrames(game, FRAME_LIMIT);

        Assertions.assertSame(this.guard, trained.getOrderType());
        Assertions.assertEquals(220, trained.getPositionY());

        production.clearRallyPoint(producer);

        Assertions.assertFalse(production.getRallyPoint(producer, point));
    }

    @Test
    void trained_units_start_their_rally_order_with_the_phase_instruction_limit() {
        Game game = this.createGame(PHASE_INSTRUCTION_LIMIT);
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createBase(game, player);

        production.setRallyPoint(producer, 100, 220);
        game.getEconomy().setAmount(player, this.ore, SOLDIER_COST);
        production.enqueueUnit(producer, this.soldier);
        this.runFrames(game, SOLDIER_TIME);

        Assertions.assertSame(this.move, this.findUnit(game, this.soldier).getOrderType());
        Assertions.assertEquals(List.of(PHASE_INSTRUCTION_LIMIT), this.moveStartLimits);
    }

    @Test
    void a_trained_soldier_acquires_and_kills_an_enemy() {
        Game game = this.createGame(INSTRUCTION_LIMIT);
        Player player = game.getPlayer(0);
        Player enemy = game.getPlayer(1);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createBase(game, player);
        Unit victim = production.createUnit(enemy, this.peasant, 160, 100, INSTRUCTION_LIMIT);

        Assertions.assertNotNull(victim);

        game.getEconomy().setAmount(player, this.ore, SOLDIER_COST);
        production.enqueueUnit(producer, this.soldier);

        for (int frame = 0; frame < FRAME_LIMIT && game.getUnits().isAllocated(victim.getIndex()); frame++) {
            game.getFrameRunner().runFrame();
        }

        Unit trained = this.findUnit(game, this.soldier);

        Assertions.assertFalse(game.getUnits().isAllocated(victim.getIndex()));
        Assertions.assertEquals(0, game.getContent().getProgress().getUnitCount(enemy, this.peasant));
        Assertions.assertTrue(trained.getPositionX() > producer.getPositionX());

        game.getFrameRunner().runFrame();

        Assertions.assertSame(this.guard, trained.getOrderType());
    }

    private @NotNull Unit createBase(@NotNull Game game, @NotNull Player player) {
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();

        Assertions.assertNotNull(production.createUnit(player, this.hall, 40, 100, INSTRUCTION_LIMIT));

        Unit producer = production.createUnit(player, this.barracks, 100, 100, INSTRUCTION_LIMIT);

        Assertions.assertNotNull(producer);
        Assertions.assertEquals(HALL_SUPPLY, game.getEconomy().getSupplyProvided(player, this.humans));

        return producer;
    }

    private @NotNull Unit findUnit(@NotNull Game game, @NotNull UnitType type) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            if (units.get(index).getType() == type) {
                return units.get(index);
            }
        }

        return Assertions.fail("No unit of " + type);
    }

    private void runFrames(@NotNull Game game, int count) {
        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }
    }

    private @NotNull Game createGame(int phaseInstructionLimit) {
        return this.createGame(phaseInstructionLimit, SkirmishScenario.QUEUE_CAPACITY, SkirmishScenario.QUEUE_CAPACITY, UnaryOperator.identity());
    }

    private @NotNull Game createGame(int phaseInstructionLimit, int columnCapacity, int queueCapacity,
                                     @NotNull UnaryOperator<StandardProductionSystem.@NotNull Builder> settings) {
        DeclarativeRequirements requirements = new DeclarativeRequirements.Builder()
                .requireBuild(this.knight, Requirement.allOf(new CompletedUnitCondition(this.academy)))
                .build();
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(EconomySystem.KEY, context -> new StandardEconomySystem())
                .useSystem(RequirementSystem.KEY, context -> new DeclarativeRequirementSystem(requirements))
                .useSystem(CombatSystem.KEY, context -> new StandardCombatSystem())
                .useSystem(ProductionSystem.KEY, context -> {
                    StandardProductionSystem.Builder production = new StandardProductionSystem.Builder(queueCapacity)
                            .setRallyOrder(this.move)
                            .setPhaseInstructionLimit(phaseInstructionLimit);

                    return settings.apply(production).build();
                })
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .addHook(ProductionSystem.QUEUE_SLOT_CLEARED, context -> (producer, slot) -> this.slotEvents.add("cleared " + slot))
                .addHook(ProductionSystem.QUEUE_SLOT_MOVED, context -> this::recordSlotMoved)
                .addFramePhase(StandardProductionSystemTest::updateOrders)
                .addFramePhase(ProductionSystem.KEY, production -> (StandardProductionSystem) production)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        StandardCombatSystem.addColumns(builder);
        StandardProductionSystem.addColumns(builder, columnCapacity);
        builder.getContent().getResourceKinds().register(this.ore);
        builder.getContent().getFactions().register(this.humans, new FactionDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.guard, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.attack, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.move, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512)
                                                                                                            .setAcceleration(64)
                                                                                                            .setStoppingDistance(2048)
                                                                                                            .setTurnRate(40)
                                                                                                            .build());
        builder.getContent().getWeaponTypes().register(this.rifle, new WeaponDefinition.Builder().setDamage(6)
                                                                                                 .setDamageFactor(1)
                                                                                                 .setCooldown(15)
                                                                                                 .setMaximumRange(32)
                                                                                                 .addTarget(WeaponTarget.GROUND)
                                                                                                 .build());
        builder.getContent().getUnitTypes().register(this.hall, this.createBuilding().setSupplyProvided(HALL_SUPPLY).build());
        builder.getContent().getUnitTypes().register(this.barracks, this.createBuilding().build());
        builder.getContent().getUnitTypes().register(this.academy, this.createBuilding().build());
        builder.getContent().getUnitTypes().register(this.soldier, this.createFighter().build());
        builder.getContent().getUnitTypes().register(this.knight, this.createFighter().build());
        builder.getContent().getUnitTypes().register(this.peasant, new UnitDefinition.Builder().setFaction(this.humans)
                                                                                               .setMotionProfile(this.walker)
                                                                                               .setHitPoints(10)
                                                                                               .build());
        builder.getContent().getTechTypes().register(this.shieldWall, new TechDefinition.Builder().setCost(ResourceCost.of(this.ore, TECH_COST))
                                                                                                  .setResearchTime(TECH_TIME)
                                                                                                  .build());
        builder.getContent().getUpgradeTypes().register(this.plating, new UpgradeDefinition.Builder()
                .setBaseCost(ResourceCost.of(this.ore, UPGRADE_BASE_COST))
                .setCostFactor(ResourceCost.of(this.ore, UPGRADE_COST_FACTOR))
                .setBaseTime(UPGRADE_TIME)
                .setTimeFactor(UPGRADE_TIME)
                .setMaximumLevel(UPGRADE_LEVELS)
                .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();
        Game game = Game.create(builder.build(), setup);

        for (int index = 0; index < game.getPlayerCount(); index++) {
            game.getContent().getProgress().setUnitAvailable(game.getPlayer(index), this.soldier, true);
        }

        return game;
    }

    private @NotNull UnitDefinition.Builder createBuilding() {
        return new UnitDefinition.Builder().setFaction(this.humans).setHitPoints(100).addTrait(UnitTrait.BUILDING);
    }

    private @NotNull UnitDefinition.Builder createFighter() {
        return new UnitDefinition.Builder().setFaction(this.humans)
                                           .setMotionProfile(this.walker)
                                           .setHitPoints(20)
                                           .setGroundWeapon(this.rifle)
                                           .setTargetAcquisitionRange(96)
                                           .setCost(ResourceCost.of(this.ore, SOLDIER_COST))
                                           .setBuildTime(SOLDIER_TIME)
                                           .setSupplyRequired(SOLDIER_SUPPLY)
                                           .setIdleOrder(ControllerKind.HUMAN, this.guard);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        StandardCombatSystem combat = (StandardCombatSystem) game.getCombat();

        behaviors.register(this.guard, new GuardBehavior(combat, this.attack));
        behaviors.register(this.attack, new AttackBehavior(combat));
        behaviors.register(this.move, new InstructionLimitRecorder(new MoveBehavior(0), this.moveStartLimits));
    }

    private void recordSlotMoved(@NotNull Unit producer, int slot, int sourceSlot) {
        this.slotEvents.add("moved " + sourceSlot + " to " + slot);
    }

    private static void updateOrders(@NotNull Game game) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            game.getOrders().update(units.get(index), INSTRUCTION_LIMIT);
        }
    }
}
