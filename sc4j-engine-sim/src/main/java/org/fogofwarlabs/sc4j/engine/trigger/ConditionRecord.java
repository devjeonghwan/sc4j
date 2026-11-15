package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one condition of a standard trigger: its type id ({@link StandardTriggerConditionUtils}, or a mod's own id) and the operands the type reads.
 * An operand a type does not read keeps its default.
 */
public final class ConditionRecord {
    /**
     * Builds {@link ConditionRecord} instances.
     */
    public static final class Builder {
        private final int type;

        private @NotNull PlayerSelector player;
        private @Nullable UnitType unitType;
        private @Nullable UnitTrait unitTrait;
        private @Nullable TriggerArea area;
        private @Nullable ResourceKind resourceKind;
        private @NotNull Comparison comparison;
        private int amount;
        private int timer;

        /**
         * Creates a {@link Builder} instance for the current player, any unit type, the whole map, comparison {@link Comparison#AT_LEAST}, amount
         * {@code 0} and timer {@code 0}.
         *
         * @param type the condition type id
         */
        public Builder(int type) {
            this.type = type;

            this.player       = PlayerSelector.CURRENT;
            this.unitType     = null;
            this.unitTrait    = null;
            this.area         = null;
            this.resourceKind = null;
            this.comparison   = Comparison.AT_LEAST;
            this.amount       = 0;
            this.timer        = 0;
        }

        /**
         * Sets the player the condition looks at.
         *
         * @param player the player selector
         * @return this instance for chaining
         */
        public @NotNull Builder setPlayer(@NotNull PlayerSelector player) {
            this.player = player;

            return this;
        }

        /**
         * Sets the unit type the condition counts.
         *
         * @param unitType the unit type, or {@code null} for any
         * @return this instance for chaining
         */
        public @NotNull Builder setUnitType(@Nullable UnitType unitType) {
            this.unitType = unitType;

            return this;
        }

        /**
         * Sets the trait the counted units' type must have.
         *
         * @param unitTrait the trait, or {@code null} for any
         * @return this instance for chaining
         */
        public @NotNull Builder setUnitTrait(@Nullable UnitTrait unitTrait) {
            this.unitTrait = unitTrait;

            return this;
        }

        /**
         * Sets the area the condition counts in.
         *
         * @param area the area, or {@code null} for the whole map
         * @return this instance for chaining
         */
        public @NotNull Builder setArea(@Nullable TriggerArea area) {
            this.area = area;

            return this;
        }

        /**
         * Sets the resource kind the condition measures.
         *
         * @param resourceKind the resource kind, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setResourceKind(@Nullable ResourceKind resourceKind) {
            this.resourceKind = resourceKind;

            return this;
        }

        /**
         * Sets how the measured value compares with the amount.
         *
         * @param comparison the comparison
         * @return this instance for chaining
         */
        public @NotNull Builder setComparison(@NotNull Comparison comparison) {
            this.comparison = comparison;

            return this;
        }

        /**
         * Sets the amount the measured value is compared with.
         *
         * @param amount the amount
         * @return this instance for chaining
         */
        public @NotNull Builder setAmount(int amount) {
            this.amount = amount;

            return this;
        }

        /**
         * Sets the timer the condition reads.
         *
         * @param timer the timer index, {@code 0} up to {@link StandardTriggerSystem#TIMER_COUNT} minus one
         * @return this instance for chaining
         */
        public @NotNull Builder setTimer(int timer) {
            this.timer = timer;

            return this;
        }

        /**
         * Builds a {@link ConditionRecord} instance with these operands.
         *
         * @return the built {@link ConditionRecord} instance
         */
        public @NotNull ConditionRecord build() {
            return new ConditionRecord(this);
        }
    }

    private final int type;
    private final @NotNull PlayerSelector player;
    private final @Nullable UnitType unitType;
    private final @Nullable UnitTrait unitTrait;
    private final @Nullable TriggerArea area;
    private final @Nullable ResourceKind resourceKind;
    private final @NotNull Comparison comparison;
    private final int amount;
    private final int timer;

    private ConditionRecord(@NotNull Builder builder) {
        this.type         = builder.type;
        this.player       = builder.player;
        this.unitType     = builder.unitType;
        this.unitTrait    = builder.unitTrait;
        this.area         = builder.area;
        this.resourceKind = builder.resourceKind;
        this.comparison   = builder.comparison;
        this.amount       = builder.amount;
        this.timer        = builder.timer;
    }

    /**
     * @return the condition type id
     */
    public int getType() {
        return this.type;
    }

    /**
     * @return the player the condition looks at
     */
    public @NotNull PlayerSelector getPlayer() {
        return this.player;
    }

    /**
     * @return the unit type the condition counts, or {@code null} for any
     */
    public @Nullable UnitType getUnitType() {
        return this.unitType;
    }

    /**
     * @return the trait the counted units' type must have, or {@code null} for any
     */
    public @Nullable UnitTrait getUnitTrait() {
        return this.unitTrait;
    }

    /**
     * @return the area the condition counts in, or {@code null} for the whole map
     */
    public @Nullable TriggerArea getArea() {
        return this.area;
    }

    /**
     * @return the resource kind the condition measures, or {@code null} for none
     */
    public @Nullable ResourceKind getResourceKind() {
        return this.resourceKind;
    }

    /**
     * @return how the measured value compares with the amount
     */
    public @NotNull Comparison getComparison() {
        return this.comparison;
    }

    /**
     * @return the amount the measured value is compared with
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * @return the timer index the condition reads
     */
    public int getTimer() {
        return this.timer;
    }
}
