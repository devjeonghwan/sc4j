package org.fogofwarlabs.sc4j.engine.path;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.fogofwarlabs.sc4j.engine.terrain.ArrayTerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default path finding over the walk cells of a terrain: a path around a wall in a few straight legs that keep the unit's size
 * off the wall, structures that block their cells while they stand, a destination on a structure, on unwalkable terrain and in an enclosed area,
 * and the same paths in two games.
 */
final class StandardPathfindingSystemTest {
    private static final int TILE_SIZE = 32;
    private static final int WALK_CELL_SIZE = 8;
    private static final int MAP_TILES = 16;
    private static final int UNIT_LIMIT = 4;
    private static final int UNIT_EXTENT = 6;
    private static final int TOWER_EXTENT = 23;
    private static final int WALL_COLUMN = 8;
    private static final int WALL_END_ROW = 12;
    private static final int LEG_LIMIT = StandardPathfindingSystem.WAYPOINT_CAPACITY + 1;
    private static final int SAMPLE_DISTANCE = 2;

    private final @NotNull UnitType soldier;
    private final @NotNull UnitType tower;
    private final @NotNull TableUnitBounds bounds;

    StandardPathfindingSystemTest() {
        this.soldier = new UnitType("soldier");
        this.tower   = new UnitType("tower");
        this.bounds  = new TableUnitBounds();
    }

    @Test
    void units_walk_around_a_wall_in_few_straight_legs_off_the_wall() {
        ArrayTerrainGrid terrain = createWalledTerrain(WALL_END_ROW);
        Game game = this.createGame(terrain);
        Unit unit = this.createUnit(game, this.soldier, 2 * TILE_SIZE, 2 * TILE_SIZE);
        int targetX = (WALL_COLUMN + 4) * TILE_SIZE;
        int targetY = 2 * TILE_SIZE;
        List<int[]> waypoints = this.walkPath(game, unit, targetX, targetY);

        Assertions.assertTrue(waypoints.size() <= 4, "a few legs: " + waypoints.size());
        Assertions.assertArrayEquals(new int[]{targetX, targetY}, waypoints.getLast());

        int previousX = 2 * TILE_SIZE;
        int previousY = 2 * TILE_SIZE;
        int lowestY = previousY;

        for (int[] waypoint : waypoints) {
            this.assertLegClear(terrain, null, previousX, previousY, waypoint[0], waypoint[1]);

            previousX = waypoint[0];
            previousY = waypoint[1];
            lowestY   = Math.max(lowestY, previousY);
        }

        Assertions.assertTrue(lowestY >= WALL_END_ROW * TILE_SIZE, "the path goes around the end of the wall");
    }

    @Test
    void structures_block_their_cells_while_they_stand() {
        ArrayTerrainGrid terrain = createWalledTerrain(0);
        Game game = this.createGame(terrain);
        Unit unit = this.createUnit(game, this.soldier, 2 * TILE_SIZE, 4 * TILE_SIZE);
        Unit structure = this.createUnit(game, this.tower, 6 * TILE_SIZE, 4 * TILE_SIZE);
        int targetX = 10 * TILE_SIZE;
        int targetY = 4 * TILE_SIZE;
        List<int[]> around = this.walkPath(game, unit, targetX, targetY);

        Assertions.assertTrue(around.size() >= 2, "the path bends around the structure");

        int previousX = 2 * TILE_SIZE;
        int previousY = 4 * TILE_SIZE;

        for (int[] waypoint : around) {
            this.assertLegClear(terrain, structure, previousX, previousY, waypoint[0], waypoint[1]);

            previousX = waypoint[0];
            previousY = waypoint[1];
        }

        List<int[]> onto = this.walkPath(game, this.createUnit(game, this.soldier, 2 * TILE_SIZE, 4 * TILE_SIZE),
                                         structure.getPositionX(), structure.getPositionY());

        Assertions.assertArrayEquals(new int[]{structure.getPositionX(), structure.getPositionY()}, onto.getLast(),
                                     "a destination on a structure is the last waypoint");

        game.getUnits().free(structure);
        game.getClock().setFrame(game.getClock().getFrame() + 1);
        unit.setPosition(2 * TILE_SIZE, 4 * TILE_SIZE);

        List<int[]> straight = this.walkPath(game, unit, targetX, targetY);

        Assertions.assertEquals(1, straight.size(), "without the structure the way is straight");
    }

    @Test
    void units_stop_short_of_unwalkable_terrain_and_miss_enclosed_areas() {
        ArrayTerrainGrid terrain = createWalledTerrain(MAP_TILES);
        Game game = this.createGame(terrain);
        Unit unit = this.createUnit(game, this.soldier, 2 * TILE_SIZE, 2 * TILE_SIZE);
        PathfindingSystem paths = game.getPathfinding();
        MutablePoint waypoint = new MutablePoint();
        int wallX = WALL_COLUMN * TILE_SIZE + WALK_CELL_SIZE / 2;

        Assertions.assertFalse(paths.isReachable(unit, (WALL_COLUMN + 4) * TILE_SIZE, 2 * TILE_SIZE));
        Assertions.assertFalse(paths.updatePath(unit, (WALL_COLUMN + 4) * TILE_SIZE, 2 * TILE_SIZE));
        Assertions.assertFalse(paths.getWaypoint(unit, waypoint));

        Assertions.assertTrue(paths.isReachable(unit, wallX, 2 * TILE_SIZE), "the nearest free cell of the wall is on the unit's side");
        Assertions.assertTrue(paths.updatePath(unit, wallX, 2 * TILE_SIZE));
        Assertions.assertTrue(paths.getWaypoint(unit, waypoint));
        Assertions.assertTrue(waypoint.getX() < WALL_COLUMN * TILE_SIZE, "the path ends before the wall");

        unit.setPosition(waypoint.getX(), waypoint.getY());

        Assertions.assertFalse(paths.updatePath(unit, wallX, 2 * TILE_SIZE), "a unit at the end of a short path has no path any more");

        paths.updatePath(unit, 2 * TILE_SIZE, 3 * TILE_SIZE);
        paths.releasePath(unit);

        Assertions.assertFalse(paths.getWaypoint(unit, waypoint));
    }

    @Test
    void the_same_game_gives_the_same_paths() {
        List<List<int[]>> paths = new ArrayList<>();

        for (int run = 0; run < 2; run++) {
            Game game = this.createGame(createWalledTerrain(WALL_END_ROW));

            this.createUnit(game, this.tower, 4 * TILE_SIZE, 8 * TILE_SIZE);
            paths.add(this.walkPath(game, this.createUnit(game, this.soldier, TILE_SIZE, 10 * TILE_SIZE), 14 * TILE_SIZE, TILE_SIZE));
        }

        Assertions.assertEquals(paths.get(0).size(), paths.get(1).size());

        for (int index = 0; index < paths.get(0).size(); index++) {
            Assertions.assertArrayEquals(paths.get(0).get(index), paths.get(1).get(index));
        }
    }

    /**
     * Follows a unit's path by putting the unit on each waypoint in turn, and returns the waypoints up to the destination.
     */
    private @NotNull List<int[]> walkPath(@NotNull Game game, @NotNull Unit unit, int targetX, int targetY) {
        PathfindingSystem paths = game.getPathfinding();
        MutablePoint waypoint = new MutablePoint();
        List<int[]> waypoints = new ArrayList<>();

        for (int leg = 0; leg < LEG_LIMIT; leg++) {
            Assertions.assertTrue(paths.updatePath(unit, targetX, targetY));
            Assertions.assertTrue(paths.getWaypoint(unit, waypoint));

            waypoints.add(new int[]{waypoint.getX(), waypoint.getY()});

            if (waypoint.getX() == targetX && waypoint.getY() == targetY) {
                return waypoints;
            }

            unit.setPosition(waypoint.getX(), waypoint.getY());
        }

        return Assertions.fail("The path does not reach the destination.");
    }

    /**
     * Checks that a unit of the soldier's size walking a leg keeps its bounds on walkable terrain and off a structure.
     */
    private void assertLegClear(@NotNull ArrayTerrainGrid terrain, @Nullable Unit structure, int fromX, int fromY, int toX, int toY) {
        int distance = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
        int steps = Math.max(1, distance / SAMPLE_DISTANCE);

        for (int step = 0; step <= steps; step++) {
            int x = fromX + (toX - fromX) * step / steps;
            int y = fromY + (toY - fromY) * step / steps;

            for (int cornerY = y - UNIT_EXTENT; cornerY <= y + UNIT_EXTENT; cornerY += 2 * UNIT_EXTENT) {
                for (int cornerX = x - UNIT_EXTENT; cornerX <= x + UNIT_EXTENT; cornerX += 2 * UNIT_EXTENT) {
                    Assertions.assertTrue(terrain.isWalkable(cornerX / WALK_CELL_SIZE, cornerY / WALK_CELL_SIZE),
                                          "the leg to " + toX + "," + toY + " reaches unwalkable terrain at " + cornerX + "," + cornerY);
                }
            }

            if (structure != null && (step < steps || toX != structure.getPositionX() || toY != structure.getPositionY())) {
                boolean inside = this.bounds.getLeft(structure) <= x && x <= this.bounds.getRight(structure) &&
                                 this.bounds.getTop(structure) <= y && y <= this.bounds.getBottom(structure);

                Assertions.assertFalse(inside, "the leg to " + toX + "," + toY + " crosses the structure at " + x + "," + y);
            }
        }
    }

    /**
     * Creates a walkable terrain with a wall of unwalkable tiles in tile column {@link #WALL_COLUMN} from the top down to the given row.
     */
    private static @NotNull ArrayTerrainGrid createWalledTerrain(int wallEndRow) {
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(TILE_SIZE, WALK_CELL_SIZE, MAP_TILES, MAP_TILES);

        for (int walkY = 0; walkY < terrain.getWalkHeight(); walkY++) {
            for (int walkX = 0; walkX < terrain.getWalkWidth(); walkX++) {
                boolean wall = walkX * WALK_CELL_SIZE / TILE_SIZE == WALL_COLUMN && walkY * WALK_CELL_SIZE / TILE_SIZE < wallEndRow;

                terrain.setWalkable(walkX, walkY, !wall);
            }
        }

        return terrain;
    }

    private @NotNull Unit createUnit(@NotNull Game game, @NotNull UnitType type, int x, int y) {
        Unit unit = game.getUnits().allocate();

        Assertions.assertNotNull(unit);

        unit.setOwner(game.getPlayer(0));
        unit.setType(type);
        unit.setPosition(x, y);

        return unit;
    }

    private @NotNull Game createGame(@NotNull ArrayTerrainGrid terrain) {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build())
                .useSystem(TableUnitBounds.KEY, context -> this.bounds)
                .useSystem(PathfindingSystem.KEY, context -> new StandardPathfindingSystem(terrain, this.bounds));

        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setCollisionLeft(UNIT_EXTENT)
                                                                                               .setCollisionUp(UNIT_EXTENT)
                                                                                               .setCollisionRight(UNIT_EXTENT)
                                                                                               .setCollisionDown(UNIT_EXTENT)
                                                                                               .build());
        builder.getContent().getUnitTypes().register(this.tower, new UnitDefinition.Builder().addTrait(UnitTrait.BUILDING)
                                                                                             .setCollisionLeft(TOWER_EXTENT)
                                                                                             .setCollisionUp(TOWER_EXTENT)
                                                                                             .setCollisionRight(TOWER_EXTENT)
                                                                                             .setCollisionDown(TOWER_EXTENT)
                                                                                             .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
