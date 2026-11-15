package org.fogofwarlabs.sc4j.demo.game;

import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.demo.map.DemoMaps;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.path.StandardPathfindingSystem;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the motion of the demo's units on Ashen Ford without computer players: a group sent to one point settles there and then stands still
 * (no unit is pushed to and fro), and a group sent across the map searches its path once on the order and rarely afterwards, so that the cost
 * of a frame of a long move stays near that of a short one.
 */
final class DemoMotionTest {
    private static final int GROUP_SIZE = 24;
    private static final int GROUP_X = 1600;
    private static final int GROUP_Y = 1250;
    private static final int GATHER_X = 1450;
    private static final int GATHER_Y = 1150;
    private static final int SETTLE_FRAMES = 500;
    private static final int STILL_FRAMES = 200;
    private static final int SHORT_TARGET_X = 1700;
    private static final int SHORT_TARGET_Y = 1450;
    private static final int LONG_TARGET_X = 300;
    private static final int LONG_TARGET_Y = 1700;
    private static final int MOVE_FRAMES = 600;
    private static final int MAXIMUM_ORDER_SEARCHES = 4;
    private static final int MAXIMUM_SEARCHES_PER_UNIT = 2;
    private static final int MAXIMUM_COST_RATIO = 4;
    private static final int WARM_UP_RUNS = 2;

    @Test
    void a_settled_group_stands_still() throws IOException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            Game game = createGame(scenario);
            List<Unit> units = createGroup(scenario, game);
            OrderType move = scenario.getContent().getOrders().getMove();

            for (Unit unit : units) {
                game.getOrders().issue(unit, move, GATHER_X, GATHER_Y, null, 0);
            }

            runFrames(game, SETTLE_FRAMES);

            int[] positionX = new int[units.size()];
            int[] positionY = new int[units.size()];

            for (int index = 0; index < units.size(); index++) {
                positionX[index] = units.get(index).getPositionX();
                positionY[index] = units.get(index).getPositionY();
            }

            for (int frame = 0; frame < STILL_FRAMES; frame++) {
                game.getFrameRunner().runFrame();

                for (int index = 0; index < units.size(); index++) {
                    Unit unit = units.get(index);

                    Assertions.assertTrue(game.getMovement().isAtDestination(unit), "unit " + unit.getIndex() + " arrived");
                    Assertions.assertEquals(positionX[index], unit.getPositionX(), "x of unit " + unit.getIndex() + " in frame " + frame);
                    Assertions.assertEquals(positionY[index], unit.getPositionY(), "y of unit " + unit.getIndex() + " in frame " + frame);
                }
            }
        }
    }

    @Test
    void a_long_group_move_searches_its_path_once_and_costs_about_a_short_one() throws IOException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            long shortCost = 0;
            long longCost = 0;

            for (int run = 0; run <= WARM_UP_RUNS; run++) {
                shortCost = measureMove(scenario, SHORT_TARGET_X, SHORT_TARGET_Y);
                longCost  = measureMove(scenario, LONG_TARGET_X, LONG_TARGET_Y);
            }

            Assertions.assertTrue(longCost <= shortCost * MAXIMUM_COST_RATIO,
                                  "a long move costs " + longCost + " ns against " + shortCost + " ns of a short one");
        }
    }

    /**
     * Sends a group to a point, checks its path searches and returns the simulation time of the move's frames in nanoseconds.
     */
    private static long measureMove(@NotNull DemoGameScenario scenario, int targetX, int targetY) {
        Game game = createGame(scenario);
        List<Unit> units = createGroup(scenario, game);
        StandardPathfindingSystem paths = (StandardPathfindingSystem) game.getPathfinding();
        OrderType move = scenario.getContent().getOrders().getMove();

        for (Unit unit : units) {
            game.getOrders().issue(unit, move, targetX, targetY, null, 0);
        }

        int searches = paths.getSearchCount();
        long start = System.nanoTime();

        game.getFrameRunner().runFrame();

        Assertions.assertTrue(paths.getSearchCount() - searches <= MAXIMUM_ORDER_SEARCHES,
                              "searches on the order frame: " + (paths.getSearchCount() - searches));

        runFrames(game, MOVE_FRAMES - 1);

        long time = System.nanoTime() - start;

        Assertions.assertTrue(paths.getSearchCount() - searches <= GROUP_SIZE * MAXIMUM_SEARCHES_PER_UNIT,
                              "searches of the move: " + (paths.getSearchCount() - searches));

        return time;
    }

    private static @NotNull Game createGame(@NotNull DemoGameScenario scenario) {
        DemoContent content = scenario.getContent();
        MapReference map = scenario.getMaps().requireReference(DemoMaps.ASHEN_FORD);
        GameSetup setup = new GameSetup.Builder(map).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, content.getDawnhold()))
                                                    .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, content.getCindermaw()))
                                                    .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                                                    .setSeed(1)
                                                    .build();

        return scenario.getGames().createGame(setup);
    }

    /**
     * Creates a group of Cindermaw cleavers, each at the free position nearest to the same point.
     */
    private static @NotNull List<@NotNull Unit> createGroup(@NotNull DemoGameScenario scenario, @NotNull Game game) {
        Player player = game.getPlayer(1);
        UnitType cleaver = scenario.getContent().requireUnitType("cleaver");
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        List<Unit> units = new ArrayList<>();

        for (int index = 0; index < GROUP_SIZE; index++) {
            Unit unit = production.createUnit(player, cleaver, GROUP_X, GROUP_Y, 0);

            Assertions.assertNotNull(unit);
            units.add(unit);
        }

        return units;
    }

    private static void runFrames(@NotNull Game game, int count) {
        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }
    }
}
