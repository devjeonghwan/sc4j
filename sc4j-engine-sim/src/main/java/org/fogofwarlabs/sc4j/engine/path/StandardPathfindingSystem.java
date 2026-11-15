package org.fogofwarlabs.sc4j.engine.path;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.frame.FrameClock;
import org.fogofwarlabs.sc4j.engine.math.IntegerMathUtils;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.fogofwarlabs.sc4j.engine.spatial.UnitBounds;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Finds paths for the units of the game's unit pool over the walk cells of the terrain: the engine default {@link PathfindingSystem}. A mod
 * registers it under {@link PathfindingSystem#KEY} with the terrain of the game's map and the bounds of its units.
 * {@snippet :
 * builder.useSystem(PathfindingSystem.KEY, context -> new StandardPathfindingSystem(terrain, bounds));
 *}
 * <p>
 * The search works on the walk grid of the terrain through its {@link org.fogofwarlabs.sc4j.engine.map.grid.GridTopology} (cells, centres,
 * neighbours), so it serves any grid layout and any client projection. Each walk cell has a clearance: how many neighbour steps it lies from the
 * nearest blocked cell. A cell is blocked by unwalkable terrain and, while they stand, by the bounds of the structures: placed units whose type is
 * a {@link UnitTrait#BUILDING} or a {@link UnitTrait#RESOURCE_CONTAINER} and not a {@link UnitTrait#FLYER}. The structures are read again once
 * per frame, at the first path query of the frame, and a change recomputes the clearance.
 * <p>
 * A path is an A* search over the cells from the unit's cell to the destination's cell, where a cell narrower than the unit (a clearance below
 * the unit's requirement, from the unit's bounds and the cell size) costs more, then string pulled into at most {@value #WAYPOINT_CAPACITY}
 * waypoints: straight legs that a unit of its size can walk. A unit on a blocked cell starts from the nearest free cell. A destination on a
 * structure keeps the destination as the last waypoint, after the free cell nearest to it, so that the unit walks up to the structure; a
 * destination on unwalkable terrain ends the path at the nearest free cell, and a unit that arrives there has no path any more. A destination
 * whose nearest free cell lies in another component of the free cells is not reachable: the unit has no path.
 * <p>
 * A group sent to one destination searches once: a unit whose search would run in the frame of an earlier search to the same destination, from
 * the same component and for the same size, takes that search's path instead when it can walk straight to the path's first waypoint, so a
 * group order costs one search instead of one per unit.
 * <p>
 * A unit keeps its path while the destination stays and it can still walk straight to its current waypoint; it moves on to the next waypoint
 * within half a cell of the current one, or within {@value #LOOKAHEAD_CELLS} cells of it when it can walk straight to the next one. A
 * destination that moved by at most {@value #LOOKAHEAD_CELLS} cells (a followed unit) to a free cell becomes the last waypoint when the last leg
 * still reaches it straight. A unit at the last stored waypoint of a longer path, or whose way is cut (a new structure, or pushed behind a
 * corner), searches again.
 * <p>
 * Determinism contract: a path update depends only on the terrain, the structures, the unit's position and size, the destination and the
 * searches run before it in the same frame, which run in unit slot order; nothing draws a random value or allocates after the system is
 * attached. The paths, in slot order, are mixed into the state hash; the clearance is derived from the units and read again after a snapshot
 * is read.
 */
public final class StandardPathfindingSystem implements PathfindingSystem, StateHashContributor, SnapshotContributor {
    /**
     * The number of waypoints a unit's path holds at most.
     */
    public static final int WAYPOINT_CAPACITY = 24;

    /**
     * The number of cells the search for the free cell nearest to a point visits at most.
     */
    private static final int NEAREST_FREE_LIMIT = 4096;
    /**
     * The divisor of the cell size that gives the distance within which a waypoint is reached.
     */
    private static final int ARRIVAL_DIVISOR = 2;
    /**
     * The number of cells within which a unit moves on to the next waypoint when it can walk straight to it.
     */
    private static final int LOOKAHEAD_CELLS = 3;
    /**
     * The frame of no structure update yet.
     */
    private static final int NO_FRAME = -1;
    /**
     * The path flag of a last waypoint on a structure, which a straight walk need not reach.
     */
    private static final int OPEN_END = 1;
    /**
     * The path flag of a path that ends at the free cell nearest to a destination on unwalkable terrain.
     */
    private static final int SHORT_END = 2;
    /**
     * The path flag of a path with more waypoints than stored, searched again at the last stored one.
     */
    private static final int TRUNCATED = 4;

    private static <T> @NotNull T requireAttached(@Nullable T value) {
        if (value == null) {
            throw Exceptions.newIllegalStateException("The standard path finding system is not attached.");
        }

        return value;
    }

    private final @NotNull TerrainGrid terrain;
    private final @NotNull UnitBounds<Unit> bounds;

    private @Nullable WalkClearance clearance;
    private @Nullable GridPathSearch search;
    private @Nullable EntityPool<Unit> units;
    private @Nullable UnitTypeTable unitTypes;
    private @Nullable FrameClock clock;
    private int arrivalDistance;
    private int lookaheadDistance;
    private int searchCount;

    private int sharedFrame;
    private int sharedGoal;
    private int sharedRequirement;
    private int sharedTargetX;
    private int sharedTargetY;
    private int sharedFlags;
    private int sharedCount;
    private final int @NotNull [] sharedX;
    private final int @NotNull [] sharedY;

    private int structureFrame;
    private boolean @NotNull [] structurePlaced;
    private int @NotNull [] structureLeft;
    private int @NotNull [] structureTop;
    private int @NotNull [] structureRight;
    private int @NotNull [] structureBottom;

    private boolean @NotNull [] hasPath;
    private int @NotNull [] pathFlags;
    private int @NotNull [] destinationX;
    private int @NotNull [] destinationY;
    private int @NotNull [] waypointCounts;
    private int @NotNull [] waypointIndexes;
    private int @NotNull [] waypointX;
    private int @NotNull [] waypointY;

    /**
     * Creates a {@link StandardPathfindingSystem} instance; it finds no path until it is attached.
     *
     * @param terrain the terrain of the game's map
     * @param bounds  reads the bounds of a unit, for its size and the footprints of the structures
     */
    public StandardPathfindingSystem(@NotNull TerrainGrid terrain, @NotNull UnitBounds<Unit> bounds) {
        this.terrain = terrain;
        this.bounds  = bounds;

        this.clearance         = null;
        this.search            = null;
        this.units             = null;
        this.unitTypes         = null;
        this.clock             = null;
        this.arrivalDistance   = 0;
        this.lookaheadDistance = 0;
        this.searchCount       = 0;

        this.sharedFrame       = NO_FRAME;
        this.sharedGoal        = WalkClearance.OUTSIDE;
        this.sharedRequirement = 0;
        this.sharedTargetX     = 0;
        this.sharedTargetY     = 0;
        this.sharedFlags       = 0;
        this.sharedCount       = 0;
        this.sharedX           = new int[WAYPOINT_CAPACITY];
        this.sharedY           = new int[WAYPOINT_CAPACITY];

        this.structureFrame  = NO_FRAME;
        this.structurePlaced = new boolean[0];
        this.structureLeft   = new int[0];
        this.structureTop    = new int[0];
        this.structureRight  = new int[0];
        this.structureBottom = new int[0];

        this.hasPath         = new boolean[0];
        this.pathFlags       = new int[0];
        this.destinationX    = new int[0];
        this.destinationY    = new int[0];
        this.waypointCounts  = new int[0];
        this.waypointIndexes = new int[0];
        this.waypointX       = new int[0];
        this.waypointY       = new int[0];
    }

    /**
     * Resolves what this system uses from the game. Called once, after every system exists.
     * <p>
     * Computes the clearance of the terrain, creates the search and one path and structure slot per pool slot.
     *
     * @param game the game
     */
    @Override
    public void attach(@NotNull Game game) {
        WalkClearance attachedClearance = new WalkClearance(this.terrain);
        int slotCount = game.getUnits().getCapacity() + 1;

        this.clearance         = attachedClearance;
        this.search            = new GridPathSearch(attachedClearance);
        this.units             = game.getUnits();
        this.unitTypes         = game.getContent().getUnitTypes();
        this.clock             = game.getClock();
        this.arrivalDistance   = Math.max(1, attachedClearance.getCellSize() / ARRIVAL_DIVISOR);
        this.lookaheadDistance = attachedClearance.getCellSize() * LOOKAHEAD_CELLS;

        this.sharedFrame = NO_FRAME;

        this.structureFrame  = NO_FRAME;
        this.structurePlaced = new boolean[slotCount];
        this.structureLeft   = new int[slotCount];
        this.structureTop    = new int[slotCount];
        this.structureRight  = new int[slotCount];
        this.structureBottom = new int[slotCount];

        this.hasPath         = new boolean[slotCount];
        this.pathFlags       = new int[slotCount];
        this.destinationX    = new int[slotCount];
        this.destinationY    = new int[slotCount];
        this.waypointCounts  = new int[slotCount];
        this.waypointIndexes = new int[slotCount];
        this.waypointX       = new int[slotCount * WAYPOINT_CAPACITY];
        this.waypointY       = new int[slotCount * WAYPOINT_CAPACITY];
    }

    /**
     * Returns how many path searches the system ran since it was attached, for measuring the cost of the paths; it is no simulation state.
     *
     * @return the number of path searches
     */
    public int getSearchCount() {
        return this.searchCount;
    }

    /**
     * Brings the path of a unit to a destination up to date: a unit that has not reached its current waypoint keeps it,
     * a unit at its waypoint advances to the next one, and a unit without a usable path gets a new one. The current
     * waypoint afterwards is read with {@link #getWaypoint}.
     *
     * @param unit    the unit
     * @param targetX the x coordinate of the destination
     * @param targetY the y coordinate of the destination
     * @return {@code true} if the unit has a path afterwards, {@code false} if the destination cannot be reached
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public boolean updatePath(@NotNull Unit unit, int targetX, int targetY) {
        WalkClearance attachedClearance = requireAttached(this.clearance);
        int slot = unit.getIndex();

        this.updateStructures();

        if (this.hasPath[slot] && (this.destinationX[slot] != targetX || this.destinationY[slot] != targetY)) {
            this.retarget(attachedClearance, unit, slot, targetX, targetY);
        }

        if (this.hasPath[slot] && this.destinationX[slot] == targetX && this.destinationY[slot] == targetY) {
            int index = this.advance(attachedClearance, slot, unit.getPositionX(), unit.getPositionY());
            int last = this.waypointCounts[slot] - 1;
            int flags = this.pathFlags[slot];
            boolean arrived = index == last &&
                              IntegerMathUtils.isWithinDistance(unit.getPositionX(), unit.getPositionY(),
                                                                this.getWaypointX(slot, index), this.getWaypointY(slot, index), this.arrivalDistance);

            if (arrived && (flags & SHORT_END) != 0) {
                this.hasPath[slot] = false;

                return false;
            }

            boolean openLeg = index == last && (flags & OPEN_END) != 0;

            if (!(arrived && (flags & TRUNCATED) != 0) &&
                (openLeg || attachedClearance.isSegmentClear(unit.getPositionX(), unit.getPositionY(),
                                                             this.getWaypointX(slot, index), this.getWaypointY(slot, index), 1, false))) {
                return true;
            }
        }

        return this.plan(attachedClearance, unit, slot, targetX, targetY);
    }

    /**
     * Checks whether a unit can reach a point over the terrain.
     * <p>
     * The standard system tests whether the free cells nearest to the unit and to the point share a component.
     *
     * @param unit    the unit
     * @param targetX the x coordinate of the point
     * @param targetY the y coordinate of the point
     * @return {@code true} if the point is reachable for the unit, {@code false} otherwise
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public boolean isReachable(@NotNull Unit unit, int targetX, int targetY) {
        WalkClearance attachedClearance = requireAttached(this.clearance);

        this.updateStructures();

        int start = attachedClearance.findNearestFree(unit.getPositionX(), unit.getPositionY(), NEAREST_FREE_LIMIT);
        int goal = attachedClearance.findNearestFree(targetX, targetY, NEAREST_FREE_LIMIT);

        return start != WalkClearance.OUTSIDE && attachedClearance.getComponent(start) == attachedClearance.getComponent(goal);
    }

    /**
     * Reads the current waypoint of a unit's path.
     *
     * @param unit     the unit
     * @param waypoint receives the waypoint
     * @return {@code true} if the unit has a path and the waypoint was written, {@code false} otherwise
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public boolean getWaypoint(@NotNull Unit unit, @NotNull MutablePoint waypoint) {
        requireAttached(this.clearance);

        int slot = unit.getIndex();

        if (!this.hasPath[slot]) {
            return false;
        }

        waypoint.set(this.getWaypointX(slot, this.waypointIndexes[slot]), this.getWaypointY(slot, this.waypointIndexes[slot]));

        return true;
    }

    /**
     * Releases the path of a unit, for example when it stops or dies.
     *
     * @param unit the unit
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void releasePath(@NotNull Unit unit) {
        requireAttached(this.clearance);

        this.hasPath[unit.getIndex()] = false;
    }

    /**
     * Mixes this part of the simulation state into the hash: for every pool slot whether it has a path, then for a slot with a path its flags,
     * destination, waypoint count, current waypoint and waypoints.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        for (int slot = 0; slot < this.hasPath.length; slot++) {
            hash.mixBoolean(this.hasPath[slot]);

            if (!this.hasPath[slot]) {
                continue;
            }

            hash.mix(this.pathFlags[slot]);
            hash.mix(this.destinationX[slot]);
            hash.mix(this.destinationY[slot]);
            hash.mix(this.waypointCounts[slot]);
            hash.mix(this.waypointIndexes[slot]);

            for (int index = 0; index < this.waypointCounts[slot]; index++) {
                hash.mix(this.getWaypointX(slot, index));
                hash.mix(this.getWaypointY(slot, index));
            }
        }
    }

    /**
     * Writes this part of the simulation state: whether each unit slot has a path, then the flags, destinations, waypoint counts, current
     * waypoints and waypoints of every unit slot.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeBooleans(this.hasPath);
        writer.writeInts(this.pathFlags);
        writer.writeInts(this.destinationX);
        writer.writeInts(this.destinationY);
        writer.writeInts(this.waypointCounts);
        writer.writeInts(this.waypointIndexes);
        writer.writeInts(this.waypointX);
        writer.writeInts(this.waypointY);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values; the structures are read again at the next path query.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another number of values
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readBooleans(this.hasPath);
        reader.readInts(this.pathFlags);
        reader.readInts(this.destinationX);
        reader.readInts(this.destinationY);
        reader.readInts(this.waypointCounts);
        reader.readInts(this.waypointIndexes);
        reader.readInts(this.waypointX);
        reader.readInts(this.waypointY);

        this.sharedFrame    = NO_FRAME;
        this.structureFrame = NO_FRAME;
    }

    /**
     * Searches a new path for a unit, as the class description says.
     *
     * @return {@code true} if the unit has a path afterwards, {@code false} if the destination cannot be reached
     */
    private boolean plan(@NotNull WalkClearance attachedClearance, @NotNull Unit unit, int slot, int targetX, int targetY) {
        GridPathSearch attachedSearch = requireAttached(this.search);
        int positionX = unit.getPositionX();
        int positionY = unit.getPositionY();
        int start = attachedClearance.findNearestFree(positionX, positionY, NEAREST_FREE_LIMIT);
        int goal = attachedClearance.findNearestFree(targetX, targetY, NEAREST_FREE_LIMIT);

        this.hasPath[slot] = false;

        if (start == WalkClearance.OUTSIDE || attachedClearance.getComponent(start) != attachedClearance.getComponent(goal)) {
            return false;
        }

        int targetCell = attachedClearance.findCell(targetX, targetY);
        boolean targetFree = targetCell == goal;
        int endX = targetFree ? targetX : attachedClearance.getCenterX(goal);
        int endY = targetFree ? targetY : attachedClearance.getCenterY(goal);
        int requirement = this.findRequirement(attachedClearance, unit);
        int frame = requireAttached(this.clock).getFrame();

        if (this.sharedFrame == frame && this.sharedGoal == goal && this.sharedRequirement == requirement && this.sharedTargetX == targetX &&
            this.sharedTargetY == targetY && attachedClearance.isSegmentClear(positionX, positionY, this.sharedX[0], this.sharedY[0], 1, false)) {
            this.storePath(slot, targetX, targetY, this.sharedFlags, this.sharedCount, this.sharedX, this.sharedY);
            this.advance(attachedClearance, slot, positionX, positionY);

            return true;
        }

        this.searchCount++;

        if (!attachedSearch.search(positionX, positionY, start, goal, endX, endY, requirement)) {
            return false;
        }

        int count = attachedSearch.getWaypointCount();
        int flags = 0;

        if (!targetFree && (targetCell == WalkClearance.OUTSIDE || attachedClearance.isTerrainBlocked(targetCell))) {
            flags = SHORT_END;
        } else if (!targetFree) {
            // The last leg walks on from the free cell nearest to the structure to the destination on it.
            flags = OPEN_END;
            count++;
        }

        if (count > WAYPOINT_CAPACITY) {
            flags = TRUNCATED;
            count = WAYPOINT_CAPACITY;
        }

        for (int index = 0; index < count; index++) {
            boolean destination = index == attachedSearch.getWaypointCount();

            this.sharedX[index] = destination ? targetX : attachedSearch.getWaypointX(index);
            this.sharedY[index] = destination ? targetY : attachedSearch.getWaypointY(index);
        }

        this.sharedFrame       = frame;
        this.sharedGoal        = goal;
        this.sharedRequirement = requirement;
        this.sharedTargetX     = targetX;
        this.sharedTargetY     = targetY;
        this.sharedFlags       = flags;
        this.sharedCount       = count;

        this.storePath(slot, targetX, targetY, flags, count, this.sharedX, this.sharedY);
        this.advance(attachedClearance, slot, positionX, positionY);

        return true;
    }

    /**
     * Gives a unit's slot a path from its first waypoint on.
     */
    private void storePath(int slot, int targetX, int targetY, int flags, int count, int @NotNull [] pathX, int @NotNull [] pathY) {
        System.arraycopy(pathX, 0, this.waypointX, slot * WAYPOINT_CAPACITY, count);
        System.arraycopy(pathY, 0, this.waypointY, slot * WAYPOINT_CAPACITY, count);

        this.hasPath[slot]         = true;
        this.pathFlags[slot]       = flags;
        this.destinationX[slot]    = targetX;
        this.destinationY[slot]    = targetY;
        this.waypointCounts[slot]  = count;
        this.waypointIndexes[slot] = 0;
    }

    /**
     * Moves the end of a path that ends at its destination to a new destination near it, if the new destination lies on a free cell and the
     * last leg reaches it straight; otherwise the path stays for the old destination.
     */
    private void retarget(@NotNull WalkClearance attachedClearance, @NotNull Unit unit, int slot, int targetX, int targetY) {
        if (this.pathFlags[slot] != 0 ||
            !IntegerMathUtils.isWithinDistance(this.destinationX[slot], this.destinationY[slot], targetX, targetY, this.lookaheadDistance)) {
            return;
        }

        int last = this.waypointCounts[slot] - 1;
        int legX = this.waypointIndexes[slot] < last ? this.getWaypointX(slot, last - 1) : unit.getPositionX();
        int legY = this.waypointIndexes[slot] < last ? this.getWaypointY(slot, last - 1) : unit.getPositionY();

        if (attachedClearance.getClearance(attachedClearance.findCell(targetX, targetY)) == 0 ||
            !attachedClearance.isSegmentClear(legX, legY, targetX, targetY, 1, false)) {
            return;
        }

        this.waypointX[slot * WAYPOINT_CAPACITY + last] = targetX;
        this.waypointY[slot * WAYPOINT_CAPACITY + last] = targetY;
        this.destinationX[slot]                         = targetX;
        this.destinationY[slot]                         = targetY;
    }

    /**
     * Moves a unit's path on past the waypoints it reached: within the arrival distance of the current waypoint, or within the lookahead
     * distance of it when the unit can walk straight to the next one.
     *
     * @return the current waypoint afterwards
     */
    private int advance(@NotNull WalkClearance attachedClearance, int slot, int positionX, int positionY) {
        int index = this.waypointIndexes[slot];
        int last = this.waypointCounts[slot] - 1;

        while (index < last) {
            int currentX = this.getWaypointX(slot, index);
            int currentY = this.getWaypointY(slot, index);

            if (!IntegerMathUtils.isWithinDistance(positionX, positionY, currentX, currentY, this.arrivalDistance) &&
                !(IntegerMathUtils.isWithinDistance(positionX, positionY, currentX, currentY, this.lookaheadDistance) &&
                  attachedClearance.isSegmentClear(positionX, positionY,
                                                   this.getWaypointX(slot, index + 1), this.getWaypointY(slot, index + 1), 1, false))) {
                break;
            }

            index++;
        }

        this.waypointIndexes[slot] = index;

        return index;
    }

    /**
     * Returns the clearance a unit's size needs: {@code 1} for a unit within half a cell of its position, plus one for every further cell its
     * bounds reach, at most the largest clearance.
     */
    private int findRequirement(@NotNull WalkClearance attachedClearance, @NotNull Unit unit) {
        int positionX = unit.getPositionX();
        int positionY = unit.getPositionY();
        int extent = Math.max(Math.max(positionX - this.bounds.getLeft(unit), this.bounds.getRight(unit) - positionX),
                              Math.max(positionY - this.bounds.getTop(unit), this.bounds.getBottom(unit) - positionY));
        int cellSize = attachedClearance.getCellSize();
        int beyond = extent - cellSize / ARRIVAL_DIVISOR;
        int requirement = beyond > 0 ? 1 + (beyond + cellSize - 1) / cellSize : 1;

        return Math.min(requirement, WalkClearance.MAXIMUM_CLEARANCE);
    }

    /**
     * Reads the structures of the unit pool once per frame: adds the bounds of new and moved structures to the clearance, removes those of
     * structures that are gone, and recomputes the clearance after a change.
     */
    private void updateStructures() {
        FrameClock attachedClock = requireAttached(this.clock);

        if (this.structureFrame == attachedClock.getFrame()) {
            return;
        }

        WalkClearance attachedClearance = requireAttached(this.clearance);
        EntityPool<Unit> pool = requireAttached(this.units);

        this.structureFrame = attachedClock.getFrame();

        for (int slot = 1; slot < this.structurePlaced.length; slot++) {
            Unit unit = pool.isAllocated(slot) ? pool.get(slot) : null;
            boolean placed = unit != null && this.isStructure(unit);
            int left = placed ? this.bounds.getLeft(unit) : 0;
            int top = placed ? this.bounds.getTop(unit) : 0;
            int right = placed ? this.bounds.getRight(unit) : 0;
            int bottom = placed ? this.bounds.getBottom(unit) : 0;

            if (placed == this.structurePlaced[slot] && left == this.structureLeft[slot] && top == this.structureTop[slot] &&
                right == this.structureRight[slot] && bottom == this.structureBottom[slot]) {
                continue;
            }

            if (this.structurePlaced[slot]) {
                attachedClearance.removeStructure(this.structureLeft[slot], this.structureTop[slot],
                                                  this.structureRight[slot], this.structureBottom[slot]);
            }

            if (placed) {
                attachedClearance.addStructure(left, top, right, bottom);
            }

            this.structurePlaced[slot] = placed;
            this.structureLeft[slot]   = left;
            this.structureTop[slot]    = top;
            this.structureRight[slot]  = right;
            this.structureBottom[slot] = bottom;
        }

        attachedClearance.update();
    }

    private boolean isStructure(@NotNull Unit unit) {
        UnitType type = unit.getType();

        if (type == null || !this.bounds.isPlaced(unit)) {
            return false;
        }

        UnitTypeTable table = requireAttached(this.unitTypes);

        return (table.hasTrait(type, UnitTrait.BUILDING) || table.hasTrait(type, UnitTrait.RESOURCE_CONTAINER)) &&
               !table.hasTrait(type, UnitTrait.FLYER);
    }

    private int getWaypointX(int slot, int index) {
        return this.waypointX[slot * WAYPOINT_CAPACITY + index];
    }

    private int getWaypointY(int slot, int index) {
        return this.waypointY[slot * WAYPOINT_CAPACITY + index];
    }
}
