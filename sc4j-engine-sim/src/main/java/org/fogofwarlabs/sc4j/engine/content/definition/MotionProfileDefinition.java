package org.fogofwarlabs.sc4j.engine.content.definition;

import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.SpriteType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the initial values of one {@link MotionProfile}: what each game copies into its motion profile table when it is
 * created. A definition is immutable; type references are identities that must be registered in the same content.
 */
public final class MotionProfileDefinition {
    /**
     * Builds {@link MotionProfileDefinition} instances. Every value starts at {@code 0}, {@code false} or {@code null}
     * unless its setter says otherwise.
     */
    public static final class Builder {
        private @Nullable SpriteType sprite;
        private int topSpeed;
        private int acceleration;
        private int stoppingDistance;
        private int turnRate;
        private boolean acceleratingTurn;

        /**
         * Creates a {@link Builder} instance with the default values.
         */
        public Builder() {
            this.sprite           = null;
            this.topSpeed         = 0;
            this.acceleration     = 0;
            this.stoppingDistance = 0;
            this.turnRate         = 0;
            this.acceleratingTurn = false;
        }

        /**
         * Sets the sprite type that draws the object.
         *
         * @param sprite the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setSprite(@Nullable SpriteType sprite) {
            this.sprite = sprite;

            return this;
        }

        /**
         * Sets the top speed, in the speed unit of the mod.
         *
         * @param topSpeed the value
         * @return this instance for chaining
         */
        public @NotNull Builder setTopSpeed(int topSpeed) {
            this.topSpeed = topSpeed;

            return this;
        }

        /**
         * Sets the acceleration, in the speed unit of the mod per frame.
         *
         * @param acceleration the value
         * @return this instance for chaining
         */
        public @NotNull Builder setAcceleration(int acceleration) {
            this.acceleration = acceleration;

            return this;
        }

        /**
         * Sets the distance needed to stop from top speed, in the distance unit of the mod.
         *
         * @param stoppingDistance the value
         * @return this instance for chaining
         */
        public @NotNull Builder setStoppingDistance(int stoppingDistance) {
            this.stoppingDistance = stoppingDistance;

            return this;
        }

        /**
         * Sets the direction change per frame, in the direction unit of the mod.
         *
         * @param turnRate the value
         * @return this instance for chaining
         */
        public @NotNull Builder setTurnRate(int turnRate) {
            this.turnRate = turnRate;

            return this;
        }

        /**
         * Sets whether the turn rate of an object grows by one each frame it turns.
         *
         * @param acceleratingTurn the value
         * @return this instance for chaining
         */
        public @NotNull Builder setAcceleratingTurn(boolean acceleratingTurn) {
            this.acceleratingTurn = acceleratingTurn;

            return this;
        }


        /**
         * Builds a {@link MotionProfileDefinition} instance from the current values.
         *
         * @return the built {@link MotionProfileDefinition} instance
         */
        public @NotNull MotionProfileDefinition build() {
            return new MotionProfileDefinition(this);
        }
    }

    private final @Nullable SpriteType sprite;
    private final int topSpeed;
    private final int acceleration;
    private final int stoppingDistance;
    private final int turnRate;
    private final boolean acceleratingTurn;

    private MotionProfileDefinition(@NotNull Builder builder) {
        this.sprite           = builder.sprite;
        this.topSpeed         = builder.topSpeed;
        this.acceleration     = builder.acceleration;
        this.stoppingDistance = builder.stoppingDistance;
        this.turnRate         = builder.turnRate;
        this.acceleratingTurn = builder.acceleratingTurn;
    }

    /**
     * @return the sprite type that draws the object, or {@code null} for none
     */
    public @Nullable SpriteType getSprite() {
        return this.sprite;
    }

    /**
     * @return the top speed, in the speed unit of the mod
     */
    public int getTopSpeed() {
        return this.topSpeed;
    }

    /**
     * @return the acceleration, in the speed unit of the mod per frame
     */
    public int getAcceleration() {
        return this.acceleration;
    }

    /**
     * @return the distance needed to stop from top speed, in the distance unit of the mod
     */
    public int getStoppingDistance() {
        return this.stoppingDistance;
    }

    /**
     * @return the direction change per frame, in the direction unit of the mod
     */
    public int getTurnRate() {
        return this.turnRate;
    }

    /**
     * @return {@code true} if the turn rate of an object grows by one each frame it turns, {@code false} otherwise
     */
    public boolean isAcceleratingTurn() {
        return this.acceleratingTurn;
    }
}
