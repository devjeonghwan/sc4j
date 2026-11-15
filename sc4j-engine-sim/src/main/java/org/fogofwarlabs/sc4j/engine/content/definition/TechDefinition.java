package org.fogofwarlabs.sc4j.engine.content.definition;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.type.TechType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the initial values of one {@link TechType}: what each game copies into its tech type table when it is
 * created. A definition is immutable; type references are identities that must be registered in the same content.
 */
public final class TechDefinition {
    /**
     * Builds {@link TechDefinition} instances. Every value starts at {@code 0}, {@code false}, {@code null} or no cost
     * unless its setter says otherwise.
     */
    public static final class Builder {
        private @Nullable Faction faction;
        private @NotNull ResourceCost cost;
        private int researchTime;
        private int energyCost;

        /**
         * Creates a {@link Builder} instance with the default values.
         */
        public Builder() {
            this.faction      = null;
            this.cost         = ResourceCost.NONE;
            this.researchTime = 0;
            this.energyCost   = 0;
        }

        /**
         * Sets the faction that researches the tech.
         *
         * @param faction the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setFaction(@Nullable Faction faction) {
            this.faction = faction;

            return this;
        }

        /**
         * Sets the research cost.
         *
         * @param cost the cost
         * @return this instance for chaining
         */
        public @NotNull Builder setCost(@NotNull ResourceCost cost) {
            this.cost = cost;

            return this;
        }

        /**
         * Sets the research time in frames.
         *
         * @param researchTime the value
         * @return this instance for chaining
         */
        public @NotNull Builder setResearchTime(int researchTime) {
            this.researchTime = researchTime;

            return this;
        }

        /**
         * Sets the energy one use costs, in the energy unit of the mod.
         *
         * @param energyCost the value
         * @return this instance for chaining
         */
        public @NotNull Builder setEnergyCost(int energyCost) {
            this.energyCost = energyCost;

            return this;
        }


        /**
         * Builds a {@link TechDefinition} instance from the current values.
         *
         * @return the built {@link TechDefinition} instance
         */
        public @NotNull TechDefinition build() {
            return new TechDefinition(this);
        }
    }

    private final @Nullable Faction faction;
    private final @NotNull ResourceCost cost;
    private final int researchTime;
    private final int energyCost;

    private TechDefinition(@NotNull Builder builder) {
        this.faction      = builder.faction;
        this.cost         = builder.cost;
        this.researchTime = builder.researchTime;
        this.energyCost   = builder.energyCost;
    }

    /**
     * @return the faction that researches the tech, or {@code null} for none
     */
    public @Nullable Faction getFaction() {
        return this.faction;
    }

    /**
     * @return the research cost
     */
    public @NotNull ResourceCost getCost() {
        return this.cost;
    }

    /**
     * @return the research time in frames
     */
    public int getResearchTime() {
        return this.researchTime;
    }

    /**
     * @return the energy one use costs, in the energy unit of the mod
     */
    public int getEnergyCost() {
        return this.energyCost;
    }
}
