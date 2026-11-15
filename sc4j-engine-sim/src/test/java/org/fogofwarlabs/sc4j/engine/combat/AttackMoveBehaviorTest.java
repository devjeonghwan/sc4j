package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.order.*;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default attack-move, patrol and hold position behaviors in an engine-only game: an attack-moving soldier kills the enemy on
 * its way and then arrives, a patrolling soldier turns back at both ends, and a holding soldier fires only at enemies in range and never moves.
 */
final class AttackMoveBehaviorTest {
    private static final int UNIT_LIMIT = 8;
    private static final int ORDER_LIMIT = 8;
    private static final int ARRIVAL_DISTANCE = 8;
    private static final int WEAPON_RANGE = 32;
    private static final int ACQUISITION_RANGE = 96;
    private static final int FRAME_LIMIT = 1000;

    private static void updateOrders(@NotNull Game game) {
        SlotList used = game.getUnits().getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            game.getOrders().update(game.getUnits().get(index), 0);
        }
    }


    private final @NotNull MotionProfile walker;
    private final @NotNull WeaponType rifle;
    private final @NotNull OrderType idle;
    private final @NotNull OrderType attackMove;
    private final @NotNull OrderType patrol;
    private final @NotNull OrderType holdPosition;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType peasant;

    AttackMoveBehaviorTest() {
        this.walker       = new MotionProfile("walker");
        this.rifle        = new WeaponType("rifle");
        this.idle         = new OrderType("idle");
        this.attackMove   = new OrderType("attack move");
        this.patrol       = new OrderType("patrol");
        this.holdPosition = new OrderType("hold position");
        this.soldier      = new UnitType("soldier");
        this.peasant      = new UnitType("peasant");
    }

    @Test
    void attack_moving_soldiers_kill_the_enemy_on_their_way_and_arrive() {
        Game game = this.createGame();
        Unit unit = this.createUnit(game, 0, this.soldier, 20, 100);
        Unit enemy = this.createUnit(game, 1, this.peasant, 200, 150);

        game.getOrders().issue(unit, this.attackMove, 400, 100, null, 0);

        for (int frame = 0; frame < FRAME_LIMIT && unit.getOrderType() != this.idle; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertFalse(game.getUnits().isAllocated(enemy.getIndex()));
        Assertions.assertSame(this.idle, unit.getOrderType());
        Assertions.assertTrue(Math.abs(unit.getPositionX() - 400) <= ARRIVAL_DISTANCE);
        Assertions.assertTrue(Math.abs(unit.getPositionY() - 100) <= ARRIVAL_DISTANCE);
    }

    @Test
    void patrolling_soldiers_turn_back_at_both_ends() {
        Game game = this.createGame();
        Unit unit = this.createUnit(game, 0, this.soldier, 20, 100);
        boolean reachedFarEnd = false;
        boolean returned = false;

        game.getOrders().issue(unit, this.patrol, 200, 100, null, 0);

        for (int frame = 0; frame < FRAME_LIMIT && !returned; frame++) {
            game.getFrameRunner().runFrame();

            reachedFarEnd |= unit.getPositionX() >= 200 - ARRIVAL_DISTANCE;
            returned = reachedFarEnd && unit.getPositionX() <= 20 + ARRIVAL_DISTANCE;
        }

        Assertions.assertTrue(returned);
        Assertions.assertSame(this.patrol, unit.getOrderType());
    }

    @Test
    void holding_soldiers_fire_only_in_range_and_never_move() {
        Game game = this.createGame();
        Unit unit = this.createUnit(game, 0, this.soldier, 100, 100);
        Unit far = this.createUnit(game, 1, this.peasant, 100 + ACQUISITION_RANGE - 10, 100);

        game.getOrders().issue(unit, this.holdPosition, 0, 0, null, 0);
        this.runFrames(game, 100);

        Assertions.assertEquals(100, unit.getPositionX());
        Assertions.assertTrue(game.getUnits().isAllocated(far.getIndex()));

        Unit near = this.createUnit(game, 1, this.peasant, 100, 100 + WEAPON_RANGE - 4);

        this.runFrames(game, 200);

        Assertions.assertFalse(game.getUnits().isAllocated(near.getIndex()));
        Assertions.assertTrue(game.getUnits().isAllocated(far.getIndex()));
        Assertions.assertEquals(100, unit.getPositionX());
        Assertions.assertEquals(100, unit.getPositionY());
        Assertions.assertSame(this.holdPosition, unit.getOrderType());
    }

    private void runFrames(@NotNull Game game, int count) {
        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }
    }

    private @NotNull Unit createUnit(@NotNull Game game, int player, @NotNull UnitType type, int x, int y) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(player), type, x, y, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(CombatSystem.KEY, context -> new StandardCombatSystem())
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .addFramePhase(AttackMoveBehaviorTest::updateOrders)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        StandardCombatSystem.addColumns(builder);
        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        PatrolBehavior.addColumns(builder);
        builder.getContent().getOrderTypes().register(this.idle, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.attackMove, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.patrol, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.holdPosition, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512)
                                                                                                            .setAcceleration(64)
                                                                                                            .setStoppingDistance(2048)
                                                                                                            .setTurnRate(40)
                                                                                                            .build());
        builder.getContent().getWeaponTypes().register(this.rifle, new WeaponDefinition.Builder().setDamage(6)
                                                                                                 .setDamageFactor(1)
                                                                                                 .setCooldown(15)
                                                                                                 .setMaximumRange(WEAPON_RANGE)
                                                                                                 .addTarget(WeaponTarget.GROUND)
                                                                                                 .build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                               .setHitPoints(40)
                                                                                               .setGroundWeapon(this.rifle)
                                                                                               .setTargetAcquisitionRange(ACQUISITION_RANGE)
                                                                                               .setIdleOrder(ControllerKind.HUMAN, this.idle)
                                                                                               .build());
        builder.getContent().getUnitTypes().register(this.peasant, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                               .setHitPoints(10)
                                                                                               .setIdleOrder(ControllerKind.HUMAN, this.idle)
                                                                                               .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        StandardCombatSystem combat = (StandardCombatSystem) game.getCombat();

        behaviors.register(this.attackMove, new AttackMoveBehavior(combat, ARRIVAL_DISTANCE));
        behaviors.register(this.patrol, new PatrolBehavior(game, combat, ARRIVAL_DISTANCE));
        behaviors.register(this.holdPosition, new HoldPositionBehavior(combat));
    }
}
