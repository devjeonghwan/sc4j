package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.engine.entity.Projectile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the general motion model: its directions and direction vectors follow the declared direction count, the neutral model brakes a moving
 * object to a halt on its destination where a declared instant stop speed stops it at once, and positions beyond sixteen bits move.
 */
final class MotionModelTest {
    private static final int DIRECTION_COUNT = 64;
    private static final int QUARTER_TURN = DIRECTION_COUNT / 4;
    private static final int TOP_SPEED = 512;
    private static final int ACCELERATION = 128;
    private static final int TURN_RATE = DIRECTION_COUNT;
    private static final int FRAME_LIMIT = 400;
    private static final int FAR_POSITION = 1_000_000;
    private static final int TRAVEL = 100;
    private static final int ARRIVAL_SPEED = 3 * ACCELERATION;

    /**
     * Reads the motion profile values of a projectile from the test constants: no stopping distance of its own and no accelerating turn.
     */
    private static final @NotNull MotionProfileProperties<Projectile> PROPERTIES = new MotionProfileProperties<>() {
        @Override
        public int getTopSpeed(@NotNull Projectile object) {
            return TOP_SPEED;
        }

        @Override
        public int getAcceleration(@NotNull Projectile object) {
            return ACCELERATION;
        }

        @Override
        public int getStoppingDistance(@NotNull Projectile object) {
            return TOP_SPEED * TOP_SPEED / (2 * ACCELERATION);
        }

        @Override
        public boolean isAcceleratingTurn(@NotNull Projectile object) {
            return false;
        }
    };

    @Test
    void directions_follow_the_direction_count() {
        MotionModel model = new MotionModel.Builder(DIRECTION_COUNT).build();

        Assertions.assertEquals(0, model.calculateDirection(0, -5));
        Assertions.assertEquals(QUARTER_TURN, model.calculateDirection(5, 0));
        Assertions.assertEquals(QUARTER_TURN * 2, model.calculateDirection(0, 5));
        Assertions.assertEquals(QUARTER_TURN * 3, model.calculateDirection(-5, 0));
        Assertions.assertEquals(QUARTER_TURN / 2, model.calculateDirection(7, -7));
        Assertions.assertEquals(QUARTER_TURN * 3 + QUARTER_TURN / 2, model.calculateDirection(-7, -7));
        Assertions.assertEquals(QUARTER_TURN * 3, model.normalizeDirection(-QUARTER_TURN));

        Assertions.assertEquals(0, model.getVectorX(QUARTER_TURN * 2, TOP_SPEED));
        Assertions.assertEquals(TOP_SPEED, model.getVectorY(QUARTER_TURN * 2, TOP_SPEED));
        Assertions.assertEquals(-TOP_SPEED, model.getVectorX(QUARTER_TURN * 3, TOP_SPEED));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MotionModel.Builder(DIRECTION_COUNT + 2));
    }

    @Test
    void the_neutral_model_brakes_to_a_halt_on_the_destination() {
        MotionModel model = new MotionModel.Builder(DIRECTION_COUNT).build();
        Projectile object = createObject(0);

        int frames = moveTo(model, object, TRAVEL);

        Assertions.assertEquals(TRAVEL, object.getPositionX());
        Assertions.assertEquals(0, object.getCurrentSpeed());
        Assertions.assertTrue(frames < FRAME_LIMIT);
    }

    @Test
    void an_instant_stop_speed_stops_at_once_at_the_destination() {
        MotionModel neutral = new MotionModel.Builder(DIRECTION_COUNT).build();
        MotionModel instant = new MotionModel.Builder(DIRECTION_COUNT).setInstantStopSpeed(TOP_SPEED).build();
        Projectile braking = createArrivedObject();
        Projectile stopping = createArrivedObject();

        neutral.updateSpeed(braking, PROPERTIES);
        instant.updateSpeed(stopping, PROPERTIES);

        Assertions.assertEquals(ARRIVAL_SPEED - ACCELERATION, braking.getCurrentSpeed());
        Assertions.assertEquals(0, stopping.getCurrentSpeed());
    }

    @Test
    void positions_beyond_sixteen_bits_move() {
        MotionModel model = new MotionModel.Builder(DIRECTION_COUNT).build();
        Projectile object = createObject(FAR_POSITION);

        moveTo(model, object, FAR_POSITION + TRAVEL);

        Assertions.assertEquals(FAR_POSITION + TRAVEL, object.getPositionX());
        Assertions.assertEquals(FAR_POSITION + TRAVEL, object.getMovementDestinationX());
    }

    private static @NotNull Projectile createObject(int positionX) {
        Projectile object = new Projectile(1);

        object.setPosition(positionX, 0);
        object.setFixedPositionX(positionX << MotionModel.FRACTION_BITS);
        object.setFixedPositionY(0);
        object.setTopSpeed(TOP_SPEED);
        object.setAcceleration(ACCELERATION);
        object.setTurnRate(TURN_RATE);
        object.setHeading(QUARTER_TURN);
        object.setTargetHeading(QUARTER_TURN);
        object.setMovementDirection(QUARTER_TURN);

        return object;
    }

    private static @NotNull Projectile createArrivedObject() {
        Projectile object = createObject(TRAVEL);

        object.setMovementDestinationX(TRAVEL);
        object.setMovementWaypointX(TRAVEL);
        object.setCurrentSpeed(ARRIVAL_SPEED);
        object.setNextSpeed(ARRIVAL_SPEED);

        return object;
    }

    /**
     * Moves an object to a point on its row until it stands there at rest, and returns the number of frames it took.
     */
    private static int moveTo(@NotNull MotionModel model, @NotNull Projectile object, int x) {
        StandardMotionStep step = new StandardMotionStep();
        int frame = 0;

        object.setMovementDestinationX(x);
        object.setMovementDestinationY(0);
        object.setMovementWaypointX(x);
        object.setMovementWaypointY(0);
        object.setDirectionTargetX(x);
        object.setDirectionTargetY(0);
        object.setMovementFlags(MovementFlags.ORDERED);

        while (frame < FRAME_LIMIT && !(model.isAtMovementDestination(object) && object.getCurrentSpeed() == 0)) {
            model.update(step, object, PROPERTIES);
            step.commit(object);
            model.updateFacing(object, PROPERTIES);
            frame++;
        }

        return frame;
    }
}
