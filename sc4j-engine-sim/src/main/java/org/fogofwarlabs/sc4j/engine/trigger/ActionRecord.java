package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one action of a standard trigger: its type id ({@link StandardTriggerActionUtils}, or a mod's own id) and the operands the type reads. An
 * operand a type does not read keeps its default.
 */
public final class ActionRecord {
    /**
     * The amount of a unit action that acts on every matching unit.
     */
    public static final int ALL_UNITS = 0;

    /**
     * Builds {@link ActionRecord} instances.
     */
    public static final class Builder {
        private final int type;

        private @NotNull PlayerSelector player;
        private @NotNull PlayerSelector targetPlayer;
        private @Nullable UnitType unitType;
        private @Nullable TriggerArea area;
        private int amount;
        private int timer;
        private @NotNull String text;

        /**
         * Creates a {@link Builder} instance for the current player as player and target player, any unit type, the whole map, amount
         * {@link #ALL_UNITS}, timer {@code 0} and an empty text.
         *
         * @param type the action type id
         */
        public Builder(int type) {
            this.type = type;

            this.player       = PlayerSelector.CURRENT;
            this.targetPlayer = PlayerSelector.CURRENT;
            this.unitType     = null;
            this.area         = null;
            this.amount       = ALL_UNITS;
            this.timer        = 0;
            this.text         = "";
        }

        /**
         * Sets the player the action acts for or on.
         *
         * @param player the player selector
         * @return this instance for chaining
         */
        public @NotNull Builder setPlayer(@NotNull PlayerSelector player) {
            this.player = player;

            return this;
        }

        /**
         * Sets the player that receives units.
         *
         * @param targetPlayer the player selector
         * @return this instance for chaining
         */
        public @NotNull Builder setTargetPlayer(@NotNull PlayerSelector targetPlayer) {
            this.targetPlayer = targetPlayer;

            return this;
        }

        /**
         * Sets the unit type the action creates or acts on.
         *
         * @param unitType the unit type, or {@code null} for any
         * @return this instance for chaining
         */
        public @NotNull Builder setUnitType(@Nullable UnitType unitType) {
            this.unitType = unitType;

            return this;
        }

        /**
         * Sets the area the action acts in.
         *
         * @param area the area, or {@code null} for the whole map
         * @return this instance for chaining
         */
        public @NotNull Builder setArea(@Nullable TriggerArea area) {
            this.area = area;

            return this;
        }

        /**
         * Sets the amount: a number of units, frames or timer frames, as the type reads it.
         *
         * @param amount the amount
         * @return this instance for chaining
         */
        public @NotNull Builder setAmount(int amount) {
            this.amount = amount;

            return this;
        }

        /**
         * Sets the timer the action sets.
         *
         * @param timer the timer index, {@code 0} up to {@link StandardTriggerSystem#TIMER_COUNT} minus one
         * @return this instance for chaining
         */
        public @NotNull Builder setTimer(int timer) {
            this.timer = timer;

            return this;
        }

        /**
         * Sets the text of a message.
         *
         * @param text the text
         * @return this instance for chaining
         */
        public @NotNull Builder setText(@NotNull String text) {
            this.text = text;

            return this;
        }

        /**
         * Builds an {@link ActionRecord} instance with these operands.
         *
         * @return the built {@link ActionRecord} instance
         */
        public @NotNull ActionRecord build() {
            return new ActionRecord(this);
        }
    }

    private final int type;
    private final @NotNull PlayerSelector player;
    private final @NotNull PlayerSelector targetPlayer;
    private final @Nullable UnitType unitType;
    private final @Nullable TriggerArea area;
    private final int amount;
    private final int timer;
    private final @NotNull String text;

    private ActionRecord(@NotNull Builder builder) {
        this.type         = builder.type;
        this.player       = builder.player;
        this.targetPlayer = builder.targetPlayer;
        this.unitType     = builder.unitType;
        this.area         = builder.area;
        this.amount       = builder.amount;
        this.timer        = builder.timer;
        this.text         = builder.text;
    }

    /**
     * @return the action type id
     */
    public int getType() {
        return this.type;
    }

    /**
     * @return the player the action acts for or on
     */
    public @NotNull PlayerSelector getPlayer() {
        return this.player;
    }

    /**
     * @return the player that receives units
     */
    public @NotNull PlayerSelector getTargetPlayer() {
        return this.targetPlayer;
    }

    /**
     * @return the unit type the action creates or acts on, or {@code null} for any
     */
    public @Nullable UnitType getUnitType() {
        return this.unitType;
    }

    /**
     * @return the area the action acts in, or {@code null} for the whole map
     */
    public @Nullable TriggerArea getArea() {
        return this.area;
    }

    /**
     * @return the amount: a number of units ({@link #ALL_UNITS} for every matching unit), frames or timer frames
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * @return the timer index the action sets
     */
    public int getTimer() {
        return this.timer;
    }

    /**
     * @return the text of a message
     */
    public @NotNull String getText() {
        return this.text;
    }
}
