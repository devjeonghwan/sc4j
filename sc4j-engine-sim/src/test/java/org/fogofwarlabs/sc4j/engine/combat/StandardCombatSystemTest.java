package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.engine.content.combat.DamageHit;
import org.fogofwarlabs.sc4j.engine.content.combat.DamageKind;
import org.fogofwarlabs.sc4j.engine.content.combat.DamageLayer;
import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.table.PlayerProgressTable;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.economy.StandardEconomySystem;
import org.fogofwarlabs.sc4j.engine.entity.EntityKind;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnReset;
import org.fogofwarlabs.sc4j.engine.entity.column.IntColumn;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.lifecycle.UnitLifecycle;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.order.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default combat system in an engine-only game: weapon hits through a damage layer and armor with the declared minimum damage,
 * the damage scale hooks, death, enemies by team, weapons by target kind, and the attack order that pursues, fires on cooldown and ends after
 * the kill.
 */
final class StandardCombatSystemTest {
    private static final int UNIT_LIMIT = 8;
    private static final int ORDER_LIMIT = 8;
    private static final int FRAME_LIMIT = 400;
    private static final int INSTRUCTION_LIMIT = 0;
    private static final int POINT = 1 << StandardCombatSystem.FRACTION_BITS;
    private static final int HALF_POINT = POINT / 2;

    private static final int TOP_SPEED = 512;
    private static final int ACCELERATION = 64;
    private static final int STOPPING_DISTANCE = 2048;
    private static final int TURN_RATE = 40;

    private static final int SOLDIER_HIT_POINTS = 20;
    private static final int SOLDIER_BARRIER = 5;
    private static final int SOLDIER_ARMOR = 2;
    private static final int SOLDIER_SUPPLY = 2;
    private static final int RIFLE_DAMAGE = 10;
    private static final int RIFLE_COOLDOWN = 15;
    private static final int RIFLE_RANGE = 32;

    /**
     * The unit column holding the barrier points of a unit.
     */
    private static final @NotNull ColumnKey<IntColumn> BARRIER_POINTS = ColumnKey.ofInt("test.barrier-points", 0, ColumnReset.ON_ALLOCATE);
    /**
     * The key of the system that registers the barrier layer of the soldiers.
     */
    private static final @NotNull SystemKey<BarrierLayer> BARRIER = SystemKey.of("test.barrier", BarrierLayer.class);

    /**
     * Takes a hit before the hit points while it holds a whole point, down to at least half a point after its armor of {@code 0}: the layer the
     * test registers for its soldiers.
     */
    private static final class BarrierLayer implements DamageLayer, GameSystem {
        private final @NotNull UnitType type;

        private @Nullable IntColumn points;

        private BarrierLayer(@NotNull UnitType type) {
            this.type = type;

            this.points = null;
        }

        @Override
        public void attach(@NotNull Game game) {
            this.points = game.getUnits().getColumn(BARRIER_POINTS);

            game.getContent().addDamageLayer(this.type, this);
        }

        @Override
        public @NotNull String getName() {
            return "Barrier";
        }

        @Override
        public int getPoints(@NotNull Unit unit) {
            return this.requirePoints().get(unit.getIndex());
        }

        @Override
        public int getMaximumPoints(@NotNull Unit unit) {
            return unit.getType() == this.type ? SOLDIER_BARRIER * POINT : 0;
        }

        @Override
        public void setPoints(@NotNull Unit unit, int points) {
            this.requirePoints().set(unit.getIndex(), points);
        }

        @Override
        public int absorbDamage(@NotNull DamageHit hit) {
            int points = this.getPoints(hit.getTargetUnit());

            if (points < POINT) {
                return 0;
            }

            int damage = hit.getDamage() > 0 ? hit.getDamage() : HALF_POINT;
            int absorbed = Math.min(damage, points);

            hit.setDamage(damage - absorbed);

            return absorbed;
        }

        private @NotNull IntColumn requirePoints() {
            IntColumn column = this.points;

            Assertions.assertNotNull(column);

            return column;
        }
    }

    private static void updateOrders(@NotNull Game game) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            game.getOrders().update(units.get(index), INSTRUCTION_LIMIT);
        }
    }


    private final @NotNull Faction humans;
    private final @NotNull DamageKind explosive;
    private final @NotNull OrderType attack;
    private final @NotNull OrderType guard;
    private final @NotNull MotionProfile walker;
    private final @NotNull WeaponType rifle;
    private final @NotNull WeaponType needle;
    private final @NotNull WeaponType rocket;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType flyer;
    private final @NotNull UnitType launcher;
    private final @NotNull List<@NotNull Unit> removedUnits;

    StandardCombatSystemTest() {
        this.humans       = new Faction("humans");
        this.explosive    = new DamageKind("explosive");
        this.attack       = new OrderType("attack");
        this.guard        = new OrderType("guard");
        this.walker       = new MotionProfile("walker");
        this.rifle        = new WeaponType("rifle");
        this.needle       = new WeaponType("needle");
        this.rocket       = new WeaponType("rocket");
        this.soldier      = new UnitType("soldier");
        this.flyer        = new UnitType("flyer");
        this.launcher     = new UnitType("launcher");
        this.removedUnits = new ArrayList<>();
    }

    @Test
    void weapon_hits_take_damage_layers_then_armor_and_deal_the_minimum_damage() {
        Game game = this.createGame();
        StandardCombatSystem combat = (StandardCombatSystem) game.getCombat();
        BarrierLayer barrier = game.getModSystem(BARRIER);
        Unit attacker = this.createUnit(game, this.soldier, 0, 10, 10);
        Unit target = this.createUnit(game, this.soldier, 1, 20, 10);

        Assertions.assertEquals(List.of(barrier), game.getContent().getDamageLayers(this.soldier));
        Assertions.assertEquals(SOLDIER_BARRIER * POINT, barrier.getPoints(target));

        combat.dealWeaponDamage(this.rifle, attacker, target, INSTRUCTION_LIMIT);

        Assertions.assertEquals(0, barrier.getPoints(target));
        Assertions.assertEquals(SOLDIER_HIT_POINTS * POINT - ((RIFLE_DAMAGE - SOLDIER_BARRIER - SOLDIER_ARMOR) * POINT), target.getHitPoints());

        int hitPoints = target.getHitPoints();

        combat.dealWeaponDamage(this.rifle, attacker, target, INSTRUCTION_LIMIT);

        Assertions.assertEquals(hitPoints - (RIFLE_DAMAGE - SOLDIER_ARMOR) * POINT, target.getHitPoints());

        hitPoints = target.getHitPoints();

        combat.dealWeaponDamage(this.needle, attacker, target, INSTRUCTION_LIMIT);

        Assertions.assertEquals(hitPoints - HALF_POINT, target.getHitPoints());
    }

    @Test
    void damage_scale_hooks_change_the_damage_of_their_kind() {
        Game game = this.createGame();
        StandardCombatSystem combat = (StandardCombatSystem) game.getCombat();
        Unit attacker = this.createUnit(game, this.launcher, 0, 10, 10);
        Unit target = this.createUnit(game, this.soldier, 1, 20, 10);

        game.getModSystem(BARRIER).setPoints(target, 0);
        combat.dealWeaponDamage(this.rocket, attacker, target, INSTRUCTION_LIMIT);

        Assertions.assertEquals(SOLDIER_HIT_POINTS * POINT - (RIFLE_DAMAGE - SOLDIER_ARMOR) * POINT / 2, target.getHitPoints());
    }

    @Test
    void lethal_damage_kills_the_unit_and_frees_its_slot() {
        Game game = this.createGame();
        Unit target = this.createUnit(game, this.soldier, 1, 20, 10);
        PlayerProgressTable progress = game.getContent().getProgress();
        EconomySystem economy = game.getEconomy();

        game.getCombat().dealDamage(target, target.getHitPoints() - 1, null, INSTRUCTION_LIMIT);

        Assertions.assertEquals(1, target.getHitPoints());
        Assertions.assertTrue(game.getUnits().isAllocated(target.getIndex()));

        game.getCombat().dealDamage(target, 1, null, INSTRUCTION_LIMIT);

        Assertions.assertEquals(0, target.getHitPoints());
        Assertions.assertFalse(game.getUnits().isAllocated(target.getIndex()));
        Assertions.assertNull(target.getOwner());
        Assertions.assertEquals(List.of(target), this.removedUnits);
        Assertions.assertEquals(0, progress.getUnitCount(game.getPlayer(1), this.soldier));
        Assertions.assertEquals(0, economy.getSupplyUsed(game.getPlayer(1), this.humans));
    }

    @Test
    void enemies_follow_the_teams_and_weapons_the_target_kind() {
        Game game = this.createGame();
        CombatSystem combat = game.getCombat();
        Unit soldier = this.createUnit(game, this.soldier, 0, 10, 10);
        Unit ally = this.createUnit(game, this.soldier, 2, 30, 10);
        Unit enemy = this.createUnit(game, this.soldier, 1, 20, 10);
        Unit enemyFlyer = this.createUnit(game, this.flyer, 1, 20, 20);
        Unit launcher = this.createUnit(game, this.launcher, 0, 10, 20);

        Assertions.assertFalse(combat.isEnemy(soldier, ally));
        Assertions.assertTrue(combat.isEnemy(soldier, enemy));
        Assertions.assertFalse(combat.canAttack(soldier, ally));
        Assertions.assertFalse(combat.canAttack(soldier, null));
        Assertions.assertTrue(combat.canAttack(soldier, enemy));
        Assertions.assertFalse(combat.canAttack(soldier, enemyFlyer));
        Assertions.assertTrue(combat.canAttack(launcher, enemyFlyer));
        Assertions.assertSame(this.rocket, combat.getWeapon(launcher, WeaponSlot.AIR));
        Assertions.assertTrue(combat.isInRange(soldier, enemy));
        Assertions.assertFalse(combat.isInRange(soldier, enemyFlyer));
        Assertions.assertFalse(combat.canAttack(launcher, enemy));
    }

    @Test
    void attack_orders_pursue_fire_on_cooldown_and_end_after_the_kill() {
        Game game = this.createGame();
        StandardCombatSystem combat = (StandardCombatSystem) game.getCombat();
        Unit attacker = this.createUnit(game, this.soldier, 0, 10, 40);
        Unit target = this.createUnit(game, this.soldier, 1, 200, 40);
        int shots = 0;
        BarrierLayer barrier = game.getModSystem(BARRIER);
        int previousHitPoints = target.getHitPoints() + barrier.getPoints(target);

        game.getOrders().issue(attacker, this.attack, 0, 0, target, INSTRUCTION_LIMIT);

        for (int frame = 0; frame < FRAME_LIMIT && game.getUnits().isAllocated(target.getIndex()); frame++) {
            game.getFrameRunner().runFrame();

            int hitPoints = target.getHitPoints() + barrier.getPoints(target);

            if (hitPoints != previousHitPoints) {
                shots++;

                Assertions.assertEquals(RIFLE_COOLDOWN, combat.getRemainingCooldown(attacker, WeaponSlot.GROUND));
            }

            previousHitPoints = hitPoints;
        }

        Assertions.assertFalse(game.getUnits().isAllocated(target.getIndex()));
        Assertions.assertEquals(4, shots);
        Assertions.assertTrue(attacker.getPositionX() > 10);
        Assertions.assertTrue(200 - attacker.getPositionX() <= RIFLE_RANGE);

        game.getFrameRunner().runFrame();

        Assertions.assertSame(this.guard, attacker.getOrderType());
    }

    private @NotNull Unit createUnit(@NotNull Game game, @NotNull UnitType type, int ownerIndex, int x, int y) {
        Unit unit = game.getUnits().allocate();
        PlayerProgressTable progress = game.getContent().getProgress();

        Assertions.assertNotNull(unit);

        unit.setOwner(game.getPlayer(ownerIndex));
        unit.setType(type);
        unit.setMotionProfile(this.walker);
        unit.setPosition(x, y);
        unit.setMovementDestinationX(x);
        unit.setMovementDestinationY(y);
        unit.setMovementWaypointX(x);
        unit.setMovementWaypointY(y);
        unit.setDirectionTargetX(x);
        unit.setDirectionTargetY(y);
        unit.setHitPoints(game.getContent().getUnitTypes().getHitPoints(type) * POINT);

        for (DamageLayer layer : game.getContent().getDamageLayers(type)) {
            layer.setPoints(unit, layer.getMaximumPoints(unit));
        }

        progress.setUnitCount(unit.getOwner(), type, progress.getUnitCount(unit.getOwner(), type) + 1);
        progress.setCompletedUnitCount(unit.getOwner(), type, progress.getCompletedUnitCount(unit.getOwner(), type) + 1);
        game.getEconomy().addSupplyUsed(game.getPlayer(ownerIndex), this.humans, game.getContent().getUnitTypes().getSupplyRequired(type));
        game.getMovement().recalculateSpeed(unit, INSTRUCTION_LIMIT);
        game.getOrders().finishOrder(unit, INSTRUCTION_LIMIT);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(EconomySystem.KEY, context -> new StandardEconomySystem())
                .useSystem(CombatSystem.KEY, context -> new StandardCombatSystem.Builder().setMinimumDamage(HALF_POINT).build())
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .useSystem(BARRIER, context -> new BarrierLayer(this.soldier))
                .addHook(StandardCombatSystem.DAMAGE_SCALE, context -> this::halveExplosiveDamage)
                .addHook(UnitLifecycle.UNIT_REMOVED, context -> (unit, death) -> this.removedUnits.add(unit))
                .addFramePhase(StandardCombatSystemTest::updateOrders)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        StandardCombatSystem.addColumns(builder);
        builder.addColumn(EntityKind.UNIT, BARRIER_POINTS);
        builder.getContent().getFactions().register(this.humans, new FactionDefinition.Builder().build());
        builder.getContent().getDamageKinds().register(this.explosive);
        builder.getContent().getOrderTypes().register(this.attack, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.guard, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(TOP_SPEED)
                                                                                                            .setAcceleration(ACCELERATION)
                                                                                                            .setStoppingDistance(STOPPING_DISTANCE)
                                                                                                            .setTurnRate(TURN_RATE)
                                                                                                            .build());
        builder.getContent().getWeaponTypes().register(this.rifle, this.createWeapon(RIFLE_DAMAGE, null, WeaponTarget.GROUND));
        builder.getContent().getWeaponTypes().register(this.needle, this.createWeapon(1, null, WeaponTarget.GROUND));
        builder.getContent().getWeaponTypes().register(this.rocket, this.createWeapon(RIFLE_DAMAGE, this.explosive, WeaponTarget.AIR));
        builder.getContent().getUnitTypes().register(this.soldier, this.createUnitType(this.rifle, null).build());
        builder.getContent().getUnitTypes().register(this.flyer, this.createUnitType(null, null).addTrait(UnitTrait.FLYER).build());
        builder.getContent().getUnitTypes().register(this.launcher, this.createUnitType(this.rocket, this.rocket).build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .addPlayer(new PlayerSlot(2, ControllerKind.HUMAN, 0, 2, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }

    private @NotNull WeaponDefinition createWeapon(int damage, @Nullable DamageKind kind, @NotNull WeaponTarget target) {
        return new WeaponDefinition.Builder().setDamage(damage)
                                             .setDamageFactor(1)
                                             .setDamageKind(kind)
                                             .setCooldown(RIFLE_COOLDOWN)
                                             .setMaximumRange(RIFLE_RANGE)
                                             .addTarget(target)
                                             .build();
    }

    private @NotNull UnitDefinition.Builder createUnitType(@Nullable WeaponType groundWeapon, @Nullable WeaponType airWeapon) {
        return new UnitDefinition.Builder().setFaction(this.humans)
                                           .setHitPoints(SOLDIER_HIT_POINTS)
                                           .setArmor(SOLDIER_ARMOR)
                                           .setSupplyRequired(SOLDIER_SUPPLY)
                                           .setGroundWeapon(groundWeapon)
                                           .setAirWeapon(airWeapon)
                                           .setIdleOrder(ControllerKind.HUMAN, this.guard);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        behaviors.register(this.attack, new AttackBehavior((StandardCombatSystem) game.getCombat()));
    }

    private int halveExplosiveDamage(@NotNull DamageKind kind, @NotNull Unit target, int damage) {
        return kind == this.explosive ? damage / 2 : damage;
    }
}
