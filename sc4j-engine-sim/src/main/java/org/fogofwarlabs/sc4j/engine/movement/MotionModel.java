package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.entity.MovingEntity;
import org.fogofwarlabs.sc4j.engine.math.IntegerMathUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Moves objects by their motion profiles: the engine's general motion model, whose constants are the data of its {@link Builder}. The standard
 * movement and projectile systems move their units and projectiles with the model the mod gives them; a mod with its own motion algorithm moves
 * its objects over the same {@link MovingEntity} fields and {@link MotionStep} instead.
 * <p>
 * Directions have {@link #getDirectionCount()} steps per full turn, growing clockwise from up; the direction toward a point is the nearest
 * step. Speeds, accelerations, exact positions and the components of a direction vector are fixed-point values with {@link #FRACTION_BITS}
 * fraction bits; positions are {@code int} pixels, so an exact position reaches {@code 2^(31 - FRACTION_BITS)} pixels on either side of
 * {@code 0}. Distances are Euclidean, rounded toward zero.
 * <p>
 * One motion step ({@link #update}, the step's commit, then {@link #updateFacing}): the desired movement direction points at the waypoint, or at
 * the direction target when the object stands on its waypoint, and the movement direction turns toward it by the turn rate; the speed changes by
 * the movement control ({@link #updateSpeed}); an accelerating object moves by its velocity and stops on its waypoint when the step would pass it;
 * then the heading turns toward the desired direction by the turn rate, and the turn rate of a motion profile with an accelerating turn grows by
 * one.
 * <p>
 * Determinism contract: the tables are computed once in integer arithmetic (a sine series in fixed point); every operation reads and writes
 * only the moved object and the step it is given, draws no random value and allocates nothing.
 */
public final class MotionModel {
    /**
     * The movement control of an object that accelerates, brakes and turns by its motion profile.
     */
    public static final int CONTROL_MOTION_PROFILE = 0;
    /**
     * The movement control of an object that moves by its motion profile but stops at once at its destination.
     */
    public static final int CONTROL_INSTANT_STOP = 1;
    /**
     * The movement control of an object whose speed comes from its walking animation (its next speed).
     */
    public static final int CONTROL_ANIMATION = 2;
    /**
     * The fraction bits of the fixed-point speeds, accelerations, exact positions and direction vector components.
     */
    public static final int FRACTION_BITS = 8;

    /**
     * The number of half turns of a full turn.
     */
    private static final int HALVES = 2;
    /**
     * The number of quarter turns of a full turn.
     */
    private static final int QUARTERS = 4;
    /**
     * The number of eighth turns of a full turn.
     */
    private static final int EIGHTHS = 8;
    /**
     * The fraction bits of the fixed-point angles and sines the tables are computed with.
     */
    private static final int TABLE_FRACTION_BITS = 30;
    /**
     * A quarter turn in radians with {@link #TABLE_FRACTION_BITS} fraction bits.
     */
    private static final long QUARTER_TURN_RADIANS = 1_686_629_713L;
    /**
     * The number of factors of the factorial each further term of the sine series divides by.
     */
    private static final int SERIES_STEP = 2;
    /**
     * The fraction bits of the slopes the direction search compares.
     */
    private static final int SLOPE_BITS = 16;
    /**
     * The whole in percent.
     */
    private static final int PERCENT = 100;
    /**
     * The factor of the squared speed in the calculated stopping distance: the squared speed divided by twice the acceleration.
     */
    private static final long STOPPING_DISTANCE_ACCELERATION_FACTOR = 2L;

    /**
     * Builds {@link MotionModel} instances: the constants of the motion.
     */
    public static final class Builder {
        private final int directionCount;
        private int instantStopSpeed;
        private int brakingCheckDistance;
        private int brakingMarginPercent;

        /**
         * Creates a {@link Builder} instance with the neutral settings: no instant stop speed (an object brakes to a halt at its destination),
         * no braking before a turn and a braking margin of the whole turn distance.
         *
         * @param directionCount the number of direction steps of a full turn, a multiple of four, at least four
         * @throws IllegalArgumentException if the direction count is below four or not a multiple of four
         */
        public Builder(int directionCount) {
            if (directionCount < QUARTERS || directionCount % QUARTERS != 0) {
                throw Exceptions.newIllegalArgumentException("Invalid direction count: {0}", directionCount);
            }

            this.directionCount       = directionCount;
            this.instantStopSpeed     = 0;
            this.brakingCheckDistance = 0;
            this.brakingMarginPercent = PERCENT;
        }

        /**
         * Sets the speed below which an object with {@link #CONTROL_MOTION_PROFILE} stops at once at its destination, unless it follows a path
         * step or passes through the destination.
         *
         * @param instantStopSpeed the speed, fixed-point, at least {@code 0}; {@code 0} lets every object brake to a halt
         * @return this instance for chaining
         * @throws IllegalArgumentException if the speed is negative
         */
        public @NotNull Builder setInstantStopSpeed(int instantStopSpeed) {
            this.instantStopSpeed = requireNotNegative(instantStopSpeed);

            return this;
        }

        /**
         * Sets the distance to its waypoint within which an object that must still turn brakes when the turn takes longer than the way left.
         *
         * @param brakingCheckDistance the distance in pixels, at least {@code 0}; {@code 0} never brakes for a turn
         * @return this instance for chaining
         * @throws IllegalArgumentException if the distance is negative
         */
        public @NotNull Builder setBrakingCheckDistance(int brakingCheckDistance) {
            this.brakingCheckDistance = requireNotNegative(brakingCheckDistance);

            return this;
        }

        /**
         * Sets the share of the distance an object covers while it turns that the braking before a turn compares with the way left.
         *
         * @param brakingMarginPercent the share in percent, at least {@code 0}
         * @return this instance for chaining
         * @throws IllegalArgumentException if the share is negative
         */
        public @NotNull Builder setBrakingMarginPercent(int brakingMarginPercent) {
            this.brakingMarginPercent = requireNotNegative(brakingMarginPercent);

            return this;
        }

        /**
         * Builds a {@link MotionModel} instance with these settings.
         *
         * @return the built {@link MotionModel} instance
         */
        public @NotNull MotionModel build() {
            return new MotionModel(this);
        }
    }

    /**
     * Calculates the sine of an angle by its series.
     *
     * @param angle the angle in radians with {@link #TABLE_FRACTION_BITS} fraction bits, {@code 0} up to a quarter turn
     * @return the sine with {@link #TABLE_FRACTION_BITS} fraction bits
     */
    private static long sine(long angle) {
        long sum = angle;
        long term = angle;

        for (int factor = SERIES_STEP; term != 0; factor += SERIES_STEP) {
            term = (term * angle >> TABLE_FRACTION_BITS) * angle >> TABLE_FRACTION_BITS;
            term = -term / ((long) factor * (factor + 1));
            sum += term;
        }

        return sum;
    }

    /**
     * Rounds a value with {@link #TABLE_FRACTION_BITS} fraction bits to {@link #FRACTION_BITS} fraction bits, halves away from zero.
     */
    private static int roundComponent(long value) {
        int shift = TABLE_FRACTION_BITS - FRACTION_BITS;
        long half = 1L << (shift - 1);

        return (int) (value >= 0 ? value + half >> shift : -(-value + half >> shift));
    }

    private static int requireNotNegative(int value) {
        if (value < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid negative motion setting: {0}", value);
        }

        return value;
    }

    private final int directionCount;
    private final int instantStopSpeed;
    private final int brakingCheckDistance;
    private final int brakingMarginPercent;

    private final int @NotNull [] horizontalComponents;
    private final int @NotNull [] verticalComponents;
    private final long @NotNull [] slopeThresholds;

    private MotionModel(@NotNull Builder builder) {
        int quarterTurn = builder.directionCount / QUARTERS;
        long[] quarterSines = new long[quarterTurn + 1];

        for (int step = 0; step <= quarterTurn; step++) {
            quarterSines[step] = sine(QUARTER_TURN_RADIANS * step / quarterTurn);
        }

        this.directionCount       = builder.directionCount;
        this.instantStopSpeed     = builder.instantStopSpeed;
        this.brakingCheckDistance = builder.brakingCheckDistance;
        this.brakingMarginPercent = builder.brakingMarginPercent;

        this.horizontalComponents = new int[builder.directionCount];
        this.verticalComponents   = new int[builder.directionCount];
        this.slopeThresholds      = new long[quarterTurn];

        for (int direction = 0; direction < builder.directionCount; direction++) {
            int quadrant = direction / quarterTurn;
            int offset = direction % quarterTurn;
            long rising = quarterSines[offset];
            long falling = quarterSines[quarterTurn - offset];
            // Clockwise from up: x = sin, y = -cos of the angle; each quadrant mirrors the first one.
            long horizontal = quadrant == 0 ? rising : quadrant == 1 ? falling : quadrant == 2 ? -rising : -falling;
            long vertical = quadrant == 0 ? -falling : quadrant == 1 ? rising : quadrant == 2 ? falling : -rising;

            this.horizontalComponents[direction] = roundComponent(horizontal);
            this.verticalComponents[direction]   = roundComponent(vertical);
        }

        for (int step = 0; step < quarterTurn; step++) {
            long angle = QUARTER_TURN_RADIANS * (HALVES * step + 1) / (HALVES * quarterTurn);

            this.slopeThresholds[step] = (sine(angle) << SLOPE_BITS) / sine(QUARTER_TURN_RADIANS - angle);
        }
    }

    /**
     * @return the number of direction steps of a full turn
     */
    public int getDirectionCount() {
        return this.directionCount;
    }

    /**
     * @return the speed below which an object with {@link #CONTROL_MOTION_PROFILE} stops at once at its destination, fixed-point
     */
    public int getInstantStopSpeed() {
        return this.instantStopSpeed;
    }

    /**
     * @return the distance in pixels to its waypoint within which an object brakes for a turn
     */
    public int getBrakingCheckDistance() {
        return this.brakingCheckDistance;
    }

    /**
     * @return the share in percent of the distance covered while turning that the braking before a turn compares with the way left
     */
    public int getBrakingMarginPercent() {
        return this.brakingMarginPercent;
    }

    /**
     * Normalizes a direction to one turn.
     *
     * @param direction the direction, any number of turns away
     * @return the direction, {@code 0} up to the direction count less one
     */
    public int normalizeDirection(int direction) {
        return Math.floorMod(direction, this.directionCount);
    }

    /**
     * Calculates the nearest direction of a pixel delta: up for a delta of {@code (0, 0)}.
     *
     * @param horizontal the horizontal pixel delta
     * @param vertical   the vertical pixel delta
     * @return the direction, {@code 0} up to the direction count less one
     */
    public int calculateDirection(int horizontal, int vertical) {
        int quarterTurn = this.directionCount / QUARTERS;

        if (horizontal == 0) {
            return vertical > 0 ? this.directionCount / HALVES : 0;
        }

        long slope = (Math.abs((long) vertical) << SLOPE_BITS) / Math.abs((long) horizontal);
        int lower = 0;
        int upper = quarterTurn;

        while (lower < upper) {
            int middle = (lower + upper) >>> 1;

            if (slope <= this.slopeThresholds[middle]) {
                upper = middle;
            } else {
                lower = middle + 1;
            }
        }

        int base = horizontal > 0 ? quarterTurn : quarterTurn * (QUARTERS - 1);
        boolean clockwise = (horizontal > 0) == (vertical >= 0);

        return this.normalizeDirection(clockwise ? base + lower : base - lower);
    }

    /**
     * Calculates the horizontal component of a direction vector scaled by a distance.
     *
     * @param direction the direction, read modulo the direction count
     * @param distance  the signed length
     * @return the horizontal component, in the unit of the distance
     */
    public int getVectorX(int direction, int distance) {
        return (int) ((long) this.horizontalComponents[this.normalizeDirection(direction)] * distance >> FRACTION_BITS);
    }

    /**
     * Calculates the vertical component of a direction vector scaled by a distance.
     *
     * @param direction the direction, read modulo the direction count
     * @param distance  the signed length
     * @return the vertical component, in the unit of the distance
     */
    public int getVectorY(int direction, int distance) {
        return (int) ((long) this.verticalComponents[this.normalizeDirection(direction)] * distance >> FRACTION_BITS);
    }

    /**
     * Calculates the prospective motion of one movement step into a step: turns the object, updates its speed and computes the next position,
     * then restores the movement flags the calculation changed.
     *
     * @param <E>        the object class
     * @param step       receives the prospective values
     * @param object     the moving object
     * @param properties reads the motion profile values of the object
     */
    public <E extends MovingEntity> void update(@NotNull MotionStep step, @NotNull E object, @NotNull MotionProfileProperties<? super E> properties) {
        int originalFlags = object.getMovementFlags();

        step.setPreMovementFlags(originalFlags);
        this.updateDirection(object);
        this.updateSpeed(object, properties);
        this.calculateValues(step, object);
        object.setMovementFlags(originalFlags);
    }

    /**
     * @param object the moving object
     * @return {@code true} if the object stands on its movement destination, {@code false} otherwise
     */
    public boolean isAtMovementDestination(@NotNull MovingEntity object) {
        return object.getMovementDestinationX() == object.getPositionX() && object.getMovementDestinationY() == object.getPositionY();
    }

    /**
     * Updates the accelerating flag and the animation requests of a step without moving the object: an object away from its destination starts
     * to accelerate (an animation-controlled object not while an attack starts) and starts its walking animation, an object at its destination
     * stops accelerating and stops its walking animation; neither request is made while an attack starts.
     *
     * @param step   receives the animation requests
     * @param object the moving object
     */
    public void updateMovementFlags(@NotNull MotionStep step, @NotNull MovingEntity object) {
        step.setStartingMovement(false);
        step.setStoppingMovement(false);

        int flags = object.getMovementFlags();

        if (!this.isAtMovementDestination(object)) {
            if ((flags & MovementFlags.ACCELERATING) == 0) {
                if (object.getMovementControl() != CONTROL_ANIMATION || (flags & MovementFlags.STARTING_ATTACK) == 0) {
                    object.setMovementFlags(flags | MovementFlags.ACCELERATING);
                }

                if ((flags & MovementFlags.STARTING_ATTACK) == 0) {
                    step.setStartingMovement(true);
                }
            }
        } else if ((flags & MovementFlags.ACCELERATING) != 0) {
            object.setMovementFlags(flags & ~MovementFlags.ACCELERATING);

            if ((flags & MovementFlags.STARTING_ATTACK) == 0) {
                step.setStoppingMovement(true);
            }
        }
    }

    private void calculateValues(@NotNull MotionStep step, @NotNull MovingEntity object) {
        this.updateMovementFlags(step, object);

        int speed = object.getCurrentSpeed();
        int horizontal = object.getFixedPositionX();
        int vertical = object.getFixedPositionY();
        int positionX = object.getPositionX();
        int positionY = object.getPositionY();

        step.setSpeed(speed);

        if ((object.getMovementFlags() & MovementFlags.ACCELERATING) != 0 && speed != 0) {
            int targetX = object.getMovementWaypointX() << FRACTION_BITS;
            int targetY = object.getMovementWaypointY() << FRACTION_BITS;
            int distance = IntegerMathUtils.getDistance(horizontal, vertical, targetX, targetY);

            if (speed >= distance) {
                horizontal = targetX;
                vertical   = targetY;

                step.setSpeed(distance);
            } else {
                horizontal += object.getVelocityX();
                vertical += object.getVelocityY();
            }

            if (object.getMovementControl() == CONTROL_ANIMATION) {
                this.setSpeed(object, 0);
            }

            positionX = horizontal >> FRACTION_BITS;
            positionY = vertical >> FRACTION_BITS;
        }

        step.setPositionX(positionX);
        step.setPositionY(positionY);
        step.setFixedPositionX(horizontal);
        step.setFixedPositionY(vertical);
        step.setPostMovementFlags(object.getMovementFlags());
    }

    /**
     * Points the desired direction at the waypoint, or at the direction target when the object stands on its waypoint, and applies the turn of
     * one frame.
     *
     * @param object the moving object
     */
    public void updateDirection(@NotNull MovingEntity object) {
        int targetX = object.getMovementWaypointX();
        int targetY = object.getMovementWaypointY();

        if (targetX == object.getPositionX() && targetY == object.getPositionY()) {
            targetX = object.getDirectionTargetX();
            targetY = object.getDirectionTargetY();
        }

        int horizontal = targetX - object.getPositionX();
        int vertical = targetY - object.getPositionY();
        int direction = horizontal == 0 && vertical == 0 ? object.getHeading() : this.calculateDirection(horizontal, vertical);

        this.setDesiredDirection(object, direction);
    }

    /**
     * Sets the desired movement direction and turns the movement direction toward it by the turn rate, refreshing the velocity when it changes.
     *
     * @param object    the moving object
     * @param direction the desired direction
     */
    public void setDesiredDirection(@NotNull MovingEntity object, int direction) {
        int desired = this.normalizeDirection(direction);
        int heading = object.getTargetHeading();

        object.setDesiredMovementDirection(desired);

        if (heading != desired) {
            heading = this.turnToward(heading, desired, object.getTurnRate());
        }

        if (heading != object.getMovementDirection()) {
            object.setMovementDirection(heading);
            this.writeVelocity(object, object.getCurrentSpeed());
        }
    }

    /**
     * Turns the heading toward the desired movement direction by the turn rate; the turn rate of a motion profile with an accelerating turn
     * grows by one each frame, and the ordered flag clears once the object faces its direction.
     *
     * @param <E>        the object class
     * @param object     the turning object
     * @param properties reads the motion profile values of the object
     */
    public <E extends MovingEntity> void updateFacing(@NotNull E object, @NotNull MotionProfileProperties<? super E> properties) {
        int movementFlags = object.getMovementFlags();

        if ((movementFlags & MovementFlags.ACCELERATING) != 0 && (movementFlags & MovementFlags.ORDERED) == 0) {
            return;
        }

        int direction = object.getDesiredMovementDirection();
        int turn = object.getTurnRate();
        int heading = this.turnToward(object.getHeading(), direction, turn);

        object.setHeading(heading);

        if (properties.isAcceleratingTurn(object)) {
            object.setTurnRate(turn + 1);
        }

        if (object.getTargetHeading() == direction && heading == direction) {
            object.setMovementFlags(movementFlags & ~MovementFlags.ORDERED);
        }
    }

    /**
     * Updates the speed of one frame by the movement control: stops at the destination, accelerates toward the top speed, or decelerates when
     * the object must turn first or reaches its stopping distance.
     *
     * @param <E>        the object class
     * @param object     the moving object
     * @param properties reads the motion profile values of the object
     */
    public <E extends MovingEntity> void updateSpeed(@NotNull E object, @NotNull MotionProfileProperties<? super E> properties) {
        int control = object.getMovementControl();
        boolean atTarget = this.isAtMovementDestination(object);
        int flags = object.getMovementFlags();

        if ((control == CONTROL_INSTANT_STOP || control == CONTROL_ANIMATION) && atTarget) {
            object.setMovementFlags(flags & ~MovementFlags.BRAKING);
            this.setSpeed(object, 0);

            return;
        }

        if (control == CONTROL_ANIMATION) {
            this.updateAnimationSpeed(object, flags);

            return;
        }

        if (control == CONTROL_MOTION_PROFILE && atTarget && object.getNextSpeed() < this.instantStopSpeed &&
            (flags & (MovementFlags.MOVING | MovementFlags.PASSING_THROUGH)) == 0) {
            object.setMovementFlags(flags & ~MovementFlags.BRAKING);
            this.setSpeed(object, 0);

            return;
        }

        this.setSpeed(object, object.getNextSpeed());

        boolean decelerate = this.mustBrakeForTurn(object);

        if (!decelerate && control == CONTROL_MOTION_PROFILE &&
            ((flags & MovementFlags.MOVING) == 0 || (flags & MovementFlags.BRAKE_ON_PATH_STEP) != 0) &&
            ((flags & MovementFlags.PASSING_THROUGH) == 0 || object.getMovementWaypointX() != object.getMovementDestinationX() ||
             object.getMovementWaypointY() != object.getMovementDestinationY())) {
            int remaining = IntegerMathUtils.getDistance(object.getFixedPositionX(), object.getFixedPositionY(),
                                                         object.getMovementWaypointX() << FRACTION_BITS,
                                                         object.getMovementWaypointY() << FRACTION_BITS);

            decelerate = this.calculateStoppingDistance(properties, object) >= remaining;
        }

        int acceleration = object.getAcceleration();
        long speed = (long) object.getCurrentSpeed() + (decelerate ? -acceleration : acceleration);

        this.setSpeed(object, speed > object.getTopSpeed() ? object.getTopSpeed() : (int) Math.max(speed, 0L));
    }

    private void updateAnimationSpeed(@NotNull MovingEntity object, int flags) {
        if ((flags & MovementFlags.ORDERED) != 0 &&
            Math.abs(this.getTurnDifference(object.getHeading(), object.getDesiredMovementDirection())) >= this.directionCount / EIGHTHS) {
            if ((flags & MovementFlags.ACCELERATING) != 0) {
                object.setMovementFlags(flags & ~(MovementFlags.ACCELERATING | MovementFlags.BRAKING));
                this.setSpeed(object, 0);
            }

            return;
        }

        this.setSpeed(object, object.getNextSpeed());
    }

    /**
     * Checks whether an object near its waypoint must brake because its turn toward the desired direction takes longer than the way left: the
     * frames of the turn times the next speed, times the braking margin, against the distance to the waypoint.
     */
    private boolean mustBrakeForTurn(@NotNull MovingEntity object) {
        int turn = object.getTurnRate();
        int difference = Math.abs(this.getTurnDifference(object.getMovementDirection(), object.getDesiredMovementDirection()));

        if (difference == 0 || turn <= 0) {
            return false;
        }

        int distance = IntegerMathUtils.getDistance(object.getPositionX(), object.getPositionY(), object.getMovementWaypointX(),
                                                    object.getMovementWaypointY());

        if (distance >= this.brakingCheckDistance) {
            return false;
        }

        long frames = (difference + turn - 1L) / turn;
        long turnDistance = (frames * object.getNextSpeed() >> FRACTION_BITS) * this.brakingMarginPercent / PERCENT;

        return turnDistance > distance;
    }

    /**
     * Sets the current speed and refreshes the velocity when the speed changes.
     *
     * @param object the moving object
     * @param speed  the new current speed, fixed-point
     */
    public void setSpeed(@NotNull MovingEntity object, int speed) {
        if (object.getCurrentSpeed() != speed) {
            object.setCurrentSpeed(speed);
            this.writeVelocity(object, speed);
        }
    }

    /**
     * Sets the movement direction and refreshes the velocity when the direction changes.
     *
     * @param object    the moving object
     * @param direction the new movement direction, read modulo the direction count
     */
    public void setDirection(@NotNull MovingEntity object, int direction) {
        int normalized = this.normalizeDirection(direction);

        if (object.getMovementDirection() != normalized) {
            object.setMovementDirection(normalized);
            this.writeVelocity(object, object.getCurrentSpeed());
        }
    }

    /**
     * Calculates the stopping distance of a moving object: {@code 0} at rest, without acceleration or with a movement control other than
     * {@link #CONTROL_MOTION_PROFILE}, the motion profile's stopping distance at its top speed and acceleration, else the squared next speed
     * divided by twice the acceleration. The motion profile values are read only as far as the rule needs them.
     *
     * @param <E>        the object class
     * @param properties reads the motion profile values of the object
     * @param object     the moving object
     * @return the stopping distance, fixed-point
     */
    public <E extends MovingEntity> int calculateStoppingDistance(@NotNull MotionProfileProperties<? super E> properties, @NotNull E object) {
        int speed = object.getNextSpeed();
        int acceleration = object.getAcceleration();

        if (speed == 0 || acceleration <= 0 || object.getMovementControl() != CONTROL_MOTION_PROFILE) {
            return 0;
        }

        if (speed == properties.getTopSpeed(object) && acceleration == properties.getAcceleration(object)) {
            return properties.getStoppingDistance(object);
        }

        return (int) Math.min((long) speed * speed / (acceleration * STOPPING_DISTANCE_ACCELERATION_FACTOR), Integer.MAX_VALUE);
    }

    /**
     * Turns a direction toward another by at most a turn rate, the shorter way.
     */
    private int turnToward(int from, int to, int turnRate) {
        return this.normalizeDirection(from + Math.max(-turnRate, Math.min(turnRate, this.getTurnDifference(from, to))));
    }

    /**
     * Returns the shortest turn from one direction to another: positive clockwise, from minus half a turn up to less than half a turn.
     */
    private int getTurnDifference(int from, int to) {
        int difference = this.normalizeDirection(to - from);

        return difference >= this.directionCount / HALVES ? difference - this.directionCount : difference;
    }

    private void writeVelocity(@NotNull MovingEntity object, int speed) {
        object.setVelocityX(this.getVectorX(object.getMovementDirection(), speed));
        object.setVelocityY(this.getVectorY(object.getMovementDirection(), speed));
    }
}
