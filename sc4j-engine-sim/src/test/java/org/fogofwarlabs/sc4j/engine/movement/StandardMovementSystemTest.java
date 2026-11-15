package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.engine.collision.CollisionSystem;
import org.fogofwarlabs.sc4j.engine.collision.StandardCollisionSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.fogofwarlabs.sc4j.engine.path.PathfindingSystem;
import org.fogofwarlabs.sc4j.engine.path.StandardPathfindingSystem;
import org.fogofwarlabs.sc4j.engine.spatial.SpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.StandardSpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.UnitBounds;
import org.fogofwarlabs.sc4j.engine.terrain.ArrayTerrainGrid;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default movement and collision systems in an engine-only game: units accelerate by their motion profiles to their
 * destination, brake and stop there, follow and turn, ground units follow the path finding default around walls and stop before an
 * unreachable destination, and the collision default finds free positions over the spatial index and the terrain and makes units immovable.
 */
final class StandardMovementSystemTest {
    private static final int UNIT_LIMIT = 4;
    private static final int HALF_SIZE = 4;
    private static final int UNIT_SIZE = 2 * HALF_SIZE + 1;
    private static final int SEARCH_DISTANCE = 64;
    private static final int FRAME_LIMIT = 200;
    private static final int TILE_SIZE = 32;
    private static final int WALK_CELL_SIZE = 8;
    private static final int MAP_TILES = 8;
    private static final int WALL_TILE = 4;
    private static final int WALL_LENGTH = 6;
    private static final int PATH_FRAME_LIMIT = 1000;

    private static final int TOP_SPEED = 512;
    private static final int ACCELERATION = 64;
    private static final int STOPPING_DISTANCE = 2048;
    private static final int TURN_RATE = 40;
    private static final int DIRECTION_RIGHT = SkirmishScenario.MOTION_MODEL.getDirectionCount() / 4;
    private static final int DIRECTION_LEFT = SkirmishScenario.MOTION_MODEL.getDirectionCount() * 3 / 4;

    private static final @NotNull MotionProfile WALKER = new MotionProfile("walker");

    /**
     * Reads square bounds around the engine unit position; a unit is placed once it has an owner.
     */
    private static final @NotNull UnitBounds<Unit> SQUARE_BOUNDS = new UnitBounds<>() {
        @Override
        public int getLeft(@NotNull Unit unit) {
            return unit.getPositionX() - HALF_SIZE;
        }

        @Override
        public int getTop(@NotNull Unit unit) {
            return unit.getPositionY() - HALF_SIZE;
        }

        @Override
        public int getRight(@NotNull Unit unit) {
            return unit.getPositionX() + HALF_SIZE;
        }

        @Override
        public int getBottom(@NotNull Unit unit) {
            return unit.getPositionY() + HALF_SIZE;
        }

        @Override
        public int getWidth(@NotNull Unit unit) {
            return UNIT_SIZE;
        }

        @Override
        public int getHeight(@NotNull Unit unit) {
            return UNIT_SIZE;
        }

        @Override
        public int getCenterX(@NotNull Unit unit) {
            return unit.getPositionX();
        }

        @Override
        public int getCenterY(@NotNull Unit unit) {
            return unit.getPositionY();
        }

        @Override
        public boolean isPlaced(@NotNull Unit unit) {
            return unit.getOwner() != null;
        }
    };

    @Test
    void units_accelerate_to_their_destination_and_stop_there() {
        Game game = createGame();
        Unit unit = createUnit(game, 10, 10);
        MovementSystem movement = game.getMovement();

        movement.setDestination(unit, 120, 10);

        Assertions.assertFalse(movement.isAtDestination(unit));

        int maximumSpeed = runUntilArrived(game, unit);

        Assertions.assertEquals(120, unit.getPositionX());
        Assertions.assertEquals(10, unit.getPositionY());
        Assertions.assertEquals(TOP_SPEED, maximumSpeed);
        Assertions.assertEquals(DIRECTION_RIGHT, unit.getMovementDirection());
        Assertions.assertTrue(movement.isAtDestination(unit));
    }

    @Test
    void stopped_units_stand_still_and_turn_to_a_direction() {
        Game game = createGame();
        Unit unit = createUnit(game, 50, 50);
        MovementSystem movement = game.getMovement();

        movement.setDestination(unit, 50, 150);
        game.getFrameRunner().runFrame();
        game.getFrameRunner().runFrame();
        movement.stop(unit);

        int stoppedX = unit.getPositionX();
        int stoppedY = unit.getPositionY();

        movement.turnTo(unit, DIRECTION_LEFT);

        for (int frame = 0; frame < FRAME_LIMIT; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(stoppedX, unit.getPositionX());
        Assertions.assertEquals(stoppedY, unit.getPositionY());
        Assertions.assertEquals(DIRECTION_LEFT, unit.getHeading());
        Assertions.assertTrue(movement.isAtDestination(unit));
    }

    @Test
    void following_units_reach_their_target() {
        Game game = createGame();
        Unit unit = createUnit(game, 10, 10);
        Unit target = createUnit(game, 80, 90);

        game.getMovement().setTargetUnit(unit, target);
        runUntilArrived(game, unit);

        Assertions.assertEquals(80, unit.getPositionX());
        Assertions.assertEquals(90, unit.getPositionY());
    }

    @Test
    void collision_default_finds_free_positions_and_makes_units_immovable() {
        Game game = createGame();
        Unit blocker = createUnit(game, 100, 100);
        Unit unit = createUnit(game, 10, 10);
        CollisionSystem collision = game.getCollision();
        MutablePoint position = new MutablePoint();

        Assertions.assertTrue(collision.isBlocking(unit, blocker));
        Assertions.assertFalse(collision.isBlocking(unit, unit));
        Assertions.assertTrue(collision.findFreePosition(unit, 40, 40, position));
        Assertions.assertEquals(40, position.getX());
        Assertions.assertEquals(40, position.getY());
        Assertions.assertTrue(collision.findFreePosition(unit, 100, 100, position));
        Assertions.assertEquals(100 - UNIT_SIZE, position.getX());
        Assertions.assertEquals(100 - UNIT_SIZE, position.getY());

        game.getMovement().setDestination(unit, 60, 10);
        game.getFrameRunner().runFrame();
        collision.makeImmovable(unit);

        Assertions.assertTrue(unit.isImmovable());
        Assertions.assertEquals(0, unit.getCurrentSpeed());
        Assertions.assertTrue(game.getMovement().isAtDestination(unit));

        game.getMovement().setDestination(unit, 60, 10);

        Assertions.assertFalse(unit.isImmovable());
    }

    @Test
    void ground_units_follow_their_path_around_a_wall() {
        ArrayTerrainGrid terrain = createWalledTerrain(WALL_LENGTH);
        Game game = createPathGame(terrain);
        Unit unit = createUnit(game, TILE_SIZE + TILE_SIZE / 2, TILE_SIZE + TILE_SIZE / 2);
        int destinationX = (WALL_TILE + 2) * TILE_SIZE + TILE_SIZE / 2;
        int lowestY = unit.getPositionY();

        game.getMovement().setDestination(unit, destinationX, unit.getPositionY());

        for (int frame = 0; frame < PATH_FRAME_LIMIT && !game.getMovement().isAtDestination(unit); frame++) {
            game.getFrameRunner().runFrame();

            lowestY = Math.max(lowestY, unit.getPositionY());

            Assertions.assertTrue(terrain.isWalkable(unit.getPositionX() / WALK_CELL_SIZE, unit.getPositionY() / WALK_CELL_SIZE));
        }

        Assertions.assertEquals(destinationX, unit.getPositionX());
        Assertions.assertEquals(TILE_SIZE + TILE_SIZE / 2, unit.getPositionY());
        Assertions.assertTrue(lowestY >= WALL_LENGTH * TILE_SIZE);
    }

    @Test
    void ground_units_stop_when_their_destination_cannot_be_reached() {
        Game game = createPathGame(createWalledTerrain(MAP_TILES));
        Unit unit = createUnit(game, TILE_SIZE + TILE_SIZE / 2, TILE_SIZE + TILE_SIZE / 2);

        game.getMovement().setDestination(unit, (WALL_TILE + 2) * TILE_SIZE, TILE_SIZE);
        game.getFrameRunner().runFrame();

        Assertions.assertTrue(game.getMovement().isAtDestination(unit));
        Assertions.assertEquals(TILE_SIZE + TILE_SIZE / 2, unit.getPositionX());
    }

    @Test
    void collision_default_finds_free_positions_on_walkable_terrain() {
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(TILE_SIZE, WALK_CELL_SIZE, MAP_TILES, MAP_TILES);

        for (int walkY = 0; walkY < terrain.getWalkHeight(); walkY++) {
            for (int walkX = 0; walkX < terrain.getWalkWidth(); walkX++) {
                terrain.setWalkable(walkX, walkY, walkX * WALK_CELL_SIZE >= 2 * TILE_SIZE);
            }
        }

        Game game = createGame(terrain);
        Unit unit = createUnit(game, 3 * TILE_SIZE, 3 * TILE_SIZE);
        MutablePoint position = new MutablePoint();

        Assertions.assertTrue(game.getCollision().findFreePosition(unit, 3 * TILE_SIZE, 40, position));
        Assertions.assertEquals(3 * TILE_SIZE, position.getX());
        Assertions.assertEquals(40, position.getY());
        Assertions.assertTrue(game.getCollision().findFreePosition(unit, 2 * TILE_SIZE, 40, position));
        Assertions.assertTrue(position.getX() - HALF_SIZE >= 2 * TILE_SIZE);
    }

    private static int runUntilArrived(@NotNull Game game, @NotNull Unit unit) {
        int maximumSpeed = 0;

        for (int frame = 0; frame < FRAME_LIMIT && !game.getMovement().isAtDestination(unit); frame++) {
            game.getFrameRunner().runFrame();

            maximumSpeed = Math.max(maximumSpeed, unit.getCurrentSpeed());
        }

        return maximumSpeed;
    }

    private static @NotNull Unit createUnit(@NotNull Game game, int x, int y) {
        Unit unit = game.getUnits().allocate();

        Assertions.assertNotNull(unit);

        unit.setOwner(game.getPlayer(0));
        unit.setMotionProfile(WALKER);
        unit.setPosition(x, y);
        unit.setMovementDestinationX(x);
        unit.setMovementDestinationY(y);
        unit.setMovementWaypointX(x);
        unit.setMovementWaypointY(y);
        unit.setDirectionTargetX(x);
        unit.setDirectionTargetY(y);
        game.getMovement().recalculateSpeed(unit, 0);
        game.getSpatialIndex().insert(unit);

        return unit;
    }

    /**
     * Creates a walkable terrain with a wall of unwalkable tiles in tile column {@link #WALL_TILE} from the top down to the given length.
     */
    private static @NotNull ArrayTerrainGrid createWalledTerrain(int wallLength) {
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(TILE_SIZE, WALK_CELL_SIZE, MAP_TILES, MAP_TILES);

        for (int walkY = 0; walkY < terrain.getWalkHeight(); walkY++) {
            for (int walkX = 0; walkX < terrain.getWalkWidth(); walkX++) {
                boolean wall = walkX * WALK_CELL_SIZE / TILE_SIZE == WALL_TILE && walkY * WALK_CELL_SIZE / TILE_SIZE < wallLength;

                terrain.setWalkable(walkX, walkY, !wall);
            }
        }

        return terrain;
    }

    private static @NotNull Game createPathGame(@NotNull TerrainGrid terrain) {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build())
                .useSystem(SpatialIndex.KEY, context -> new StandardSpatialIndex(SQUARE_BOUNDS, UNIT_SIZE, UNIT_SIZE))
                .useSystem(PathfindingSystem.KEY, context -> new StandardPathfindingSystem(terrain, SQUARE_BOUNDS))
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        return buildGame(builder);
    }

    private static @NotNull Game createGame() {
        return createGame(null);
    }

    private static @NotNull Game createGame(@Nullable TerrainGrid terrain) {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build())
                .useSystem(SpatialIndex.KEY, context -> new StandardSpatialIndex(SQUARE_BOUNDS, UNIT_SIZE, UNIT_SIZE))
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(CollisionSystem.KEY, context -> new StandardCollisionSystem(SQUARE_BOUNDS, SEARCH_DISTANCE, terrain))
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        return buildGame(builder);
    }

    private static @NotNull Game buildGame(@NotNull GameDefinition.Builder builder) {
        builder.getContent().getMotionProfiles().register(WALKER, new MotionProfileDefinition.Builder().setTopSpeed(TOP_SPEED)
                                                                                                       .setAcceleration(ACCELERATION)
                                                                                                       .setStoppingDistance(STOPPING_DISTANCE)
                                                                                                       .setTurnRate(TURN_RATE)
                                                                                                       .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
