package org.fogofwarlabs.sc4j.engine.collision;

import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.spatial.SpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.StandardSpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the unit separation of the engine default collision system with the table bounds in an engine-only game: stacked idle units spread
 * until they overlap by at most the idle tolerance and then stand still in every frame, a moving unit pushes an idle unit out of its way and
 * arrives, and a unit slides along a building that does not move.
 */
final class StandardCollisionSystemTest {
    private static final int UNIT_LIMIT = 8;
    private static final int UNIT_EXTENT = 4;
    private static final int BUILDING_EXTENT = 16;
    private static final int SEARCH_DISTANCE = 64;
    private static final int FRAME_LIMIT = 300;
    private static final int STILL_FRAMES = 20;
    private static final int TOP_SPEED = 512;
    private static final int SPEED_FRACTION = 256;
    private static final int CROWD_SIZE = 5;
    private static final int POSITION_FRACTION_BITS = 8;

    private final @NotNull MotionProfile walker;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType tower;
    private final @NotNull TableUnitBounds bounds;

    StandardCollisionSystemTest() {
        this.walker  = new MotionProfile("walker");
        this.soldier = new UnitType("soldier");
        this.tower   = new UnitType("tower");
        this.bounds  = new TableUnitBounds();
    }

    @Test
    void stacked_idle_units_spread_until_they_overlap_at_most_the_tolerance_and_then_stand_still() {
        Game game = this.createGame();
        Unit first = this.createUnit(game, this.soldier, 100, 100);
        Unit second = this.createUnit(game, this.soldier, 100, 100);
        Unit third = this.createUnit(game, this.soldier, 101, 100);

        this.runFrames(game, FRAME_LIMIT);

        Assertions.assertFalse(this.overlapsBeyondTolerance(first, second));
        Assertions.assertFalse(this.overlapsBeyondTolerance(first, third));
        Assertions.assertFalse(this.overlapsBeyondTolerance(second, third));
        Assertions.assertTrue(game.getMovement().isAtDestination(first));

        int[] positions = {first.getPositionX(), first.getPositionY(), second.getPositionX(), second.getPositionY(), third.getPositionX(),
                           third.getPositionY()};

        for (int frame = 0; frame < STILL_FRAMES; frame++) {
            this.runFrames(game, 1);

            Assertions.assertArrayEquals(positions, new int[]{first.getPositionX(), first.getPositionY(), second.getPositionX(),
                                                              second.getPositionY(), third.getPositionX(), third.getPositionY()});
        }
    }

    @Test
    void a_moving_unit_pushes_an_idle_unit_out_of_its_way() {
        Game game = this.createGame();
        Unit mover = this.createUnit(game, this.soldier, 40, 100);
        Unit idle = this.createUnit(game, this.soldier, 100, 100);

        game.getMovement().setDestination(mover, 160, 100);

        for (int frame = 0; frame < FRAME_LIMIT && !game.getMovement().isAtDestination(mover); frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(160, mover.getPositionX());
        Assertions.assertNotEquals(100, idle.getPositionX());
        Assertions.assertFalse(this.overlaps(mover, idle));
    }

    @Test
    void units_slide_along_a_building_and_the_building_stays() {
        Game game = this.createGame();
        Unit building = this.createUnit(game, this.tower, 100, 100);
        Unit mover = this.createUnit(game, this.soldier, 40, 104);
        CollisionSystem collision = game.getCollision();

        Assertions.assertFalse(((StandardCollisionSystem) collision).isPushable(building));
        Assertions.assertTrue(((StandardCollisionSystem) collision).isPushable(mover));

        game.getMovement().setDestination(mover, 160, 104);

        for (int frame = 0; frame < FRAME_LIMIT && !game.getMovement().isAtDestination(mover); frame++) {
            game.getFrameRunner().runFrame();

            Assertions.assertFalse(this.overlapsDeeply(mover, building));
        }

        Assertions.assertEquals(160, mover.getPositionX());
        Assertions.assertEquals(104, mover.getPositionY());
        Assertions.assertEquals(100, building.getPositionX());
        Assertions.assertEquals(100, building.getPositionY());
    }

    @Test
    void squeezed_units_never_move_faster_than_their_top_speed() {
        Game game = this.createGame();
        List<Unit> units = new ArrayList<>();

        this.createUnit(game, this.tower, 100, 100);

        for (int index = 0; index < CROWD_SIZE; index++) {
            units.add(this.createUnit(game, this.soldier, 96 + index, 120));
        }

        Unit eastward = this.createUnit(game, this.soldier, 40, 121);
        Unit westward = this.createUnit(game, this.soldier, 160, 119);

        units.add(eastward);
        units.add(westward);
        game.getMovement().setDestination(eastward, 160, 121);
        game.getMovement().setDestination(westward, 40, 119);

        int[] previousX = new int[units.size()];
        int[] previousY = new int[units.size()];
        // The motion step moves whole pixels of a fixed point position, so a step may round up by one pixel.
        int bound = TOP_SPEED / SPEED_FRACTION + 1;

        for (int frame = 0; frame < FRAME_LIMIT; frame++) {
            for (int index = 0; index < units.size(); index++) {
                previousX[index] = units.get(index).getPositionX();
                previousY[index] = units.get(index).getPositionY();
            }

            game.getFrameRunner().runFrame();

            for (int index = 0; index < units.size(); index++) {
                int changeX = units.get(index).getPositionX() - previousX[index];
                int changeY = units.get(index).getPositionY() - previousY[index];

                Assertions.assertTrue(changeX * changeX + changeY * changeY <= bound * bound,
                                      "unit " + index + " moved " + changeX + "," + changeY + " in frame " + frame);
            }
        }

        for (int first = 0; first < units.size(); first++) {
            for (int second = first + 1; second < units.size(); second++) {
                Assertions.assertFalse(this.overlapsDeeply(units.get(first), units.get(second)), "units " + first + " and " + second + " overlap");
            }
        }
    }

    @Test
    void get_gap_should_measure_the_space_between_the_bounds() {
        Game game = this.createGame();
        Unit first = this.createUnit(game, this.soldier, 100, 100);
        Unit second = this.createUnit(game, this.tower, 100 + UNIT_EXTENT + BUILDING_EXTENT + 11, 100);

        Assertions.assertEquals(2 * UNIT_EXTENT + 1, this.bounds.getWidth(first));
        Assertions.assertEquals(2 * BUILDING_EXTENT + 1, this.bounds.getHeight(second));
        Assertions.assertEquals(10, this.bounds.getGap(first, second));
        Assertions.assertEquals(10, this.bounds.getGap(second, first));
    }

    private boolean overlaps(@NotNull Unit first, @NotNull Unit second) {
        return this.bounds.getLeft(first) <= this.bounds.getRight(second) && this.bounds.getLeft(second) <= this.bounds.getRight(first) &&
               this.bounds.getTop(first) <= this.bounds.getBottom(second) && this.bounds.getTop(second) <= this.bounds.getBottom(first);
    }

    /**
     * Checks whether two units overlap by more than the idle overlap tolerance on both axes.
     */
    private boolean overlapsBeyondTolerance(@NotNull Unit first, @NotNull Unit second) {
        int overlapX = Math.min(this.bounds.getRight(first), this.bounds.getRight(second)) -
                       Math.max(this.bounds.getLeft(first), this.bounds.getLeft(second)) + 1;
        int overlapY = Math.min(this.bounds.getBottom(first), this.bounds.getBottom(second)) -
                       Math.max(this.bounds.getTop(first), this.bounds.getTop(second)) + 1;

        return overlapX > StandardCollisionSystem.IDLE_OVERLAP_TOLERANCE && overlapY > StandardCollisionSystem.IDLE_OVERLAP_TOLERANCE;
    }

    /**
     * Checks whether two units overlap by more than the push distance of one frame on both axes.
     */
    private boolean overlapsDeeply(@NotNull Unit first, @NotNull Unit second) {
        int overlapX = Math.min(this.bounds.getRight(first), this.bounds.getRight(second)) -
                       Math.max(this.bounds.getLeft(first), this.bounds.getLeft(second)) + 1;
        int overlapY = Math.min(this.bounds.getBottom(first), this.bounds.getBottom(second)) -
                       Math.max(this.bounds.getTop(first), this.bounds.getTop(second)) + 1;

        return overlapX > 2 * StandardCollisionSystem.DEFAULT_PUSH_DISTANCE && overlapY > 2 * StandardCollisionSystem.DEFAULT_PUSH_DISTANCE;
    }

    private void runFrames(@NotNull Game game, int count) {
        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }
    }

    private @NotNull Unit createUnit(@NotNull Game game, @NotNull UnitType type, int x, int y) {
        Unit unit = game.getUnits().allocate();

        Assertions.assertNotNull(unit);

        unit.setOwner(game.getPlayer(0));
        unit.setType(type);
        unit.setMotionProfile(game.getContent().getUnitTypes().getMotionProfile(type));
        unit.setPosition(x, y);
        unit.setFixedPositionX(x << POSITION_FRACTION_BITS);
        unit.setFixedPositionY(y << POSITION_FRACTION_BITS);
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

    private @NotNull Game createGame() {
        int largestSize = 2 * BUILDING_EXTENT + 1;
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build())
                .useSystem(TableUnitBounds.KEY, context -> this.bounds)
                .useSystem(SpatialIndex.KEY, context -> new StandardSpatialIndex(this.bounds, largestSize, largestSize))
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(CollisionSystem.KEY, context -> new StandardCollisionSystem(this.bounds, SEARCH_DISTANCE))
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement)
                .addFramePhase(CollisionSystem.KEY, collision -> (StandardCollisionSystem) collision);

        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(TOP_SPEED)
                                                                                                            .setAcceleration(64)
                                                                                                            .setStoppingDistance(2048)
                                                                                                            .setTurnRate(40)
                                                                                                            .build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                               .setCollisionLeft(UNIT_EXTENT)
                                                                                               .setCollisionUp(UNIT_EXTENT)
                                                                                               .setCollisionRight(UNIT_EXTENT)
                                                                                               .setCollisionDown(UNIT_EXTENT)
                                                                                               .build());
        builder.getContent().getUnitTypes().register(this.tower, new UnitDefinition.Builder().addTrait(UnitTrait.BUILDING)
                                                                                             .setCollisionLeft(BUILDING_EXTENT)
                                                                                             .setCollisionUp(BUILDING_EXTENT)
                                                                                             .setCollisionRight(BUILDING_EXTENT)
                                                                                             .setCollisionDown(BUILDING_EXTENT)
                                                                                             .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
