package org.fogofwarlabs.sc4j.engine.content.faction;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds what the engine knows of a {@link Faction}: its worker, town hall and supply provider types, the resource kinds it
 * uses and the units it starts with. It is immutable and the same in every game. What a faction looks and sounds like in
 * the interface is mod data, not part of the definition.
 */
public final class FactionDefinition {
    /**
     * Builds {@link FactionDefinition} instances. Every type starts at {@code null} and every list empty.
     */
    public static final class Builder {
        private @Nullable UnitType worker;
        private @Nullable UnitType townHall;
        private @Nullable UnitType supplyProvider;
        private final @NotNull List<@NotNull ResourceKind> resourceKinds;
        private final @NotNull List<@NotNull UnitType> startUnits;

        /**
         * Creates a {@link Builder} instance without types.
         */
        public Builder() {
            this.worker         = null;
            this.townHall       = null;
            this.supplyProvider = null;
            this.resourceKinds  = new ArrayList<>();
            this.startUnits     = new ArrayList<>();
        }

        /**
         * Sets the unit type that gathers resources and constructs buildings.
         *
         * @param worker the unit type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setWorker(@Nullable UnitType worker) {
            this.worker = worker;

            return this;
        }

        /**
         * Sets the unit type that accepts gathered resources and produces workers.
         *
         * @param townHall the unit type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setTownHall(@Nullable UnitType townHall) {
            this.townHall = townHall;

            return this;
        }

        /**
         * Sets the unit type that the faction builds for more supply.
         *
         * @param supplyProvider the unit type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setSupplyProvider(@Nullable UnitType supplyProvider) {
            this.supplyProvider = supplyProvider;

            return this;
        }

        /**
         * Adds a resource kind the faction uses.
         *
         * @param resourceKind the resource kind
         * @return this instance for chaining
         */
        public @NotNull Builder addResourceKind(@NotNull ResourceKind resourceKind) {
            this.resourceKinds.add(resourceKind);

            return this;
        }

        /**
         * Adds a unit a player of the faction starts with; a type added twice starts twice.
         *
         * @param startUnit the unit type
         * @return this instance for chaining
         */
        public @NotNull Builder addStartUnit(@NotNull UnitType startUnit) {
            this.startUnits.add(startUnit);

            return this;
        }


        /**
         * Builds a {@link FactionDefinition} instance from the current values.
         *
         * @return the built {@link FactionDefinition} instance
         */
        public @NotNull FactionDefinition build() {
            return new FactionDefinition(this);
        }
    }

    private final @Nullable UnitType worker;
    private final @Nullable UnitType townHall;
    private final @Nullable UnitType supplyProvider;
    private final @NotNull List<@NotNull ResourceKind> resourceKinds;
    private final @NotNull List<@NotNull UnitType> startUnits;

    private FactionDefinition(@NotNull Builder builder) {
        this.worker         = builder.worker;
        this.townHall       = builder.townHall;
        this.supplyProvider = builder.supplyProvider;
        this.resourceKinds  = List.copyOf(builder.resourceKinds);
        this.startUnits     = List.copyOf(builder.startUnits);
    }

    /**
     * @return the unit type that gathers resources and constructs buildings, or {@code null} for none
     */
    public @Nullable UnitType getWorker() {
        return this.worker;
    }

    /**
     * @return the unit type that accepts gathered resources and produces workers, or {@code null} for none
     */
    public @Nullable UnitType getTownHall() {
        return this.townHall;
    }

    /**
     * @return the unit type that the faction builds for more supply, or {@code null} for none
     */
    public @Nullable UnitType getSupplyProvider() {
        return this.supplyProvider;
    }

    /**
     * @return the resource kinds the faction uses, in the order they were added
     */
    public @NotNull List<@NotNull ResourceKind> getResourceKinds() {
        return this.resourceKinds;
    }

    /**
     * @return the units a player of the faction starts with, in the order they were added
     */
    public @NotNull List<@NotNull UnitType> getStartUnits() {
        return this.startUnits;
    }
}
