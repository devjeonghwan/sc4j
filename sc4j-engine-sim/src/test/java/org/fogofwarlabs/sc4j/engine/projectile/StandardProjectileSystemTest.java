package org.fogofwarlabs.sc4j.engine.projectile;

import org.fogofwarlabs.sc4j.engine.combat.AttackBehavior;
import org.fogofwarlabs.sc4j.engine.combat.CombatSystem;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.entity.Projectile;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.order.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default projectile system in an engine-only game: an attack with a projectile weapon launches a projectile that flies by the
 * motion algorithm and hits on arrival, a projectile whose target died vanishes without a hit, and a projectile vanishes after its flight time.
 */
final class StandardProjectileSystemTest {
    private static final int UNIT_LIMIT = 4;
    private static final int ORDER_LIMIT = 4;
    private static final int PROJECTILE_LIMIT = 4;
    private static final int INSTRUCTION_LIMIT = 0;
    private static final int FRAME_LIMIT = 400;
    private static final int POINT = 1 << StandardCombatSystem.FRACTION_BITS;

    private static final int ARCHER_HIT_POINTS = 30;
    private static final int ARROW_DAMAGE = 10;
    private static final int ARROW_COOLDOWN = 60;
    private static final int ARROW_RANGE = 160;
    private static final int ARROW_SPEED = 1024;

    private final @NotNull OrderType attack;
    private final @NotNull OrderType guard;
    private final @NotNull MotionProfile arrowFlight;
    private final @NotNull WeaponType bow;
    private final @NotNull UnitType archer;

    StandardProjectileSystemTest() {
        this.attack      = new OrderType("attack");
        this.guard       = new OrderType("guard");
        this.arrowFlight = new MotionProfile("arrow");
        this.bow         = new WeaponType("bow");
        this.archer      = new UnitType("archer");
    }

    @Test
    void projectiles_fly_to_their_target_and_hit_on_arrival() {
        Game game = this.createGame(FRAME_LIMIT);
        ProjectileSystem projectiles = game.getModSystem(ProjectileSystem.KEY);
        Unit attacker = this.createUnit(game, 0, 10, 40);
        Unit target = this.createUnit(game, 1, 130, 40);
        int fullHitPoints = target.getHitPoints();

        game.getOrders().issue(attacker, this.attack, 0, 0, target, INSTRUCTION_LIMIT);
        game.getFrameRunner().runFrame();

        Assertions.assertEquals(1, projectiles.getActiveCount());
        Assertions.assertEquals(fullHitPoints, target.getHitPoints());

        Projectile projectile = this.findProjectile(game);
        int frames = 1;

        while (projectiles.getActiveCount() != 0 && frames < FRAME_LIMIT) {
            int previousX = projectile.getPositionX();

            game.getFrameRunner().runFrame();
            frames++;

            Assertions.assertTrue(projectiles.getActiveCount() == 0 || projectile.getPositionX() > previousX);
        }

        Assertions.assertTrue(frames > 2);
        Assertions.assertEquals(target.getPositionX(), projectile.getPositionX());
        Assertions.assertEquals(fullHitPoints - ARROW_DAMAGE * POINT, target.getHitPoints());

        for (int frame = 0; frame < FRAME_LIMIT && game.getUnits().isAllocated(target.getIndex()); frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertFalse(game.getUnits().isAllocated(target.getIndex()));
    }

    @Test
    void projectiles_of_a_dead_target_vanish_without_a_hit() {
        Game game = this.createGame(FRAME_LIMIT);
        ProjectileSystem projectiles = game.getModSystem(ProjectileSystem.KEY);
        Unit attacker = this.createUnit(game, 0, 10, 40);
        Unit target = this.createUnit(game, 1, 130, 40);

        Assertions.assertTrue(projectiles.launch(this.bow, attacker, target, INSTRUCTION_LIMIT));

        game.getCombat().dealDamage(target, target.getHitPoints(), null, INSTRUCTION_LIMIT);

        Unit bystander = this.createUnit(game, 1, 300, 40);

        for (int frame = 0; frame < FRAME_LIMIT && projectiles.getActiveCount() != 0; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(0, projectiles.getActiveCount());
        Assertions.assertEquals(ARCHER_HIT_POINTS * POINT, bystander.getHitPoints());
        Assertions.assertSame(this.guard, attacker.getOrderType());
    }

    @Test
    void projectiles_vanish_after_their_flight_time() {
        int flightTime = 3;
        Game game = this.createGame(flightTime);
        ProjectileSystem projectiles = game.getModSystem(ProjectileSystem.KEY);
        Unit attacker = this.createUnit(game, 0, 10, 40);
        Unit target = this.createUnit(game, 1, 130, 40);

        Assertions.assertTrue(projectiles.launch(this.bow, attacker, target, INSTRUCTION_LIMIT));

        for (int frame = 0; frame < flightTime; frame++) {
            Assertions.assertEquals(1, projectiles.getActiveCount());

            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(0, projectiles.getActiveCount());
        Assertions.assertEquals(ARCHER_HIT_POINTS * POINT, target.getHitPoints());
    }

    private @NotNull Projectile findProjectile(@NotNull Game game) {
        EntityPool<Projectile> pool = game.getProjectiles();

        return pool.get(pool.getUsedSlots().getFirst());
    }

    private @NotNull Unit createUnit(@NotNull Game game, int ownerIndex, int x, int y) {
        Unit unit = game.getUnits().allocate();

        Assertions.assertNotNull(unit);

        unit.setOwner(game.getPlayer(ownerIndex));
        unit.setType(this.archer);
        unit.setPosition(x, y);
        unit.setMovementDestinationX(x);
        unit.setMovementDestinationY(y);
        unit.setMovementWaypointX(x);
        unit.setMovementWaypointY(y);
        unit.setDirectionTargetX(x);
        unit.setDirectionTargetY(y);
        unit.setHitPoints(ARCHER_HIT_POINTS * POINT);
        game.getOrders().finishOrder(unit, INSTRUCTION_LIMIT);

        return unit;
    }

    private @NotNull Game createGame(int maximumFlightTime) {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).setProjectiles(PROJECTILE_LIMIT).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(CombatSystem.KEY, context -> new StandardCombatSystem())
                .useSystem(ProjectileSystem.KEY, context -> new StandardProjectileSystem(maximumFlightTime, SkirmishScenario.MOTION_MODEL))
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .addFramePhase(StandardProjectileSystemTest::updateOrders)
                .addFramePhase(ProjectileSystem.KEY, projectiles -> (StandardProjectileSystem) projectiles)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        StandardCombatSystem.addColumns(builder);
        builder.getContent().getOrderTypes().register(this.attack, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.guard, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.arrowFlight, new MotionProfileDefinition.Builder().setTopSpeed(ARROW_SPEED)
                                                                                                                 .setAcceleration(ARROW_SPEED)
                                                                                                                 .setTurnRate(127)
                                                                                                                 .build());
        builder.getContent().getWeaponTypes().register(this.bow, new WeaponDefinition.Builder().setDamage(ARROW_DAMAGE)
                                                                                               .setDamageFactor(1)
                                                                                               .setCooldown(ARROW_COOLDOWN)
                                                                                               .setMaximumRange(ARROW_RANGE)
                                                                                               .setProjectile(this.arrowFlight)
                                                                                               .addTarget(WeaponTarget.GROUND)
                                                                                               .build());
        builder.getContent().getUnitTypes().register(this.archer, new UnitDefinition.Builder().setHitPoints(ARCHER_HIT_POINTS)
                                                                                              .setGroundWeapon(this.bow)
                                                                                              .setIdleOrder(ControllerKind.HUMAN, this.guard)
                                                                                              .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        behaviors.register(this.attack, new AttackBehavior((StandardCombatSystem) game.getCombat()));
    }

    private static void updateOrders(@NotNull Game game) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            game.getOrders().update(units.get(index), INSTRUCTION_LIMIT);
        }
    }
}
