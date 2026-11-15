package org.fogofwarlabs.sc4j.engine.path;

import org.fogofwarlabs.sc4j.engine.math.IntegerMathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Searches paths over the walk cells of a {@link WalkClearance} for the {@link StandardPathfindingSystem}: an A* search from cell to neighbouring
 * cell, then string pulling into a few straight legs.
 * <p>
 * A step costs the distance between the two cell centres; a step into a cell whose clearance is below the unit's requirement costs
 * {@value #NARROW_COST_FACTOR} times as much, so that units keep their size away from walls and structures but still pass a narrow gap. The
 * estimate is the straight distance to the goal's centre, so the search finds a cheapest path. Of equal totals the smaller estimate, then the
 * lower cell index comes first; the binary heap holds each cell once.
 * <p>
 * String pulling: the corner cells of the cell path (where its step direction changes) and the end point are the candidate waypoints; from the
 * start point each leg goes to the farthest candidate (at most {@value #PULL_WINDOW} ahead) that a straight walk reaches over cells of the
 * unit's requirement (free cells at the leg's two ends), else to the next candidate, along the searched cells.
 * <p>
 * Determinism contract: a search depends only on the clearance, its arguments and the cell order; it draws no random value and allocates
 * nothing after creation.
 */
final class GridPathSearch {
    /**
     * The factor of the cost of a step into a cell narrower than the unit's requirement.
     */
    static final int NARROW_COST_FACTOR = 4;
    /**
     * The number of candidate waypoints after a leg's start that string pulling tries.
     */
    static final int PULL_WINDOW = 32;

    /**
     * The cost of a world unit of distance, so that diagonal steps keep their fraction.
     */
    private static final int COST_SCALE = 16;
    /**
     * The parent of the start cell.
     */
    private static final int NO_PARENT = -1;
    /**
     * The heap position of a cell that is not in the heap.
     */
    private static final int NOT_IN_HEAP = -1;
    /**
     * The branching factor of the binary heap.
     */
    private static final int HEAP_BRANCHING = 2;

    private final @NotNull WalkClearance clearance;
    private final int neighbourCount;
    private final int @NotNull [] stepCosts;

    private final int @NotNull [] costs;
    private final int @NotNull [] estimates;
    private final int @NotNull [] parents;
    private final int @NotNull [] parentSteps;
    private final int @NotNull [] marks;
    private final boolean @NotNull [] closed;
    private final int @NotNull [] heap;
    private final int @NotNull [] heapPositions;
    private final int @NotNull [] pathCells;
    private final int @NotNull [] candidateX;
    private final int @NotNull [] candidateY;
    private final int @NotNull [] waypointX;
    private final int @NotNull [] waypointY;

    private int mark;
    private int heapSize;
    private int waypointCount;

    /**
     * Creates a {@link GridPathSearch} instance over the cells of a clearance, with the step cost of every cell's neighbours.
     *
     * @param clearance the clearance of the walk cells
     */
    GridPathSearch(@NotNull WalkClearance clearance) {
        int cellCount = clearance.getCellCount();
        int neighbours = clearance.getNeighbourCount();

        this.clearance      = clearance;
        this.neighbourCount = neighbours;
        this.stepCosts      = new int[cellCount * neighbours];

        this.costs         = new int[cellCount];
        this.estimates     = new int[cellCount];
        this.parents       = new int[cellCount];
        this.parentSteps   = new int[cellCount];
        this.marks         = new int[cellCount];
        this.closed        = new boolean[cellCount];
        this.heap          = new int[cellCount];
        this.heapPositions = new int[cellCount];
        this.pathCells     = new int[cellCount];
        this.candidateX    = new int[cellCount + 1];
        this.candidateY    = new int[cellCount + 1];
        this.waypointX     = new int[cellCount + 1];
        this.waypointY     = new int[cellCount + 1];

        this.mark          = 0;
        this.heapSize      = 0;
        this.waypointCount = 0;

        for (int cell = 0; cell < cellCount; cell++) {
            for (int neighbour = 0; neighbour < neighbours; neighbour++) {
                int other = clearance.getNeighbour(cell, neighbour);

                this.stepCosts[cell * neighbours + neighbour] = other == WalkClearance.OUTSIDE ? 0 : this.measure(cell, other);
            }
        }
    }

    /**
     * @return the number of waypoints of the last found path
     */
    int getWaypointCount() {
        return this.waypointCount;
    }

    /**
     * @param index the waypoint, {@code 0} up to {@link #getWaypointCount()} excluded
     * @return the world x of the waypoint
     */
    int getWaypointX(int index) {
        return this.waypointX[index];
    }

    /**
     * @param index the waypoint, {@code 0} up to {@link #getWaypointCount()} excluded
     * @return the world y of the waypoint
     */
    int getWaypointY(int index) {
        return this.waypointY[index];
    }


    /**
     * Searches the path of a unit from its position to an end point and pulls it into waypoints: the legs' end points, the last being the end
     * point.
     *
     * @param startX      the world x of the unit
     * @param startY      the world y of the unit
     * @param startCell   the free cell the search starts at, the unit's cell or the free cell nearest to it
     * @param goalCell    the free cell the search ends at, in the start cell's component
     * @param endX        the world x of the path's end point, in the goal cell or beyond it
     * @param endY        the world y of the path's end point
     * @param requirement the clearance the unit's size needs, at least {@code 1}
     * @return {@code true} if a path was found, {@code false} otherwise
     */
    boolean search(int startX, int startY, int startCell, int goalCell, int endX, int endY, int requirement) {
        this.waypointCount = 0;

        if (!this.searchCells(startCell, goalCell, requirement)) {
            return false;
        }

        int pathLength = 0;

        for (int cell = goalCell; cell != NO_PARENT; cell = this.parents[cell]) {
            this.pathCells[pathLength++] = cell;
        }

        int candidateCount = this.collectCandidates(pathLength, endX, endY);

        this.pull(startX, startY, candidateCount, requirement);

        return true;
    }

    /**
     * Runs the A* search between two cells; the parents of the reached cells lead back from the goal to the start.
     */
    private boolean searchCells(int startCell, int goalCell, int requirement) {
        int searchMark = this.nextMark();
        int goalX = this.clearance.getCenterX(goalCell);
        int goalY = this.clearance.getCenterY(goalCell);

        this.heapSize = 0;
        this.open(startCell, searchMark, 0, NO_PARENT, NO_PARENT, goalX, goalY);

        while (this.heapSize > 0) {
            int cell = this.poll();

            if (cell == goalCell) {
                return true;
            }

            this.closed[cell] = true;

            for (int neighbour = 0; neighbour < this.neighbourCount; neighbour++) {
                int other = this.clearance.getNeighbour(cell, neighbour);

                if (other == WalkClearance.OUTSIDE || this.marks[other] == searchMark && this.closed[other] || !this.clearance.canStep(cell, other)) {
                    continue;
                }

                int stepCost = this.stepCosts[cell * this.neighbourCount + neighbour];

                if (this.clearance.getClearance(other) < requirement) {
                    stepCost *= NARROW_COST_FACTOR;
                }

                int cost = this.costs[cell] + stepCost;

                if (this.marks[other] != searchMark) {
                    this.open(other, searchMark, cost, cell, neighbour, goalX, goalY);
                } else if (cost < this.costs[other]) {
                    this.costs[other]       = cost;
                    this.parents[other]     = cell;
                    this.parentSteps[other] = neighbour;
                    this.siftUp(this.heapPositions[other]);
                }
            }
        }

        return false;
    }

    private void open(int cell, int searchMark, int cost, int parent, int parentStep, int goalX, int goalY) {
        int centerX = this.clearance.getCenterX(cell);
        int centerY = this.clearance.getCenterY(cell);

        this.marks[cell]       = searchMark;
        this.closed[cell]      = false;
        this.costs[cell]       = cost;
        this.estimates[cell]   = IntegerMathUtils.getDistance(centerX * COST_SCALE, centerY * COST_SCALE, goalX * COST_SCALE, goalY * COST_SCALE);
        this.parents[cell]     = parent;
        this.parentSteps[cell] = parentStep;

        this.heap[this.heapSize] = cell;
        this.heapPositions[cell] = this.heapSize;
        this.heapSize++;
        this.siftUp(this.heapSize - 1);
    }

    private int poll() {
        int first = this.heap[0];

        this.heapSize--;
        this.heapPositions[first] = NOT_IN_HEAP;

        if (this.heapSize > 0) {
            int last = this.heap[this.heapSize];

            this.heap[0]             = last;
            this.heapPositions[last] = 0;
            this.siftDown(0);
        }

        return first;
    }

    private void siftUp(int position) {
        int cell = this.heap[position];
        int current = position;

        while (current > 0) {
            int parentPosition = (current - 1) / HEAP_BRANCHING;
            int parent = this.heap[parentPosition];

            if (!this.isBefore(cell, parent)) {
                break;
            }

            this.heap[current]         = parent;
            this.heapPositions[parent] = current;
                                         current = parentPosition;
        }

        this.heap[current]       = cell;
        this.heapPositions[cell] = current;
    }

    private void siftDown(int position) {
        int cell = this.heap[position];
        int current = position;

        while (true) {
            int child = current * HEAP_BRANCHING + 1;

            if (child >= this.heapSize) {
                break;
            }

            if (child + 1 < this.heapSize && this.isBefore(this.heap[child + 1], this.heap[child])) {
                child++;
            }

            int childCell = this.heap[child];

            if (!this.isBefore(childCell, cell)) {
                break;
            }

            this.heap[current]            = childCell;
            this.heapPositions[childCell] = current;
                                            current = child;
        }

        this.heap[current]       = cell;
        this.heapPositions[cell] = current;
    }

    /**
     * Orders the heap: the smaller total first, then the smaller estimate, then the lower cell index.
     */
    private boolean isBefore(int cell, int other) {
        long total = (long) this.costs[cell] + this.estimates[cell];
        long otherTotal = (long) this.costs[other] + this.estimates[other];

        if (total != otherTotal) {
            return total < otherTotal;
        }

        if (this.estimates[cell] != this.estimates[other]) {
            return this.estimates[cell] < this.estimates[other];
        }

        return cell < other;
    }

    /**
     * Collects the candidate waypoints from the cell path (goal first in {@link #pathCells}): the centres of the cells after which the step
     * direction changes, in walking order, then the end point.
     *
     * @return the number of candidates
     */
    private int collectCandidates(int pathLength, int endX, int endY) {
        int count = 0;

        // The path runs from the start cell at index pathLength - 1 to the goal cell at index 0.
        for (int index = pathLength - 2; index >= 1; index--) {
            int cell = this.pathCells[index];

            if (this.parentSteps[cell] != this.parentSteps[this.pathCells[index - 1]]) {
                this.candidateX[count] = this.clearance.getCenterX(cell);
                this.candidateY[count] = this.clearance.getCenterY(cell);
                count++;
            }
        }

        this.candidateX[count] = endX;
        this.candidateY[count] = endY;
        count++;

        return count;
    }

    /**
     * Pulls the candidates into waypoints, as the class description says.
     */
    private void pull(int startX, int startY, int candidateCount, int requirement) {
        int anchorX = startX;
        int anchorY = startY;
        int next = 0;

        while (next < candidateCount) {
            int chosen = next;
            int farthest = Math.min(candidateCount - 1, next + PULL_WINDOW);

            for (int candidate = farthest; candidate > next; candidate--) {
                if (this.clearance.isSegmentClear(anchorX, anchorY, this.candidateX[candidate], this.candidateY[candidate], requirement, true)) {
                    chosen = candidate;

                    break;
                }
            }

            this.waypointX[this.waypointCount] = this.candidateX[chosen];
            this.waypointY[this.waypointCount] = this.candidateY[chosen];
            this.waypointCount++;

            anchorX = this.candidateX[chosen];
            anchorY = this.candidateY[chosen];
            next    = chosen + 1;
        }
    }

    private int measure(int cell, int other) {
        return IntegerMathUtils.getDistance(this.clearance.getCenterX(cell) * COST_SCALE, this.clearance.getCenterY(cell) * COST_SCALE,
                                            this.clearance.getCenterX(other) * COST_SCALE, this.clearance.getCenterY(other) * COST_SCALE);
    }

    private int nextMark() {
        this.mark++;

        if (this.mark == Integer.MAX_VALUE) {
            Arrays.fill(this.marks, 0);
            this.mark = 1;
        }

        return this.mark;
    }
}
