package org.fogofwarlabs.sc4j.engine.content.definition;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.type.*;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the initial values of one {@link UnitType}: what each game copies into its unit type table when it is
 * created. A definition is immutable; type references are identities that must be registered in the same content.
 */
public final class UnitDefinition {
    /**
     * Builds {@link UnitDefinition} instances. Every value starts at {@code 0}, {@code false}, {@code null} or no cost
     * unless its setter says otherwise.
     */
    public static final class Builder {
        private @Nullable Faction faction;
        private @Nullable MotionProfile motionProfile;
        private int hitPoints;
        private int armor;
        private @Nullable UpgradeType armorUpgrade;
        private int sightRange;
        private int targetAcquisitionRange;
        private @Nullable WeaponType groundWeapon;
        private @Nullable WeaponType airWeapon;
        private @NotNull ResourceCost cost;
        private int buildTime;
        private int supplyProvided;
        private int supplyRequired;
        private int spaceRequired;
        private int spaceProvided;
        private int placementWidth;
        private int placementHeight;
        private int collisionLeft;
        private int collisionUp;
        private int collisionRight;
        private int collisionDown;
        private int traits;
        private final @Nullable OrderType @NotNull [] idleOrders;

        /**
         * Creates a {@link Builder} instance with the default values.
         */
        public Builder() {
            this.faction                = null;
            this.motionProfile          = null;
            this.hitPoints              = 0;
            this.armor                  = 0;
            this.armorUpgrade           = null;
            this.sightRange             = 0;
            this.targetAcquisitionRange = 0;
            this.groundWeapon           = null;
            this.airWeapon              = null;
            this.cost                   = ResourceCost.NONE;
            this.buildTime              = 0;
            this.supplyProvided         = 0;
            this.supplyRequired         = 0;
            this.spaceRequired          = 0;
            this.spaceProvided          = 0;
            this.placementWidth         = 0;
            this.placementHeight        = 0;
            this.collisionLeft          = 0;
            this.collisionUp            = 0;
            this.collisionRight         = 0;
            this.collisionDown          = 0;
            this.traits                 = 0;
            this.idleOrders             = new OrderType[ControllerKind.values().length];
        }

        /**
         * Sets the faction the unit type belongs to.
         *
         * @param faction the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setFaction(@Nullable Faction faction) {
            this.faction = faction;

            return this;
        }

        /**
         * Sets the motion profile that moves and draws the unit.
         *
         * @param motionProfile the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setMotionProfile(@Nullable MotionProfile motionProfile) {
            this.motionProfile = motionProfile;

            return this;
        }

        /**
         * Sets the maximum hit points, in the hit point unit of the mod.
         *
         * @param hitPoints the value
         * @return this instance for chaining
         */
        public @NotNull Builder setHitPoints(int hitPoints) {
            this.hitPoints = hitPoints;

            return this;
        }

        /**
         * Sets the base armor.
         *
         * @param armor the value
         * @return this instance for chaining
         */
        public @NotNull Builder setArmor(int armor) {
            this.armor = armor;

            return this;
        }

        /**
         * Sets the upgrade that raises the armor.
         *
         * @param armorUpgrade the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setArmorUpgrade(@Nullable UpgradeType armorUpgrade) {
            this.armorUpgrade = armorUpgrade;

            return this;
        }

        /**
         * Sets the sight range, in the distance unit of the mod.
         *
         * @param sightRange the value
         * @return this instance for chaining
         */
        public @NotNull Builder setSightRange(int sightRange) {
            this.sightRange = sightRange;

            return this;
        }

        /**
         * Sets the range in which the unit picks targets by itself, in the distance unit of the mod.
         *
         * @param targetAcquisitionRange the value
         * @return this instance for chaining
         */
        public @NotNull Builder setTargetAcquisitionRange(int targetAcquisitionRange) {
            this.targetAcquisitionRange = targetAcquisitionRange;

            return this;
        }

        /**
         * Sets the weapon against ground targets.
         *
         * @param groundWeapon the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setGroundWeapon(@Nullable WeaponType groundWeapon) {
            this.groundWeapon = groundWeapon;

            return this;
        }

        /**
         * Sets the weapon against air targets.
         *
         * @param airWeapon the type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setAirWeapon(@Nullable WeaponType airWeapon) {
            this.airWeapon = airWeapon;

            return this;
        }

        /**
         * Sets the cost of one unit.
         *
         * @param cost the cost
         * @return this instance for chaining
         */
        public @NotNull Builder setCost(@NotNull ResourceCost cost) {
            this.cost = cost;

            return this;
        }

        /**
         * Sets the build time in frames.
         *
         * @param buildTime the value
         * @return this instance for chaining
         */
        public @NotNull Builder setBuildTime(int buildTime) {
            this.buildTime = buildTime;

            return this;
        }

        /**
         * Sets the supply the unit provides to its owner.
         *
         * @param supplyProvided the value
         * @return this instance for chaining
         */
        public @NotNull Builder setSupplyProvided(int supplyProvided) {
            this.supplyProvided = supplyProvided;

            return this;
        }

        /**
         * Sets the supply the unit uses.
         *
         * @param supplyRequired the value
         * @return this instance for chaining
         */
        public @NotNull Builder setSupplyRequired(int supplyRequired) {
            this.supplyRequired = supplyRequired;

            return this;
        }

        /**
         * Sets the transport space the unit takes.
         *
         * @param spaceRequired the value
         * @return this instance for chaining
         */
        public @NotNull Builder setSpaceRequired(int spaceRequired) {
            this.spaceRequired = spaceRequired;

            return this;
        }

        /**
         * Sets the transport space the unit offers.
         *
         * @param spaceProvided the value
         * @return this instance for chaining
         */
        public @NotNull Builder setSpaceProvided(int spaceProvided) {
            this.spaceProvided = spaceProvided;

            return this;
        }

        /**
         * Sets the width of the placement footprint in pixels.
         *
         * @param placementWidth the value
         * @return this instance for chaining
         */
        public @NotNull Builder setPlacementWidth(int placementWidth) {
            this.placementWidth = placementWidth;

            return this;
        }

        /**
         * Sets the height of the placement footprint in pixels.
         *
         * @param placementHeight the value
         * @return this instance for chaining
         */
        public @NotNull Builder setPlacementHeight(int placementHeight) {
            this.placementHeight = placementHeight;

            return this;
        }

        /**
         * Sets the extent of the collision box left of the position, in pixels.
         *
         * @param collisionLeft the value
         * @return this instance for chaining
         */
        public @NotNull Builder setCollisionLeft(int collisionLeft) {
            this.collisionLeft = collisionLeft;

            return this;
        }

        /**
         * Sets the extent of the collision box above the position, in pixels.
         *
         * @param collisionUp the value
         * @return this instance for chaining
         */
        public @NotNull Builder setCollisionUp(int collisionUp) {
            this.collisionUp = collisionUp;

            return this;
        }

        /**
         * Sets the extent of the collision box right of the position, in pixels.
         *
         * @param collisionRight the value
         * @return this instance for chaining
         */
        public @NotNull Builder setCollisionRight(int collisionRight) {
            this.collisionRight = collisionRight;

            return this;
        }

        /**
         * Sets the extent of the collision box below the position, in pixels.
         *
         * @param collisionDown the value
         * @return this instance for chaining
         */
        public @NotNull Builder setCollisionDown(int collisionDown) {
            this.collisionDown = collisionDown;

            return this;
        }

        /**
         * Adds one of the traits.
         *
         * @param trait the trait
         * @return this instance for chaining
         */
        public @NotNull Builder addTrait(@NotNull UnitTrait trait) {
            this.traits |= trait.getMask();

            return this;
        }

        /**
         * Sets the order a unit of the type idles with when its owner has one controller kind: the order it starts with once completed.
         *
         * @param kind  the controller kind of the owner
         * @param order the order type, or {@code null} for none
         * @return this instance for chaining
         */
        public @NotNull Builder setIdleOrder(@NotNull ControllerKind kind, @Nullable OrderType order) {
            this.idleOrders[kind.ordinal()] = order;

            return this;
        }


        /**
         * Builds a {@link UnitDefinition} instance from the current values.
         *
         * @return the built {@link UnitDefinition} instance
         */
        public @NotNull UnitDefinition build() {
            return new UnitDefinition(this);
        }
    }

    private final @Nullable Faction faction;
    private final @Nullable MotionProfile motionProfile;
    private final int hitPoints;
    private final int armor;
    private final @Nullable UpgradeType armorUpgrade;
    private final int sightRange;
    private final int targetAcquisitionRange;
    private final @Nullable WeaponType groundWeapon;
    private final @Nullable WeaponType airWeapon;
    private final @NotNull ResourceCost cost;
    private final int buildTime;
    private final int supplyProvided;
    private final int supplyRequired;
    private final int spaceRequired;
    private final int spaceProvided;
    private final int placementWidth;
    private final int placementHeight;
    private final int collisionLeft;
    private final int collisionUp;
    private final int collisionRight;
    private final int collisionDown;
    private final int traits;
    private final @Nullable OrderType @NotNull [] idleOrders;

    private UnitDefinition(@NotNull Builder builder) {
        this.faction                = builder.faction;
        this.motionProfile          = builder.motionProfile;
        this.hitPoints              = builder.hitPoints;
        this.armor                  = builder.armor;
        this.armorUpgrade           = builder.armorUpgrade;
        this.sightRange             = builder.sightRange;
        this.targetAcquisitionRange = builder.targetAcquisitionRange;
        this.groundWeapon           = builder.groundWeapon;
        this.airWeapon              = builder.airWeapon;
        this.cost                   = builder.cost;
        this.buildTime              = builder.buildTime;
        this.supplyProvided         = builder.supplyProvided;
        this.supplyRequired         = builder.supplyRequired;
        this.spaceRequired          = builder.spaceRequired;
        this.spaceProvided          = builder.spaceProvided;
        this.placementWidth         = builder.placementWidth;
        this.placementHeight        = builder.placementHeight;
        this.collisionLeft          = builder.collisionLeft;
        this.collisionUp            = builder.collisionUp;
        this.collisionRight         = builder.collisionRight;
        this.collisionDown          = builder.collisionDown;
        this.traits                 = builder.traits;
        this.idleOrders             = builder.idleOrders.clone();
    }

    /**
     * @return the faction the unit type belongs to, or {@code null} for none
     */
    public @Nullable Faction getFaction() {
        return this.faction;
    }

    /**
     * @return the motion profile that moves and draws the unit, or {@code null} for none
     */
    public @Nullable MotionProfile getMotionProfile() {
        return this.motionProfile;
    }

    /**
     * @return the maximum hit points, in the hit point unit of the mod
     */
    public int getHitPoints() {
        return this.hitPoints;
    }

    /**
     * @return the base armor
     */
    public int getArmor() {
        return this.armor;
    }

    /**
     * @return the upgrade that raises the armor, or {@code null} for none
     */
    public @Nullable UpgradeType getArmorUpgrade() {
        return this.armorUpgrade;
    }

    /**
     * @return the sight range, in the distance unit of the mod
     */
    public int getSightRange() {
        return this.sightRange;
    }

    /**
     * @return the range in which the unit picks targets by itself, in the distance unit of the mod
     */
    public int getTargetAcquisitionRange() {
        return this.targetAcquisitionRange;
    }

    /**
     * @return the weapon against ground targets, or {@code null} for none
     */
    public @Nullable WeaponType getGroundWeapon() {
        return this.groundWeapon;
    }

    /**
     * @return the weapon against air targets, or {@code null} for none
     */
    public @Nullable WeaponType getAirWeapon() {
        return this.airWeapon;
    }

    /**
     * @return the cost of one unit
     */
    public @NotNull ResourceCost getCost() {
        return this.cost;
    }

    /**
     * @return the build time in frames
     */
    public int getBuildTime() {
        return this.buildTime;
    }

    /**
     * @return the supply the unit provides to its owner
     */
    public int getSupplyProvided() {
        return this.supplyProvided;
    }

    /**
     * @return the supply the unit uses
     */
    public int getSupplyRequired() {
        return this.supplyRequired;
    }

    /**
     * @return the transport space the unit takes
     */
    public int getSpaceRequired() {
        return this.spaceRequired;
    }

    /**
     * @return the transport space the unit offers
     */
    public int getSpaceProvided() {
        return this.spaceProvided;
    }

    /**
     * @return the width of the placement footprint in pixels
     */
    public int getPlacementWidth() {
        return this.placementWidth;
    }

    /**
     * @return the height of the placement footprint in pixels
     */
    public int getPlacementHeight() {
        return this.placementHeight;
    }

    /**
     * @return the extent of the collision box left of the position, in pixels
     */
    public int getCollisionLeft() {
        return this.collisionLeft;
    }

    /**
     * @return the extent of the collision box above the position, in pixels
     */
    public int getCollisionUp() {
        return this.collisionUp;
    }

    /**
     * @return the extent of the collision box right of the position, in pixels
     */
    public int getCollisionRight() {
        return this.collisionRight;
    }

    /**
     * @return the extent of the collision box below the position, in pixels
     */
    public int getCollisionDown() {
        return this.collisionDown;
    }

    /**
     * @return the traits, a mask of {@link UnitTrait#getMask()} bits
     */
    public int getTraits() {
        return this.traits;
    }

    /**
     * Returns the order a unit of the type idles with when its owner has one controller kind.
     *
     * @param kind the controller kind of the owner
     * @return the order type, or {@code null} for none
     */
    public @Nullable OrderType getIdleOrder(@NotNull ControllerKind kind) {
        return this.idleOrders[kind.ordinal()];
    }
}
