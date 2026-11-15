package org.fogofwarlabs.sc4j.engine.client.view;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.SkirmishScenario;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default game view on the skirmish map: players with their factions and colours, units by object with their read-only state,
 * unit search by point and rectangle, visibility and resources of the local player.
 */
final class StandardGameViewTest {
    private static final int SCOUT_X = 400;
    private static final int SCOUT_Y = 144;
    private static final int MAP_SIZE = 48 * 32;
    private static final int FRAMES = 8;

    @Test
    void view_shows_players_units_visibility_and_resources_of_the_game() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameView view = skirmish.getSession().getView();
        SkirmishScenario scenario = skirmish.getScenario();

        skirmish.runFrames(FRAMES);

        Player local = view.getLocalPlayer();

        Assertions.assertNotNull(local);
        Assertions.assertEquals(SkirmishSessionScenario.LOCAL_SLOT, local.getIndex());
        Assertions.assertEquals(FRAMES, view.getFrame());
        Assertions.assertEquals(MAP_SIZE, view.getMapWidth());
        Assertions.assertEquals(MAP_SIZE, view.getMapHeight());
        Assertions.assertSame(scenario.getWardens(), view.getFaction(local));
        Assertions.assertNull(view.getFaction(view.getPlayers().get(2)));
        Assertions.assertEquals(SkirmishSessionScenario.TEAM_COLORS[SkirmishSessionScenario.LOCAL_SLOT], view.getTeamColor(local));
        Assertions.assertEquals(SkirmishScenario.START_GOLD, view.getResourceAmount(local, scenario.getGold()));

        Unit scout = view.findUnitAt(SCOUT_X, SCOUT_Y);

        Assertions.assertNotNull(scout);

        UnitView scoutView = view.getUnitView(scout);

        Assertions.assertSame(scoutView, view.getUnitView(scout), "one view per unit");
        Assertions.assertSame(local, scoutView.getOwner());
        Assertions.assertTrue(scoutView.isPlaced());
        Assertions.assertTrue(scoutView.getLeft() <= SCOUT_X && scoutView.getRight() >= SCOUT_X);
        Assertions.assertTrue(scoutView.getHitPoints() > 0);
        Assertions.assertEquals(scoutView.getHitPoints(), scoutView.getMaximumHitPoints());
        Assertions.assertTrue(view.getUnits().contains(scout));
        Assertions.assertTrue(view.isVisible(local, SCOUT_X, SCOUT_Y));
        Assertions.assertTrue(view.isUnitVisible(local, scout));
        Assertions.assertFalse(view.isExplored(local, MAP_SIZE - 1, MAP_SIZE - 1));

        List<Unit> all = new ArrayList<>();
        List<Unit> near = new ArrayList<>();

        view.findUnits(0, 0, MAP_SIZE - 1, MAP_SIZE - 1, all);
        view.findUnits(SCOUT_X, SCOUT_Y, SCOUT_X, SCOUT_Y, near);

        Assertions.assertEquals(view.getUnits(), all, "every unit is placed on the map");
        Assertions.assertEquals(List.of(scout), near);
        Assertions.assertNull(view.findUnitAt(MAP_SIZE / 2, MAP_SIZE / 2));
    }
}
