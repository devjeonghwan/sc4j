package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the settings of the engine default computer player ({@link StandardComputerController}): the orders it gives and the numbers its
 * decisions follow.
 */
public final class ComputerSettings {
    /**
     * The frames between two decisions when the mod sets none.
     */
    public static final int DEFAULT_DECISION_INTERVAL = 8;
    /**
     * The number of workers when the mod sets none.
     */
    public static final int DEFAULT_WORKER_TARGET = 10;
    /**
     * The free supply below which a supply provider is built when the mod sets none.
     */
    public static final int DEFAULT_SUPPLY_MARGIN = 4;
    /**
     * The number of production buildings when the mod sets none.
     */
    public static final int DEFAULT_PRODUCTION_BUILDING_TARGET = 2;
    /**
     * The size of the first attack wave when the mod sets none.
     */
    public static final int DEFAULT_FIRST_WAVE_SIZE = 6;
    /**
     * The growth of the attack waves when the mod sets none.
     */
    public static final int DEFAULT_WAVE_GROWTH = 2;
    /**
     * The largest attack wave size when the mod sets none.
     */
    public static final int DEFAULT_MAXIMUM_WAVE_SIZE = 16;
    /**
     * The building search radius in tiles when the mod sets none.
     */
    public static final int DEFAULT_BUILD_RADIUS = 12;
    /**
     * The spacing in pixels between buildings when the mod sets none.
     */
    public static final int DEFAULT_BUILDING_SPACING = 16;
    /**
     * The distance in pixels between buildings and resource containers when the mod sets none.
     */
    public static final int DEFAULT_RESOURCE_CLEARANCE = 96;
    /**
     * The resource search range in pixels when the mod sets none.
     */
    public static final int DEFAULT_RESOURCE_SEARCH_RANGE = 640;

    /**
     * Builds {@link ComputerSettings} instances. The defaults: a decision every {@value #DEFAULT_DECISION_INTERVAL} frames,
     * {@value #DEFAULT_WORKER_TARGET} workers, a supply margin of {@value #DEFAULT_SUPPLY_MARGIN}, {@value #DEFAULT_PRODUCTION_BUILDING_TARGET}
     * production buildings, a first wave of {@value #DEFAULT_FIRST_WAVE_SIZE} fighting units growing by {@value #DEFAULT_WAVE_GROWTH} per wave up
     * to {@value #DEFAULT_MAXIMUM_WAVE_SIZE}, a building search radius of {@value #DEFAULT_BUILD_RADIUS} tiles, a spacing of
     * {@value #DEFAULT_BUILDING_SPACING} pixels between buildings, a clearance of {@value #DEFAULT_RESOURCE_CLEARANCE} pixels from resource
     * containers and a resource search range of {@value #DEFAULT_RESOURCE_SEARCH_RANGE} pixels.
     */
    public static final class Builder {
        private final @NotNull OrderType gatherOrder;
        private final @NotNull OrderType attackMoveOrder;

        private int decisionInterval;
        private int workerTarget;
        private int supplyMargin;
        private int productionBuildingTarget;
        private int firstWaveSize;
        private int waveGrowth;
        private int maximumWaveSize;
        private int buildRadius;
        private int buildingSpacing;
        private int resourceClearance;
        private int resourceSearchRange;

        /**
         * Creates a {@link Builder} instance with the default numbers.
         *
         * @param gatherOrder     the order type a worker gets to gather from a resource container
         * @param attackMoveOrder the order type an attack wave gets towards the enemy
         */
        public Builder(@NotNull OrderType gatherOrder, @NotNull OrderType attackMoveOrder) {
            this.gatherOrder     = gatherOrder;
            this.attackMoveOrder = attackMoveOrder;

            this.decisionInterval         = DEFAULT_DECISION_INTERVAL;
            this.workerTarget             = DEFAULT_WORKER_TARGET;
            this.supplyMargin             = DEFAULT_SUPPLY_MARGIN;
            this.productionBuildingTarget = DEFAULT_PRODUCTION_BUILDING_TARGET;
            this.firstWaveSize            = DEFAULT_FIRST_WAVE_SIZE;
            this.waveGrowth               = DEFAULT_WAVE_GROWTH;
            this.maximumWaveSize          = DEFAULT_MAXIMUM_WAVE_SIZE;
            this.buildRadius              = DEFAULT_BUILD_RADIUS;
            this.buildingSpacing          = DEFAULT_BUILDING_SPACING;
            this.resourceClearance        = DEFAULT_RESOURCE_CLEARANCE;
            this.resourceSearchRange      = DEFAULT_RESOURCE_SEARCH_RANGE;
        }

        /**
         * Sets the number of frames between two decisions.
         *
         * @param decisionInterval the interval, at least {@code 1}
         * @return this instance for chaining
         */
        public @NotNull Builder setDecisionInterval(int decisionInterval) {
            this.decisionInterval = decisionInterval;

            return this;
        }

        /**
         * Sets the number of workers the computer player trains.
         *
         * @param workerTarget the number of workers, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setWorkerTarget(int workerTarget) {
            this.workerTarget = workerTarget;

            return this;
        }

        /**
         * Sets the free supply below which the computer player builds a supply provider.
         *
         * @param supplyMargin the free supply, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setSupplyMargin(int supplyMargin) {
            this.supplyMargin = supplyMargin;

            return this;
        }

        /**
         * Sets the number of production buildings the computer player builds.
         *
         * @param productionBuildingTarget the number of buildings, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setProductionBuildingTarget(int productionBuildingTarget) {
            this.productionBuildingTarget = productionBuildingTarget;

            return this;
        }

        /**
         * Sets the number of idle fighting units that start the first attack wave.
         *
         * @param firstWaveSize the number of units, at least {@code 1}
         * @return this instance for chaining
         */
        public @NotNull Builder setFirstWaveSize(int firstWaveSize) {
            this.firstWaveSize = firstWaveSize;

            return this;
        }

        /**
         * Sets how many units each attack wave adds to the size of the next.
         *
         * @param waveGrowth the growth, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setWaveGrowth(int waveGrowth) {
            this.waveGrowth = waveGrowth;

            return this;
        }

        /**
         * Sets the largest size of an attack wave.
         *
         * @param maximumWaveSize the number of units, at least the first wave size
         * @return this instance for chaining
         */
        public @NotNull Builder setMaximumWaveSize(int maximumWaveSize) {
            this.maximumWaveSize = maximumWaveSize;

            return this;
        }

        /**
         * Sets the largest distance in tiles from the town hall at which buildings are placed.
         *
         * @param buildRadius the radius, at least {@code 1}
         * @return this instance for chaining
         */
        public @NotNull Builder setBuildRadius(int buildRadius) {
            this.buildRadius = buildRadius;

            return this;
        }

        /**
         * Sets the free space in pixels the computer player leaves between the buildings it places.
         *
         * @param buildingSpacing the spacing, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setBuildingSpacing(int buildingSpacing) {
            this.buildingSpacing = buildingSpacing;

            return this;
        }

        /**
         * Sets the distance in pixels the computer player keeps buildings away from resource containers.
         *
         * @param resourceClearance the distance, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setResourceClearance(int resourceClearance) {
            this.resourceClearance = resourceClearance;

            return this;
        }

        /**
         * Sets the distance in pixels from its town hall within which a worker of the computer player looks for resources.
         *
         * @param resourceSearchRange the range, at least {@code 0}
         * @return this instance for chaining
         */
        public @NotNull Builder setResourceSearchRange(int resourceSearchRange) {
            this.resourceSearchRange = resourceSearchRange;

            return this;
        }

        /**
         * Builds a {@link ComputerSettings} instance with these settings.
         *
         * @return the built {@link ComputerSettings} instance
         * @throws IllegalArgumentException if a number is outside its range
         */
        public @NotNull ComputerSettings build() {
            if (this.decisionInterval < 1 || this.firstWaveSize < 1 || this.maximumWaveSize < this.firstWaveSize || this.buildRadius < 1 ||
                this.workerTarget < 0 || this.supplyMargin < 0 || this.productionBuildingTarget < 0 || this.waveGrowth < 0 ||
                this.buildingSpacing < 0 || this.resourceClearance < 0 || this.resourceSearchRange < 0) {
                throw Exceptions.newIllegalArgumentException("A computer player setting is out of its range.");
            }

            return new ComputerSettings(this);
        }
    }

    private final @NotNull OrderType gatherOrder;
    private final @NotNull OrderType attackMoveOrder;

    private final int decisionInterval;
    private final int workerTarget;
    private final int supplyMargin;
    private final int productionBuildingTarget;
    private final int firstWaveSize;
    private final int waveGrowth;
    private final int maximumWaveSize;
    private final int buildRadius;
    private final int buildingSpacing;
    private final int resourceClearance;
    private final int resourceSearchRange;

    private ComputerSettings(@NotNull Builder builder) {
        this.gatherOrder     = builder.gatherOrder;
        this.attackMoveOrder = builder.attackMoveOrder;

        this.decisionInterval         = builder.decisionInterval;
        this.workerTarget             = builder.workerTarget;
        this.supplyMargin             = builder.supplyMargin;
        this.productionBuildingTarget = builder.productionBuildingTarget;
        this.firstWaveSize            = builder.firstWaveSize;
        this.waveGrowth               = builder.waveGrowth;
        this.maximumWaveSize          = builder.maximumWaveSize;
        this.buildRadius              = builder.buildRadius;
        this.buildingSpacing          = builder.buildingSpacing;
        this.resourceClearance        = builder.resourceClearance;
        this.resourceSearchRange      = builder.resourceSearchRange;
    }

    /**
     * @return the order type a worker gets to gather from a resource container
     */
    public @NotNull OrderType getGatherOrder() {
        return this.gatherOrder;
    }

    /**
     * @return the order type an attack wave gets towards the enemy
     */
    public @NotNull OrderType getAttackMoveOrder() {
        return this.attackMoveOrder;
    }

    /**
     * @return the number of frames between two decisions
     */
    public int getDecisionInterval() {
        return this.decisionInterval;
    }

    /**
     * @return the number of workers the computer player trains
     */
    public int getWorkerTarget() {
        return this.workerTarget;
    }

    /**
     * @return the free supply below which the computer player builds a supply provider
     */
    public int getSupplyMargin() {
        return this.supplyMargin;
    }

    /**
     * @return the number of production buildings the computer player builds
     */
    public int getProductionBuildingTarget() {
        return this.productionBuildingTarget;
    }

    /**
     * @return the number of idle fighting units that start the first attack wave
     */
    public int getFirstWaveSize() {
        return this.firstWaveSize;
    }

    /**
     * @return how many units each attack wave adds to the size of the next
     */
    public int getWaveGrowth() {
        return this.waveGrowth;
    }

    /**
     * @return the largest size of an attack wave
     */
    public int getMaximumWaveSize() {
        return this.maximumWaveSize;
    }

    /**
     * @return the largest distance in tiles from the town hall at which buildings are placed
     */
    public int getBuildRadius() {
        return this.buildRadius;
    }

    /**
     * @return the free space in pixels left between placed buildings
     */
    public int getBuildingSpacing() {
        return this.buildingSpacing;
    }

    /**
     * @return the distance in pixels buildings keep from resource containers
     */
    public int getResourceClearance() {
        return this.resourceClearance;
    }

    /**
     * @return the distance in pixels from the town hall within which workers look for resources
     */
    public int getResourceSearchRange() {
        return this.resourceSearchRange;
    }
}
