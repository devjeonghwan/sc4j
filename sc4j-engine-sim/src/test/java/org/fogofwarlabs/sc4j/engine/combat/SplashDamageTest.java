package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponTarget;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.fogofwarlabs.sc4j.engine.visibility.StandardVisibilitySystem;
import org.fogofwarlabs.sc4j.engine.visibility.VisibilitySystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the splash damage, the miss rule and the detection rule of the engine default combat system in an engine-only game: units around a hit take
 * the declared shares of their radius or the whole damage without declared shares, the splash targets setting decides whether allies are hit, a miss
 * hook makes a hit deal nothing, neutral units are nobody's enemy, and an undetected cloaked unit cannot be attacked.
 */
final class SplashDamageTest {
    private static final int HIT_POINTS = 100;
    private static final int DAMAGE = 40;
    private static final int INNER_RADIUS = 10;
    private static final int MIDDLE_RADIUS = 20;
    private static final int OUTER_RADIUS = 30;
    private static final int INNER_SHARE = 100;
    private static final int MIDDLE_SHARE = 50;
    private static final int OUTER_SHARE = 25;

    private final @NotNull WeaponType bomb;
    private final @NotNull UnitType catapult;
    private final @NotNull UnitType dummy;

    private boolean missing;

    SplashDamageTest() {
        this.bomb     = new WeaponType("bomb");
        this.catapult = new UnitType("catapult");
        this.dummy    = new UnitType("dummy");

        this.missing = false;
    }

    @Test
    void units_around_a_hit_take_the_share_of_their_radius() {
        Game game = this.createGame(new StandardCombatSystem.Builder().setSplashShares(INNER_SHARE, MIDDLE_SHARE, OUTER_SHARE));
        Unit attacker = this.createUnit(game, 0, this.catapult, 0, 0);
        Unit target = this.createUnit(game, 1, this.dummy, 200, 200);
        Unit middle = this.createUnit(game, 1, this.dummy, 200 + MIDDLE_RADIUS - 2, 200);
        Unit outer = this.createUnit(game, 1, this.dummy, 200, 200 + OUTER_RADIUS - 2);
        Unit far = this.createUnit(game, 1, this.dummy, 200 + OUTER_RADIUS + 2, 200);
        Unit ally = this.createUnit(game, 0, this.dummy, 205, 200);

        game.getCombat().dealWeaponDamage(this.bomb, attacker, target, 0);

        Assertions.assertEquals(HIT_POINTS - DAMAGE << StandardCombatSystem.FRACTION_BITS, target.getHitPoints());
        Assertions.assertEquals(HIT_POINTS - DAMAGE / 2 << StandardCombatSystem.FRACTION_BITS, middle.getHitPoints());
        Assertions.assertEquals(HIT_POINTS - DAMAGE / 4 << StandardCombatSystem.FRACTION_BITS, outer.getHitPoints());
        Assertions.assertEquals(HIT_POINTS << StandardCombatSystem.FRACTION_BITS, far.getHitPoints());
        Assertions.assertEquals(HIT_POINTS << StandardCombatSystem.FRACTION_BITS, ally.getHitPoints());
    }

    @Test
    void units_around_a_hit_take_the_whole_damage_without_declared_shares() {
        Game game = this.createGame(new StandardCombatSystem.Builder());
        Unit attacker = this.createUnit(game, 0, this.catapult, 0, 0);
        Unit target = this.createUnit(game, 1, this.dummy, 200, 200);
        Unit outer = this.createUnit(game, 1, this.dummy, 200, 200 + OUTER_RADIUS - 2);

        game.getCombat().dealWeaponDamage(this.bomb, attacker, target, 0);

        Assertions.assertEquals(HIT_POINTS - DAMAGE << StandardCombatSystem.FRACTION_BITS, target.getHitPoints());
        Assertions.assertEquals(HIT_POINTS - DAMAGE << StandardCombatSystem.FRACTION_BITS, outer.getHitPoints());
    }

    @Test
    void splash_reaching_all_units_hits_allies_and_misses_deal_nothing() {
        Game game = this.createGame(new StandardCombatSystem.Builder().setSplashTargets(SplashTargets.ALL));
        Unit attacker = this.createUnit(game, 0, this.catapult, 0, 0);
        Unit target = this.createUnit(game, 1, this.dummy, 200, 200);
        Unit ally = this.createUnit(game, 0, this.dummy, 205, 200);

        game.getCombat().dealWeaponDamageAt(this.bomb, attacker, null, 200, 200, 0);

        Assertions.assertEquals(HIT_POINTS - DAMAGE << StandardCombatSystem.FRACTION_BITS, ally.getHitPoints());
        Assertions.assertEquals(HIT_POINTS - DAMAGE << StandardCombatSystem.FRACTION_BITS, target.getHitPoints());

        this.missing = true;
        game.getCombat().dealWeaponDamage(this.bomb, attacker, target, 0);

        Assertions.assertEquals(HIT_POINTS - DAMAGE << StandardCombatSystem.FRACTION_BITS, target.getHitPoints());
    }

    @Test
    void neutral_and_undetected_units_cannot_be_attacked() {
        Game game = this.createGame(new StandardCombatSystem.Builder());
        Unit attacker = this.createUnit(game, 0, this.catapult, 0, 0);
        Unit neutral = this.createUnit(game, 2, this.dummy, 20, 0);
        Unit enemy = this.createUnit(game, 1, this.dummy, 0, 20);

        Assertions.assertFalse(game.getCombat().isEnemy(attacker, neutral));
        Assertions.assertFalse(game.getCombat().canAttack(attacker, neutral));
        Assertions.assertTrue(game.getCombat().canAttack(attacker, enemy));

        game.getVisibility().setCloaked(enemy, true);

        Assertions.assertFalse(game.getCombat().canAttack(attacker, enemy));
    }

    private @NotNull Unit createUnit(@NotNull Game game, int player, @NotNull UnitType type, int x, int y) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(player), type, x, y, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame(StandardCombatSystem.@NotNull Builder combat) {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(8).build())
                .useSystem(CombatSystem.KEY, context -> combat.build())
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(VisibilitySystem.KEY, context -> new StandardVisibilitySystem(8, 8, 32, 4))
                .addHook(StandardCombatSystem.MISS, context -> (weapon, attacker, target) -> this.missing ? MissAnswer.MISS : MissAnswer.NONE);

        StandardCombatSystem.addColumns(builder);
        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getWeaponTypes().register(this.bomb, new WeaponDefinition.Builder().setDamage(DAMAGE)
                                                                                                .setDamageFactor(1)
                                                                                                .setMaximumRange(400)
                                                                                                .setSplashInnerRadius(INNER_RADIUS)
                                                                                                .setSplashMiddleRadius(MIDDLE_RADIUS)
                                                                                                .setSplashOuterRadius(OUTER_RADIUS)
                                                                                                .addTarget(WeaponTarget.GROUND)
                                                                                                .build());
        builder.getContent().getUnitTypes().register(this.catapult, new UnitDefinition.Builder().setHitPoints(HIT_POINTS)
                                                                                                .setGroundWeapon(this.bomb)
                                                                                                .build());
        builder.getContent().getUnitTypes().register(this.dummy, new UnitDefinition.Builder().setHitPoints(HIT_POINTS).build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
