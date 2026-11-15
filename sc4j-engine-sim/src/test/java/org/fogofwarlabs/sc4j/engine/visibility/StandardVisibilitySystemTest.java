package org.fogofwarlabs.sc4j.engine.visibility;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default visibility system in an engine-only game: units reveal their sight to their owner, explored tiles stay explored, and a
 * cloaked unit is hidden from the other players.
 */
final class StandardVisibilitySystemTest {
    private static final int MAP_SIZE = 32;
    private static final int TILE_SIZE = 32;
    private static final int MAXIMUM_SIGHT_RANGE = 12;
    private static final int SCOUT_SIGHT_RANGE = 4;
    private static final int UNIT_LIMIT = 4;

    private static final @NotNull UnitType SCOUT = new UnitType("scout");

    @Test
    void units_reveal_their_sight_to_their_owner() {
        Game game = createGame();
        Unit unit = createUnit(game, 0, 160, 160);
        VisibilitySystem visibility = game.getVisibility();
        Player owner = game.getPlayer(0);
        Player other = game.getPlayer(1);

        game.getFrameRunner().runFrame();

        Assertions.assertEquals(SCOUT_SIGHT_RANGE * TILE_SIZE, visibility.getSightRange(unit));
        Assertions.assertTrue(visibility.isVisible(owner, 160, 160));
        Assertions.assertTrue(visibility.isVisible(owner, 160 + (SCOUT_SIGHT_RANGE - 1) * TILE_SIZE, 160));
        Assertions.assertFalse(visibility.isVisible(owner, 160 + (SCOUT_SIGHT_RANGE + 1) * TILE_SIZE, 160));
        Assertions.assertFalse(visibility.isVisible(other, 160, 160));
        Assertions.assertTrue(visibility.isUnitVisible(owner, unit));

        unit.setPosition(800, 800);
        game.getFrameRunner().runFrame();

        Assertions.assertFalse(visibility.isVisible(owner, 160, 160));
        Assertions.assertTrue(visibility.isExplored(owner, 160, 160));
        Assertions.assertFalse(visibility.isExplored(other, 160, 160));
        Assertions.assertTrue(visibility.isVisible(owner, 800, 800));
    }

    @Test
    void cloaked_units_are_detected_by_their_owner_only() {
        Game game = createGame();
        Unit unit = createUnit(game, 0, 160, 160);
        Unit observer = createUnit(game, 1, 192, 160);
        VisibilitySystem visibility = game.getVisibility();

        game.getFrameRunner().runFrame();

        Assertions.assertTrue(visibility.isUnitVisible(game.getPlayer(1), unit));

        visibility.setCloaked(unit, true);

        Assertions.assertFalse(visibility.isUnitVisible(game.getPlayer(1), unit));
        Assertions.assertTrue(visibility.isUnitVisible(game.getPlayer(0), unit));
        Assertions.assertTrue(visibility.isUnitVisible(game.getPlayer(0), observer));
    }

    private static @NotNull Unit createUnit(@NotNull Game game, int playerIndex, int x, int y) {
        Unit unit = game.getUnits().allocate();

        Assertions.assertNotNull(unit);

        unit.setOwner(game.getPlayer(playerIndex));
        unit.setType(SCOUT);
        unit.setPosition(x, y);

        return unit;
    }

    private static @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build())
                .useSystem(VisibilitySystem.KEY, context -> new StandardVisibilitySystem(MAP_SIZE, MAP_SIZE, TILE_SIZE, MAXIMUM_SIGHT_RANGE))
                .addFramePhase(VisibilitySystem.KEY, visibility -> (StandardVisibilitySystem) visibility);

        builder.getContent().getUnitTypes().register(SCOUT, new UnitDefinition.Builder().setSightRange(SCOUT_SIGHT_RANGE).build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
