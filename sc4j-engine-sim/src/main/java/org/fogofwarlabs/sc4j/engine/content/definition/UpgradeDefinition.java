package org.fogofwarlabs.sc4j.engine.content.definition;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the initial values of one {@link UpgradeType}: what each game copies into its upgrade type table when it is
 * created. A definition is immutable; type references are identities that must be registered in the same content.
 */
public final class UpgradeDefinition {
    /**
     * Builds {@link UpgradeDefinition} instances. Every value starts at {@code 0}, {@code false}, {@code null} or no cost
     * unless its setter says otherwise.
     */
    public static final class Builder {
        private @Nullable Faction faction;
        private @NotNull ResourceCost baseCost;
        private @NotNull ResourceCost costFactor;
        private int baseTime;
        private int timeFactor;
        private int maximumLevel;

        /**
         * Creates a {@link Builder} instance with the default values.
         */
        public Builder() {
            this.faction      = null;
            this.baseCost     = ResourceCost.NONE;
            this.costFactor   = ResourceCost.NONE;
            this.baseTime     = 0;
            this.timeFactor   = 0;
            this.maximumLevel = 0;
        }

        /**
         * Sets the faction that researches the upgrade.
         *
         * @param faction the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setFaction(@Nullable Faction faction) {
            this.faction = faction;

            return this;
        }

        /**
         * Sets the cost of the first level.
         *
         * @param baseCost the cost
         * @return this instance for chaining
         */
        public @NotNull Builder setBaseCost(@NotNull ResourceCost baseCost) {
            this.baseCost = baseCost;

            return this;
        }

        /**
         * Sets the cost added per further level.
         *
         * @param costFactor the cost
         * @return this instance for chaining
         */
        public @NotNull Builder setCostFactor(@NotNull ResourceCost costFactor) {
            this.costFactor = costFactor;

            return this;
        }

        /**
         * Sets the research time of the first level in frames.
         *
         * @param baseTime the value
         * @return this instance for chaining
         */
        public @NotNull Builder setBaseTime(int baseTime) {
            this.baseTime = baseTime;

            return this;
        }

        /**
         * Sets the research time added per further level in frames.
         *
         * @param timeFactor the value
         * @return this instance for chaining
         */
        public @NotNull Builder setTimeFactor(int timeFactor) {
            this.timeFactor = timeFactor;

            return this;
        }

        /**
         * Sets the default maximum level.
         *
         * @param maximumLevel the value
         * @return this instance for chaining
         */
        public @NotNull Builder setMaximumLevel(int maximumLevel) {
            this.maximumLevel = maximumLevel;

            return this;
        }


        /**
         * Builds an {@link UpgradeDefinition} instance from the current values.
         *
         * @return the built {@link UpgradeDefinition} instance
         */
        public @NotNull UpgradeDefinition build() {
            return new UpgradeDefinition(this);
        }
    }

    private final @Nullable Faction faction;
    private final @NotNull ResourceCost baseCost;
    private final @NotNull ResourceCost costFactor;
    private final int baseTime;
    private final int timeFactor;
    private final int maximumLevel;

    private UpgradeDefinition(@NotNull Builder builder) {
        this.faction      = builder.faction;
        this.baseCost     = builder.baseCost;
        this.costFactor   = builder.costFactor;
        this.baseTime     = builder.baseTime;
        this.timeFactor   = builder.timeFactor;
        this.maximumLevel = builder.maximumLevel;
    }

    /**
     * @return the faction that researches the upgrade, or {@code null} for none
     */
    public @Nullable Faction getFaction() {
        return this.faction;
    }

    /**
     * @return the cost of the first level
     */
    public @NotNull ResourceCost getBaseCost() {
        return this.baseCost;
    }

    /**
     * @return the cost added per further level
     */
    public @NotNull ResourceCost getCostFactor() {
        return this.costFactor;
    }

    /**
     * @return the research time of the first level in frames
     */
    public int getBaseTime() {
        return this.baseTime;
    }

    /**
     * @return the research time added per further level in frames
     */
    public int getTimeFactor() {
        return this.timeFactor;
    }

    /**
     * @return the default maximum level
     */
    public int getMaximumLevel() {
        return this.maximumLevel;
    }
}
