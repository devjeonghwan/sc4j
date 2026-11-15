package org.fogofwarlabs.sc4j.engine.projectile;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.combat.CombatSystem;
import org.fogofwarlabs.sc4j.engine.content.table.MotionProfileTable;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.TypeRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.entity.MovingEntity;
import org.fogofwarlabs.sc4j.engine.entity.Projectile;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.movement.*;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Flies weapon projectiles in the game's projectile pool by a motion model ({@link MotionModel}) the mod declares: the engine default
 * {@link ProjectileSystem}. A mod registers it under {@link ProjectileSystem#KEY}, lists it as a frame phase and sets the projectile limit of the
 * game ({@link org.fogofwarlabs.sc4j.engine.game.EngineLimits.Builder#setProjectiles}).
 * {@snippet :
 * builder.useSystem(ProjectileSystem.KEY, context -> new StandardProjectileSystem(255, motionModel))
 *        .addFramePhase(ProjectileSystem.KEY, projectiles -> (StandardProjectileSystem) projectiles);
 *}
 * <p>
 * A launched projectile starts at its source with the top speed, acceleration and turn rate of the weapon's projectile motion profile
 * ({@link org.fogofwarlabs.sc4j.engine.content.table.WeaponTypeTable#getProjectile}), facing its target. Each frame a homing projectile
 * ({@link org.fogofwarlabs.sc4j.engine.content.table.WeaponTypeTable#isHoming}) homes on its target while the target lives (its unit slot
 * still allocated in the generation of the launch, with hit points above {@code 0}): its destination, waypoint and direction target become the
 * target's position; a projectile that does not home keeps the target's position at the launch. Then it makes one motion step
 * ({@link StandardMotionStep}) and turns. A projectile standing on its destination hits: a homing projectile's living target takes the weapon hit
 * from the source ({@link CombatSystem#dealWeaponDamage}; the source only while it lives); a projectile that does not home lands at its
 * position ({@link CombatSystem#dealWeaponDamageAt}) and hits its target only if the target still lives and its collision bounds hold the
 * landing point. Then the projectile's slot is freed. A projectile whose motion profile cannot move hits at once; one that has flown the maximum
 * flight time vanishes without a hit.
 * <p>
 * Determinism contract: the system draws no random value and allocates nothing after it is attached; the projectiles move in slot order. The
 * weapon, source, target, their generations, flight time and homing flag of each slot are mixed into the state hash in slot order; the motion
 * fields are part of the pool hash.
 */
public final class StandardProjectileSystem implements ProjectileSystem, FramePhase, StateHashContributor, SnapshotContributor {
    private static <T> @NotNull T requireAttached(@Nullable T value) {
        if (value == null) {
            throw Exceptions.newIllegalStateException("The standard projectile system is not attached.");
        }

        return value;
    }

    private final int maximumFlightTime;
    private final @NotNull MotionModel motionModel;
    private final @NotNull StandardMotionStep step;

    private @Nullable Game game;
    private @Nullable EntityPool<Projectile> projectiles;
    private @Nullable EntityPool<Unit> units;
    private @Nullable MotionProfileProperties<MovingEntity> properties;

    private @Nullable WeaponType @NotNull [] weapons;
    private @Nullable Unit @NotNull [] sourceUnits;
    private @Nullable Unit @NotNull [] targetUnits;
    private int @NotNull [] sourceGenerations;
    private int @NotNull [] targetGenerations;
    private int @NotNull [] flightTimes;
    private boolean @NotNull [] homing;

    /**
     * Creates a {@link StandardProjectileSystem} instance; it flies nothing until it is attached.
     *
     * @param maximumFlightTime the number of frames a projectile flies at most before it vanishes, at least {@code 1}
     * @param motionModel       the motion model that moves the projectiles
     * @throws IllegalArgumentException if the maximum flight time is below {@code 1}
     */
    public StandardProjectileSystem(int maximumFlightTime, @NotNull MotionModel motionModel) {
        if (maximumFlightTime < 1) {
            throw Exceptions.newIllegalArgumentException("The maximum flight time {0} is below 1.", maximumFlightTime);
        }

        this.maximumFlightTime = maximumFlightTime;
        this.motionModel       = motionModel;
        this.step              = new StandardMotionStep();

        this.game        = null;
        this.projectiles = null;
        this.units       = null;
        this.properties  = null;

        this.weapons           = new WeaponType[0];
        this.sourceUnits       = new Unit[0];
        this.targetUnits       = new Unit[0];
        this.sourceGenerations = new int[0];
        this.targetGenerations = new int[0];
        this.flightTimes       = new int[0];
        this.homing            = new boolean[0];
    }

    /**
     * @return the number of projectiles in flight
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public int getActiveCount() {
        return requireAttached(this.projectiles).getAllocatedCount();
    }

    /**
     * @return the number of direction steps of a full turn of the projectiles' headings, clockwise from up
     */
    @Override
    public int getDirectionCount() {
        return this.motionModel.getDirectionCount();
    }


    /**
     * Resolves what this system uses from the game. Called once, after every system exists.
     * <p>
     * Binds the projectile and unit pools and the motion profile table, and sizes the weapon, source, target, flight time and homing flag of each
     * pool slot.
     *
     * @param game the game
     */
    @Override
    public void attach(@NotNull Game game) {
        int length = game.getProjectiles().getCapacity() + 1;

        this.game        = game;
        this.projectiles = game.getProjectiles();
        this.units       = game.getUnits();
        this.properties  = new TableMotionProfileProperties(game.getContent().getMotionProfiles());

        this.weapons           = new WeaponType[length];
        this.sourceUnits       = new Unit[length];
        this.targetUnits       = new Unit[length];
        this.sourceGenerations = new int[length];
        this.targetGenerations = new int[length];
        this.flightTimes       = new int[length];
        this.homing            = new boolean[length];
    }

    /**
     * Launches the projectile of a weapon from a unit at a target unit.
     *
     * @param weapon           the weapon, which names the projectile's motion profile
     * @param sourceUnit       the attacking unit, where the projectile starts
     * @param targetUnit       the target unit
     * @param instructionLimit the instruction limit of each animation program run the launch starts
     * @return {@code true} if a projectile was launched, {@code false} if the weapon has no projectile or the projectile pool is full
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public boolean launch(@NotNull WeaponType weapon, @NotNull Unit sourceUnit, @NotNull Unit targetUnit, int instructionLimit) {
        Game attachedGame = requireAttached(this.game);
        MotionProfile motionProfile = attachedGame.getContent().getWeaponTypes().getProjectile(weapon);

        if (motionProfile == null) {
            return false;
        }

        Projectile projectile = requireAttached(this.projectiles).allocate();

        if (projectile == null) {
            return false;
        }

        MotionProfileTable motionProfiles = attachedGame.getContent().getMotionProfiles();
        int x = sourceUnit.getPositionX();
        int y = sourceUnit.getPositionY();
        int heading = x == targetUnit.getPositionX() && y == targetUnit.getPositionY()
                      ? sourceUnit.getHeading()
                      : this.motionModel.calculateDirection(targetUnit.getPositionX() - x, targetUnit.getPositionY() - y);
        int index = projectile.getIndex();

        projectile.resetMotion();
        projectile.setMotionProfile(motionProfile);
        projectile.setPosition(x, y);
        projectile.setFixedPositionX(x << MotionModel.FRACTION_BITS);
        projectile.setFixedPositionY(y << MotionModel.FRACTION_BITS);
        projectile.setTopSpeed(motionProfiles.getTopSpeed(motionProfile));
        projectile.setAcceleration(motionProfiles.getAcceleration(motionProfile));
        projectile.setTurnRate(motionProfiles.getTurnRate(motionProfile));
        projectile.setHeading(heading);
        projectile.setTargetHeading(heading);
        this.motionModel.setDirection(projectile, heading);
        this.aim(projectile, targetUnit);

        EntityPool<Unit> unitPool = requireAttached(this.units);

        this.weapons[index]           = weapon;
        this.sourceUnits[index]       = sourceUnit;
        this.targetUnits[index]       = targetUnit;
        this.sourceGenerations[index] = unitPool.getGeneration(sourceUnit.getIndex());
        this.targetGenerations[index] = unitPool.getGeneration(targetUnit.getIndex());
        this.flightTimes[index]       = 0;
        this.homing[index]            = attachedGame.getContent().getWeaponTypes().isHoming(weapon);

        if (projectile.getTopSpeed() == 0) {
            this.hit(projectile, instructionLimit);
        }

        return true;
    }

    /**
     * Runs this phase of the current frame.
     * <p>
     * Moves every projectile of the pool in slot order and lets the arrived ones hit.
     *
     * @param game the game
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void run(@NotNull Game game) {
        EntityPool<Projectile> pool = requireAttached(this.projectiles);
        MotionProfileProperties<MovingEntity> attachedProperties = requireAttached(this.properties);
        SlotList used = pool.getUsedSlots();
        int index = used.getFirst();

        while (index != SlotList.NONE) {
            int next = used.getNext(index);
            Projectile projectile = pool.get(index);
            Unit target = this.findLiving(this.targetUnits[index], this.targetGenerations[index]);

            if (target != null && this.homing[index]) {
                this.aim(projectile, target);
            }

            this.motionModel.update(this.step, projectile, attachedProperties);
            this.step.commit(projectile);
            this.motionModel.updateFacing(projectile, attachedProperties);

            this.flightTimes[index]++;

            if (this.motionModel.isAtMovementDestination(projectile)) {
                this.hit(projectile, 0);
            } else if (this.flightTimes[index] >= this.maximumFlightTime) {
                this.release(projectile);
            }

            index = next;
        }
    }

    /**
     * Mixes this part of the simulation state into the hash: for each pool slot in slot order the weapon index, the slot indices of the source
     * and target units ({@code 0} for none), their generations, the flight time and the homing flag.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        for (int index = 0; index < this.weapons.length; index++) {
            WeaponType weapon = this.weapons[index];
            Unit source = this.sourceUnits[index];
            Unit target = this.targetUnits[index];

            hash.mix(weapon != null ? weapon.getIndex() : -1);
            hash.mix(source != null ? source.getIndex() : 0);
            hash.mix(target != null ? target.getIndex() : 0);
            hash.mix(this.sourceGenerations[index]);
            hash.mix(this.targetGenerations[index]);
            hash.mix(this.flightTimes[index]);
            hash.mixBoolean(this.homing[index]);
        }
    }

    /**
     * Writes this part of the simulation state: the number of projectile slots, then per slot the weapon, the source and target units with their
     * generations, the flight time and the homing flag.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.weapons.length);

        for (int index = 0; index < this.weapons.length; index++) {
            writer.writeType(this.weapons[index]);
            writer.writeEntity(this.sourceUnits[index]);
            writer.writeEntity(this.targetUnits[index]);
            writer.writeInt(this.sourceGenerations[index]);
            writer.writeInt(this.targetGenerations[index]);
            writer.writeInt(this.flightTimes[index]);
            writer.writeBoolean(this.homing[index]);
        }
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early, holds another number of slots or names no weapon type or unit of the game
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        TypeRegistry<WeaponType> weaponTypes = reader.getGame().getContent().getDefinition().getWeaponTypes().getTypes();
        EntityPool<Unit> unitPool = reader.getGame().getUnits();

        reader.readLength(this.weapons.length);

        for (int index = 0; index < this.weapons.length; index++) {
            this.weapons[index]           = reader.readType(weaponTypes);
            this.sourceUnits[index]       = reader.readEntity(unitPool);
            this.targetUnits[index]       = reader.readEntity(unitPool);
            this.sourceGenerations[index] = reader.readInt();
            this.targetGenerations[index] = reader.readInt();
            this.flightTimes[index]       = reader.readInt();
            this.homing[index]            = reader.readBoolean();
        }
    }

    private void aim(@NotNull Projectile projectile, @NotNull Unit target) {
        int x = target.getPositionX();
        int y = target.getPositionY();

        if (projectile.getMovementDestinationX() == x && projectile.getMovementDestinationY() == y) {
            return;
        }

        projectile.setMovementDestinationX(x);
        projectile.setMovementDestinationY(y);
        projectile.setMovementWaypointX(x);
        projectile.setMovementWaypointY(y);
        projectile.setDirectionTargetX(x);
        projectile.setDirectionTargetY(y);
        projectile.setMovementFlags((projectile.getMovementFlags() | MovementFlags.ORDERED) & ~MovementFlags.BRAKING);
    }

    private void hit(@NotNull Projectile projectile, int instructionLimit) {
        int index = projectile.getIndex();
        WeaponType weapon = this.weapons[index];
        Unit source = this.findLiving(this.sourceUnits[index], this.sourceGenerations[index]);
        Unit target = this.findLiving(this.targetUnits[index], this.targetGenerations[index]);

        if (weapon != null && this.homing[index]) {
            if (target != null) {
                requireAttached(this.game).getCombat().dealWeaponDamage(weapon, source, target, instructionLimit);
            }
        } else if (weapon != null) {
            int x = projectile.getPositionX();
            int y = projectile.getPositionY();
            Unit landedOn = target != null && this.isInCollisionBounds(target, x, y) ? target : null;

            requireAttached(this.game).getCombat().dealWeaponDamageAt(weapon, source, landedOn, x, y, instructionLimit);
        }

        this.release(projectile);
    }

    private void release(@NotNull Projectile projectile) {
        int index = projectile.getIndex();

        this.weapons[index]           = null;
        this.sourceUnits[index]       = null;
        this.targetUnits[index]       = null;
        this.sourceGenerations[index] = 0;
        this.targetGenerations[index] = 0;
        this.flightTimes[index]       = 0;
        this.homing[index]            = false;

        requireAttached(this.projectiles).free(projectile);
    }

    /**
     * Checks whether the collision bounds of a unit's type hold a point.
     */
    private boolean isInCollisionBounds(@NotNull Unit unit, int x, int y) {
        UnitType type = unit.getType();

        if (type == null) {
            return unit.getPositionX() == x && unit.getPositionY() == y;
        }

        UnitTypeTable unitTypes = requireAttached(this.game).getContent().getUnitTypes();

        return x >= unit.getPositionX() - unitTypes.getCollisionLeft(type) && x <= unit.getPositionX() + unitTypes.getCollisionRight(type) &&
               y >= unit.getPositionY() - unitTypes.getCollisionUp(type) && y <= unit.getPositionY() + unitTypes.getCollisionDown(type);
    }

    /**
     * Returns a unit the projectile remembers if it still lives: its slot is allocated in the same generation and it has hit points left.
     */
    private @Nullable Unit findLiving(@Nullable Unit unit, int generation) {
        EntityPool<Unit> unitPool = requireAttached(this.units);

        if (unit == null || !unitPool.isAllocated(unit.getIndex()) || unitPool.getGeneration(unit.getIndex()) != generation ||
            unit.getHitPoints() <= 0) {
            return null;
        }

        return unit;
    }
}
