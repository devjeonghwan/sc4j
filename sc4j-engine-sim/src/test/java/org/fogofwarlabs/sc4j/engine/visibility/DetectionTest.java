package org.fogofwarlabs.sc4j.engine.visibility;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the detection rules of the engine default visibility system: a cloaked unit is detected by its owner and the owner's allies, and by the
 * players of a detector whose sight range reaches it, until it leaves that range.
 */
final class DetectionTest {
    private static final int TILE_SIZE = 32;
    private static final int SIGHT_RANGE = 3;

    private final @NotNull UnitType watchtower;
    private final @NotNull UnitType rogue;

    DetectionTest() {
        this.watchtower = new UnitType("watchtower");
        this.rogue      = new UnitType("rogue");
    }

    @Test
    void detectors_reveal_cloaked_units_in_their_sight_range_to_their_team() {
        Game game = this.createGame();
        VisibilitySystem visibility = game.getVisibility();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit tower = production.createUnit(game.getPlayer(0), this.watchtower, 40, 40, 0);
        Unit hidden = production.createUnit(game.getPlayer(1), this.rogue, 100, 40, 0);

        Assertions.assertNotNull(tower);
        Assertions.assertNotNull(hidden);

        visibility.setCloaked(hidden, true);

        Assertions.assertFalse(visibility.isDetected(game.getPlayer(0), hidden));
        Assertions.assertTrue(visibility.isDetected(game.getPlayer(1), hidden));
        Assertions.assertTrue(visibility.isDetected(game.getPlayer(2), hidden));

        game.getFrameRunner().runFrame();

        Assertions.assertTrue(visibility.isDetected(game.getPlayer(0), hidden));
        Assertions.assertTrue(visibility.isDetected(game.getPlayer(3), hidden));
        Assertions.assertFalse(visibility.isDetected(game.getPlayer(4), hidden));

        hidden.setPosition(40 + SIGHT_RANGE * TILE_SIZE + 10, 40);
        game.getFrameRunner().runFrame();

        Assertions.assertFalse(visibility.isDetected(game.getPlayer(0), hidden));
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(4).build())
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(VisibilitySystem.KEY, context -> new StandardVisibilitySystem(8, 8, TILE_SIZE, 4))
                .addFramePhase(VisibilitySystem.KEY, visibility -> (StandardVisibilitySystem) visibility);

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getUnitTypes().register(this.watchtower, new UnitDefinition.Builder().setHitPoints(100)
                                                                                                  .setSightRange(SIGHT_RANGE)
                                                                                                  .addTrait(UnitTrait.DETECTOR)
                                                                                                  .build());
        builder.getContent().getUnitTypes().register(this.rogue, new UnitDefinition.Builder().setHitPoints(10).setSightRange(2).build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .addPlayer(new PlayerSlot(2, ControllerKind.HUMAN, 1, 2, null))
                                                                                       .addPlayer(new PlayerSlot(3, ControllerKind.HUMAN, 0, 3, null))
                                                                                       .addPlayer(new PlayerSlot(4, ControllerKind.HUMAN, 2, 4, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
