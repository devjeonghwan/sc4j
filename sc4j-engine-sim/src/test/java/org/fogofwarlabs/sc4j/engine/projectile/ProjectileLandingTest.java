package org.fogofwarlabs.sc4j.engine.projectile;

import org.fogofwarlabs.sc4j.engine.combat.CombatSystem;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponTarget;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the projectiles that do not home of the engine default projectile system: such a projectile lands where its target stood at the launch,
 * hits a target still standing there, misses a target that moved away and splashes the units around the landing point.
 */
final class ProjectileLandingTest {
    private static final int HIT_POINTS = 50;
    private static final int DAMAGE = 10;
    private static final int FRAME_LIMIT = 200;
    private static final int POINT = 1 << StandardCombatSystem.FRACTION_BITS;

    private final @NotNull MotionProfile stone;
    private final @NotNull WeaponType sling;
    private final @NotNull WeaponType mortar;
    private final @NotNull UnitType slinger;
    private final @NotNull UnitType target;

    ProjectileLandingTest() {
        this.stone   = new MotionProfile("stone");
        this.sling   = new WeaponType("sling");
        this.mortar  = new WeaponType("mortar");
        this.slinger = new UnitType("slinger");
        this.target  = new UnitType("target");
    }

    @Test
    void projectiles_that_do_not_home_land_where_the_target_stood() {
        Game game = this.createGame();
        ProjectileSystem projectiles = game.getModSystem(ProjectileSystem.KEY);
        Unit attacker = this.createUnit(game, 0, 10, 40);
        Unit standing = this.createUnit(game, 1, 130, 40);
        Unit leaving = this.createUnit(game, 1, 130, 140);
        Unit neighbor = this.createUnit(game, 1, 140, 150);

        Assertions.assertTrue(projectiles.launch(this.sling, attacker, standing, 0));
        Assertions.assertTrue(projectiles.launch(this.sling, attacker, leaving, 0));
        Assertions.assertTrue(projectiles.launch(this.mortar, attacker, neighbor, 0));

        leaving.setPosition(130, 240);
        neighbor.setPosition(130, 140);
        this.runUntilLanded(game, projectiles);

        Assertions.assertEquals((HIT_POINTS - DAMAGE) * POINT, standing.getHitPoints());
        Assertions.assertEquals(HIT_POINTS * POINT, leaving.getHitPoints());
        Assertions.assertEquals((HIT_POINTS - DAMAGE) * POINT, neighbor.getHitPoints());
    }

    private void runUntilLanded(@NotNull Game game, @NotNull ProjectileSystem projectiles) {
        for (int frame = 0; frame < FRAME_LIMIT && projectiles.getActiveCount() != 0; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(0, projectiles.getActiveCount());
    }

    private @NotNull Unit createUnit(@NotNull Game game, int player, int x, int y) {
        UnitType type = player == 0 ? this.slinger : this.target;
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(player), type, x, y, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(8).setProjectiles(8).build())
                .useSystem(CombatSystem.KEY, context -> new StandardCombatSystem())
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(ProjectileSystem.KEY, context -> new StandardProjectileSystem(FRAME_LIMIT, SkirmishScenario.MOTION_MODEL))
                .addFramePhase(ProjectileSystem.KEY, projectiles -> (StandardProjectileSystem) projectiles);

        StandardCombatSystem.addColumns(builder);
        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getMotionProfiles().register(this.stone, new MotionProfileDefinition.Builder().setTopSpeed(1024)
                                                                                                           .setAcceleration(1024)
                                                                                                           .setTurnRate(127)
                                                                                                           .build());
        builder.getContent().getWeaponTypes().register(this.sling, new WeaponDefinition.Builder().setDamage(DAMAGE)
                                                                                                 .setDamageFactor(1)
                                                                                                 .setMaximumRange(400)
                                                                                                 .setProjectile(this.stone)
                                                                                                 .setHoming(false)
                                                                                                 .addTarget(WeaponTarget.GROUND)
                                                                                                 .build());
        builder.getContent().getWeaponTypes().register(this.mortar, new WeaponDefinition.Builder().setDamage(DAMAGE)
                                                                                                  .setDamageFactor(1)
                                                                                                  .setMaximumRange(400)
                                                                                                  .setProjectile(this.stone)
                                                                                                  .setHoming(false)
                                                                                                  .setSplashInnerRadius(20)
                                                                                                  .setSplashMiddleRadius(20)
                                                                                                  .setSplashOuterRadius(20)
                                                                                                  .addTarget(WeaponTarget.GROUND)
                                                                                                  .build());
        builder.getContent().getUnitTypes().register(this.slinger, new UnitDefinition.Builder().setHitPoints(HIT_POINTS).build());
        builder.getContent().getUnitTypes().register(this.target, new UnitDefinition.Builder().setHitPoints(HIT_POINTS)
                                                                                              .setCollisionLeft(4)
                                                                                              .setCollisionUp(4)
                                                                                              .setCollisionRight(4)
                                                                                              .setCollisionDown(4)
                                                                                              .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
