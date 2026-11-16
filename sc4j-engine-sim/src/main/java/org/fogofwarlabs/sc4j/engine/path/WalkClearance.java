package org.fogofwarlabs.sc4j.engine.path;

import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Holds the clearance of the walk cells of a terrain for the {@link StandardPathfindingSystem}: which cells are blocked (unwalkable terrain or
 * the footprint of a structure), how many neighbour steps each free cell lies from the nearest blocked cell, and which free cells are connected.
 * It goes through the terrain's walk {@link GridTopology} only (cells, centres and neighbours), so it serves any grid layout.
 * <p>
 * The clearance of a blocked cell is {@code 0}; a free cell next to a blocked cell, or next to the edge of the map, has clearance {@code 1}, its
 * other neighbours {@code 2}, and so on up to {@value #MAXIMUM_CLEARANCE}. A step between two neighbouring free cells is allowed unless it cuts
 * through the corner where two blocked cells meet; the components join the free cells along allowed steps. Structures are counted per cell, so
 * overlapping footprints add and remove independently; the clearance and the components are recomputed in {@link #update()} after a change.
 * <p>
 * Determinism contract: the values depend only on the terrain and the structures added; every computation visits cells in index order or in
 * breadth-first order from them, draws no random value and allocates nothing after creation.
 */
final class WalkClearance {
    /**
     * The largest clearance stored.
     */
    static final int MAXIMUM_CLEARANCE = 15;
    /**
     * The cell index of a position outside the grid.
     */
    static final int OUTSIDE = -1;
    /**
     * The component of a blocked cell.
     */
    static final int NO_COMPONENT = 0;

    /**
     * The divisor of the smaller cell side that gives the distance between the samples of a segment test.
     */
    private static final int SAMPLES_PER_CELL = 2;

    private final @NotNull GridTopology cells;
    private final int columnCount;
    private final int cellCount;
    private final int neighbourCount;
    private final int sampleDistance;

    private final int @NotNull [] neighbourCells;
    private final int @NotNull [] centerX;
    private final int @NotNull [] centerY;
    private final boolean @NotNull [] terrainBlocked;
    private final int @NotNull [] structureCounts;
    private final byte @NotNull [] clearances;
    private final int @NotNull [] components;
    private final int @NotNull [] queue;
    private final int @NotNull [] marks;

    private int mark;
    private boolean changed;

    /**
     * Creates a {@link WalkClearance} instance over the walk cells of a terrain without structures.
     *
     * @param terrain the terrain
     */
    WalkClearance(@NotNull TerrainGrid terrain) {
        GridTopology walkCells = terrain.getWalkGrid();
        int count = walkCells.getColumnCount() * walkCells.getRowCount();

        this.cells          = walkCells;
        this.columnCount    = walkCells.getColumnCount();
        this.cellCount      = count;
        this.neighbourCount = walkCells.getNeighbourCount();
        this.sampleDistance = Math.max(1, Math.min(walkCells.getCellWidth(), walkCells.getCellHeight()) / SAMPLES_PER_CELL);

        this.neighbourCells  = new int[count * this.neighbourCount];
        this.centerX         = new int[count];
        this.centerY         = new int[count];
        this.terrainBlocked  = new boolean[count];
        this.structureCounts = new int[count];
        this.clearances      = new byte[count];
        this.components      = new int[count];
        this.queue           = new int[count];
        this.marks           = new int[count];

        this.mark    = 0;
        this.changed = true;

        for (int cell = 0; cell < count; cell++) {
            int column = this.getColumn(cell);
            int row = this.getRow(cell);

            for (int neighbour = 0; neighbour < this.neighbourCount; neighbour++) {
                int neighbourColumn = walkCells.getNeighbourColumn(column, row, neighbour);
                int neighbourRow = walkCells.getNeighbourRow(column, row, neighbour);

                this.neighbourCells[cell * this.neighbourCount + neighbour] = walkCells.contains(neighbourColumn, neighbourRow)
                                                                              ? neighbourRow * this.columnCount + neighbourColumn
                                                                              : OUTSIDE;
            }

            this.centerX[cell]        = walkCells.getCenterX(column, row);
            this.centerY[cell]        = walkCells.getCenterY(column, row);
            this.terrainBlocked[cell] = !terrain.isWalkable(column, row);
        }

        this.update();
    }

    /**
     * @return the number of walk cells
     */
    int getCellCount() {
        return this.cellCount;
    }

    /**
     * @return the number of neighbours of every cell, counting neighbours outside the grid
     */
    int getNeighbourCount() {
        return this.neighbourCount;
    }

    /**
     * @return the smaller side of a walk cell in world units
     */
    int getCellSize() {
        return Math.min(this.cells.getCellWidth(), this.cells.getCellHeight());
    }

    /**
     * Finds the cell that holds a world position.
     *
     * @param x the world x
     * @param y the world y
     * @return the cell index, or {@link #OUTSIDE} if the position lies outside the map
     */
    int findCell(int x, int y) {
        int column = this.cells.findColumn(x, y);
        int row = this.cells.findRow(x, y);

        return this.cells.contains(column, row) ? row * this.columnCount + column : OUTSIDE;
    }

    /**
     * @param cell the cell index
     * @return the world x of the cell's centre
     */
    int getCenterX(int cell) {
        return this.centerX[cell];
    }

    /**
     * @param cell the cell index
     * @return the world y of the cell's centre
     */
    int getCenterY(int cell) {
        return this.centerY[cell];
    }

    /**
     * Finds a neighbour of a cell.
     *
     * @param cell      the cell index
     * @param neighbour the neighbour, {@code 0} up to {@link #getNeighbourCount()} excluded
     * @return the neighbour's cell index, or {@link #OUTSIDE} if it lies outside the grid
     */
    int getNeighbour(int cell, int neighbour) {
        return this.neighbourCells[cell * this.neighbourCount + neighbour];
    }

    /**
     * @param cell the cell index, or {@link #OUTSIDE}
     * @return the clearance of the cell, {@code 0} for a blocked cell or a position outside the grid
     */
    int getClearance(int cell) {
        return cell != OUTSIDE ? this.clearances[cell] : 0;
    }

    /**
     * @param cell the cell index
     * @return {@code true} if the terrain of the cell is not walkable, {@code false} otherwise
     */
    boolean isTerrainBlocked(int cell) {
        return this.terrainBlocked[cell];
    }

    /**
     * @param cell the cell index, or {@link #OUTSIDE}
     * @return the component of the free cell, {@link #NO_COMPONENT} for a blocked cell or a position outside the grid
     */
    int getComponent(int cell) {
        return cell != OUTSIDE ? this.components[cell] : NO_COMPONENT;
    }


    /**
     * Adds the footprint of a structure: every cell that a point of the rectangle lies in is blocked while a structure covers it.
     *
     * @param left   the left edge in world units, inclusive
     * @param top    the top edge, inclusive
     * @param right  the right edge, inclusive
     * @param bottom the bottom edge, inclusive
     */
    void addStructure(int left, int top, int right, int bottom) {
        this.changeStructure(left, top, right, bottom, 1);
    }

    /**
     * Removes the footprint of a structure that was added with the same rectangle.
     *
     * @param left   the left edge in world units, inclusive
     * @param top    the top edge, inclusive
     * @param right  the right edge, inclusive
     * @param bottom the bottom edge, inclusive
     */
    void removeStructure(int left, int top, int right, int bottom) {
        this.changeStructure(left, top, right, bottom, -1);
    }

    /**
     * Recomputes the clearance and the components if a structure footprint changed which cells are blocked since the last update.
     */
    void update() {
        if (!this.changed) {
            return;
        }

        this.changed = false;

        this.updateClearances();
        this.updateComponents();
    }

    /**
     * Checks whether a unit may step from a cell to a neighbouring cell: the neighbour is free and, where either cell touches a blocked cell, the
     * straight line between their centres touches no blocked cell, not even at a corner.
     *
     * @param cell      the cell index
     * @param neighbour the neighbouring cell index, or {@link #OUTSIDE}
     * @return {@code true} if the step is allowed, {@code false} otherwise
     */
    boolean canStep(int cell, int neighbour) {
        int neighbourClearance = this.getClearance(neighbour);

        if (neighbourClearance == 0) {
            return false;
        }

        return neighbourClearance > 1 && this.getClearance(cell) > 1 ||
               this.isSegmentClear(this.getCenterX(cell), this.getCenterY(cell), this.getCenterX(neighbour), this.getCenterY(neighbour), 1, true);
    }

    /**
     * Checks whether a straight walk between two points keeps to cells of at least a clearance: the points along it, one sample distance (half
     * the smaller cell side) apart, after the start point; in the cells of the two end points a free cell is enough. A strict test also rejects
     * a point within one world unit of a blocked cell, so that a walk through the corner where two blocked cells meet is not clear.
     *
     * @param fromX       the world x of the start
     * @param fromY       the world y of the start
     * @param toX         the world x of the end
     * @param toY         the world y of the end
     * @param requirement the smallest clearance of the cells walked through, at least {@code 1}
     * @param strict      whether points within one world unit of a blocked cell fail too
     * @return {@code true} if every sampled point keeps to the clearance, {@code false} otherwise
     */
    boolean isSegmentClear(int fromX, int fromY, int toX, int toY, int requirement, boolean strict) {
        int changeX = toX - fromX;
        int changeY = toY - fromY;
        int distance = Math.max(Math.abs(changeX), Math.abs(changeY));
        int steps = (distance + this.sampleDistance - 1) / this.sampleDistance;
        int fromCell = this.findCell(fromX, fromY);
        int toCell = this.findCell(toX, toY);

        for (int step = 1; step <= steps; step++) {
            int x = fromX + (int) ((long) changeX * step / steps);
            int y = fromY + (int) ((long) changeY * step / steps);
            int cell = this.findCell(x, y);

            if (this.getClearance(cell) < (cell == fromCell || cell == toCell ? 1 : requirement)) {
                return false;
            }

            if (strict && (this.getClearance(this.findCell(x - 1, y - 1)) == 0 || this.getClearance(this.findCell(x + 1, y - 1)) == 0 ||
                           this.getClearance(this.findCell(x - 1, y + 1)) == 0 || this.getClearance(this.findCell(x + 1, y + 1)) == 0)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds the free cell nearest to a world position: the position's cell if it is free, else the first free cell of a breadth-first search over
     * the neighbours from the cell (from the nearest cell of the grid for a position outside it), in neighbour order.
     *
     * @param x     the world x
     * @param y     the world y
     * @param limit the largest number of cells the search visits
     * @return the free cell index, or {@link #OUTSIDE} if the search found none
     */
    int findNearestFree(int x, int y, int limit) {
        int column = Math.clamp(this.cells.findColumn(x, y), 0, this.columnCount - 1);
        int row = Math.clamp(this.cells.findRow(x, y), 0, this.cellCount / this.columnCount - 1);
        int start = row * this.columnCount + column;

        if (this.clearances[start] > 0) {
            return start;
        }

        int head = 0;
        int tail = 0;
        int searchMark = this.nextMark();

        this.queue[tail++] = start;
        this.marks[start]  = searchMark;

        while (head < tail && head < limit) {
            int cell = this.queue[head++];

            for (int neighbour = 0; neighbour < this.neighbourCount; neighbour++) {
                int next = this.getNeighbour(cell, neighbour);

                if (next == OUTSIDE || this.marks[next] == searchMark) {
                    continue;
                }

                if (this.clearances[next] > 0) {
                    return next;
                }

                this.marks[next]   = searchMark;
                this.queue[tail++] = next;
            }
        }

        return OUTSIDE;
    }

    private int getColumn(int cell) {
        return cell % this.columnCount;
    }

    private int getRow(int cell) {
        return cell / this.columnCount;
    }

    private int nextMark() {
        this.mark++;

        if (this.mark == Integer.MAX_VALUE) {
            Arrays.fill(this.marks, 0);
            this.mark = 1;
        }

        return this.mark;
    }

    /**
     * Adds or removes a structure on the cells that the points of its rectangle lie in, sampled half a cell apart and along its edges, each cell
     * once.
     */
    private void changeStructure(int left, int top, int right, int bottom, int change) {
        int footprintMark = this.nextMark();

        for (int y = top; ; y = Math.min(y + this.sampleDistance, bottom)) {
            for (int x = left; ; x = Math.min(x + this.sampleDistance, right)) {
                int cell = this.findCell(x, y);

                if (cell != OUTSIDE && this.marks[cell] != footprintMark) {
                    this.marks[cell] = footprintMark;
                    this.changeStructureCount(cell, change);
                }

                if (x >= right) {
                    break;
                }
            }

            if (y >= bottom) {
                break;
            }
        }
    }

    private void changeStructureCount(int cell, int change) {
        int before = this.structureCounts[cell];
        int after = before + change;

        this.structureCounts[cell] = after;

        if (before == 0 != (after == 0) && !this.terrainBlocked[cell]) {
            this.changed = true;
        }
    }

    /**
     * Computes the clearance of every cell breadth first: the blocked cells first, then the free cells at the edge of the map, then outward.
     */
    private void updateClearances() {
        int tail = 0;

        for (int cell = 0; cell < this.cellCount; cell++) {
            boolean blocked = this.terrainBlocked[cell] || this.structureCounts[cell] > 0;

            this.clearances[cell] = (byte) (blocked ? 0 : MAXIMUM_CLEARANCE);

            if (blocked) {
                this.queue[tail++] = cell;
            }
        }

        for (int cell = 0; cell < this.cellCount; cell++) {
            if (this.clearances[cell] > 1 && this.isAtEdge(cell)) {
                this.clearances[cell] = 1;
                this.queue[tail++]    = cell;
            }
        }

        for (int head = 0; head < tail; head++) {
            int cell = this.queue[head];
            int next = this.clearances[cell] + 1;

            if (next >= MAXIMUM_CLEARANCE) {
                continue;
            }

            for (int neighbour = 0; neighbour < this.neighbourCount; neighbour++) {
                int other = this.getNeighbour(cell, neighbour);

                if (other != OUTSIDE && this.clearances[other] > next) {
                    this.clearances[other] = (byte) next;
                    this.queue[tail++]     = other;
                }
            }
        }
    }

    private boolean isAtEdge(int cell) {
        for (int neighbour = 0; neighbour < this.neighbourCount; neighbour++) {
            if (this.getNeighbour(cell, neighbour) == OUTSIDE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Labels the components of the free cells in cell index order, each by a breadth-first search along the allowed steps.
     */
    private void updateComponents() {
        int component = NO_COMPONENT;

        Arrays.fill(this.components, NO_COMPONENT);

        for (int seed = 0; seed < this.cellCount; seed++) {
            if (this.clearances[seed] == 0 || this.components[seed] != NO_COMPONENT) {
                continue;
            }

            int head = 0;
            int tail = 0;

            component++;

            this.components[seed] = component;
            this.queue[tail++]    = seed;

            while (head < tail) {
                int cell = this.queue[head++];

                for (int neighbour = 0; neighbour < this.neighbourCount; neighbour++) {
                    int other = this.getNeighbour(cell, neighbour);

                    if (other != OUTSIDE && this.components[other] == NO_COMPONENT && this.canStep(cell, other)) {
                        this.components[other] = component;
                        this.queue[tail++]     = other;
                    }
                }
            }
        }
    }
}
