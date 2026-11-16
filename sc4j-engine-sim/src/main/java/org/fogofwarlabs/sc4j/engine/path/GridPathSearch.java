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
 * estimate is the larger of the straight distance to the goal's centre and the landmark bound, so the search finds a cheapest path. Of equal
 * totals the smaller estimate, then the lower cell index comes first; the binary heap holds each cell once.
 * <p>
 * Landmarks: on creation the search measures the step distances over the terrain without structures from up to {@value #LANDMARK_COUNT}
 * landmark cells, each the free cell farthest from the landmarks before it (the first one farthest from the first free cell). A path with
 * structures is never shorter than without them, so the difference of the distances of a cell and of the goal to a landmark is a lower bound of
 * the cell's way to the goal; around a long detour (a river crossed at a ford) it keeps the search near the way instead of filling the
 * terrain in front of the obstacle.
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
     * The number of landmark cells the estimate measures from at most.
     */
    static final int LANDMARK_COUNT = 4;

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
    /**
     * The landmark distance of a cell that no step from the landmark reaches.
     */
    private static final int UNREACHED = Integer.MAX_VALUE;

    private final @NotNull WalkClearance clearance;
    private final int neighbourCount;
    private final int @NotNull [] stepCosts;

    private final int @NotNull [] costs;
    private final int @NotNull [] estimates;
    private final int @NotNull [] parents;
    private final int @NotNull [] parentSteps;
    private final int @NotNull [] marks;
    private final boolean @NotNull [] closed;
    private final long @NotNull [] heapTotals;
    private final long @NotNull [] heapTies;
    private final int @NotNull [] heapPositions;
    private final int @NotNull [] pathCells;
    private final int @NotNull [] candidateX;
    private final int @NotNull [] candidateY;
    private final int @NotNull [] waypointX;
    private final int @NotNull [] waypointY;
    private final int @NotNull [] landmarkDistances;
    private final int landmarkCount;

    private long expandedCount;
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
        this.heapTotals    = new long[cellCount];
        this.heapTies      = new long[cellCount];
        this.heapPositions = new int[cellCount];
        this.pathCells     = new int[cellCount];
        this.candidateX    = new int[cellCount + 1];
        this.candidateY    = new int[cellCount + 1];
        this.waypointX     = new int[cellCount + 1];
        this.waypointY     = new int[cellCount + 1];

        this.landmarkDistances = new int[cellCount * LANDMARK_COUNT];

        this.expandedCount = 0;
        this.mark          = 0;
        this.heapSize      = 0;
        this.waypointCount = 0;

        for (int cell = 0; cell < cellCount; cell++) {
            for (int neighbour = 0; neighbour < neighbours; neighbour++) {
                int other = clearance.getNeighbour(cell, neighbour);

                this.stepCosts[cell * neighbours + neighbour] = other == WalkClearance.OUTSIDE ? 0 : this.measure(cell, other);
            }
        }

        this.landmarkCount = this.placeLandmarks();
    }

    /**
     * @return the number of cells the path searches took from the open set since creation, for measuring their cost
     */
    long getExpandedCount() {
        return this.expandedCount;
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
        this.open(startCell, searchMark, 0, NO_PARENT, NO_PARENT, this.estimate(startCell, goalCell, goalX, goalY));

        while (this.heapSize > 0) {
            int cell = this.poll();
            this.expandedCount++;

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
                    this.open(other, searchMark, cost, cell, neighbour, this.estimate(other, goalCell, goalX, goalY));
                } else if (cost < this.costs[other]) {
                    this.costs[other]       = cost;
                    this.parents[other]     = cell;
                    this.parentSteps[other] = neighbour;
                    this.lowerCost(other);
                }
            }
        }

        return false;
    }

    /**
     * Runs a search without goal from a cell over the current clearance and stores the step distance of every cell as a landmark's.
     */
    private void measureLandmark(int sourceCell, int landmark) {
        int searchMark = this.nextMark();

        for (int cell = 0; cell < this.marks.length; cell++) {
            this.landmarkDistances[cell * LANDMARK_COUNT + landmark] = UNREACHED;
        }

        this.heapSize = 0;
        this.open(sourceCell, searchMark, 0, NO_PARENT, NO_PARENT, 0);

        while (this.heapSize > 0) {
            int cell = this.poll();

            this.closed[cell]                                        = true;
            this.landmarkDistances[cell * LANDMARK_COUNT + landmark] = this.costs[cell];

            for (int neighbour = 0; neighbour < this.neighbourCount; neighbour++) {
                int other = this.clearance.getNeighbour(cell, neighbour);

                if (other == WalkClearance.OUTSIDE || this.marks[other] == searchMark && this.closed[other] || !this.clearance.canStep(cell, other)) {
                    continue;
                }

                int cost = this.costs[cell] + this.stepCosts[cell * this.neighbourCount + neighbour];

                if (this.marks[other] != searchMark) {
                    this.open(other, searchMark, cost, cell, neighbour, 0);
                } else if (cost < this.costs[other]) {
                    this.costs[other] = cost;
                    this.lowerCost(other);
                }
            }
        }
    }

    /**
     * Places the landmarks on the clearance without structures, as the class description says, and measures their distances.
     *
     * @return the number of landmarks placed
     */
    private int placeLandmarks() {
        int seed = 0;

        while (seed < this.marks.length && this.clearance.getClearance(seed) == 0) {
            seed++;
        }

        if (seed == this.marks.length) {
            return 0;
        }

        // The first landmark's distances hold those of the first free cell until the farthest cell from it replaces them.
        this.measureLandmark(seed, 0);

        int count = 0;

        while (count < LANDMARK_COUNT) {
            int farthest = WalkClearance.OUTSIDE;
            long farthestDistance = 0;

            for (int cell = 0; cell < this.marks.length; cell++) {
                if (this.clearance.getClearance(cell) == 0) {
                    continue;
                }

                long distance = this.findNearestLandmarkDistance(cell, Math.max(count, 1));

                if (distance > farthestDistance) {
                    farthest         = cell;
                    farthestDistance = distance;
                }
            }

            if (farthest == WalkClearance.OUTSIDE) {
                break;
            }

            this.measureLandmark(farthest, count);
            count++;
        }

        return count;
    }

    /**
     * Returns the step distance from a cell to the nearest of the first landmarks, {@link Long#MAX_VALUE} if none of them reaches it.
     */
    private long findNearestLandmarkDistance(int cell, int count) {
        long nearest = Long.MAX_VALUE;

        for (int landmark = 0; landmark < count; landmark++) {
            int distance = this.landmarkDistances[cell * LANDMARK_COUNT + landmark];

            if (distance != UNREACHED) {
                nearest = Math.min(nearest, distance);
            }
        }

        return nearest;
    }

    /**
     * Returns the estimate of a cell's way to the goal: the straight distance between the centres or the landmark bound, the larger one.
     */
    private int estimate(int cell, int goalCell, int goalX, int goalY) {
        int estimate = IntegerMathUtils.getDistance(this.clearance.getCenterX(cell) * COST_SCALE, this.clearance.getCenterY(cell) * COST_SCALE,
                                                    goalX * COST_SCALE, goalY * COST_SCALE);
        int cellBase = cell * LANDMARK_COUNT;
        int goalBase = goalCell * LANDMARK_COUNT;

        for (int landmark = 0; landmark < this.landmarkCount; landmark++) {
            int cellDistance = this.landmarkDistances[cellBase + landmark];
            int goalDistance = this.landmarkDistances[goalBase + landmark];

            if (cellDistance != UNREACHED && goalDistance != UNREACHED) {
                estimate = Math.max(estimate, Math.abs(goalDistance - cellDistance));
            }
        }

        return estimate;
    }

    private void open(int cell, int searchMark, int cost, int parent, int parentStep, int estimate) {
        this.marks[cell]       = searchMark;
        this.closed[cell]      = false;
        this.costs[cell]       = cost;
        this.estimates[cell]   = estimate;
        this.parents[cell]     = parent;
        this.parentSteps[cell] = parentStep;

        this.heapTotals[this.heapSize] = (long) cost + estimate;
        this.heapTies[this.heapSize]   = (long) estimate << Integer.SIZE | cell;
        this.heapPositions[cell]       = this.heapSize;
        this.heapSize++;
        this.siftUp(this.heapSize - 1);
    }

    /**
     * Moves a cell in the heap after its cost became lower.
     */
    private void lowerCost(int cell) {
        int position = this.heapPositions[cell];

        this.heapTotals[position] = (long) this.costs[cell] + this.estimates[cell];
        this.siftUp(position);
    }

    private int poll() {
        int first = (int) this.heapTies[0];

        this.heapSize--;
        this.heapPositions[first] = NOT_IN_HEAP;

        if (this.heapSize > 0) {
            this.heapTotals[0]                         = this.heapTotals[this.heapSize];
            this.heapTies[0]                           = this.heapTies[this.heapSize];
            this.heapPositions[(int) this.heapTies[0]] = 0;
            this.siftDown(0);
        }

        return first;
    }

    private void siftUp(int position) {
        long total = this.heapTotals[position];
        long tie = this.heapTies[position];
        int current = position;

        while (current > 0) {
            int parentPosition = (current - 1) / HEAP_BRANCHING;

            if (!isBefore(total, tie, this.heapTotals[parentPosition], this.heapTies[parentPosition])) {
                break;
            }

            this.moveEntry(parentPosition, current);
            current = parentPosition;
        }

        this.heapTotals[current]      = total;
        this.heapTies[current]        = tie;
        this.heapPositions[(int) tie] = current;
    }

    private void siftDown(int position) {
        long total = this.heapTotals[position];
        long tie = this.heapTies[position];
        int current = position;

        while (true) {
            int child = current * HEAP_BRANCHING + 1;

            if (child >= this.heapSize) {
                break;
            }

            if (child + 1 < this.heapSize &&
                isBefore(this.heapTotals[child + 1], this.heapTies[child + 1], this.heapTotals[child], this.heapTies[child])) {
                child++;
            }

            if (!isBefore(this.heapTotals[child], this.heapTies[child], total, tie)) {
                break;
            }

            this.moveEntry(child, current);
            current = child;
        }

        this.heapTotals[current]      = total;
        this.heapTies[current]        = tie;
        this.heapPositions[(int) tie] = current;
    }

    private void moveEntry(int from, int to) {
        this.heapTotals[to]                         = this.heapTotals[from];
        this.heapTies[to]                           = this.heapTies[from];
        this.heapPositions[(int) this.heapTies[to]] = to;
    }

    /**
     * Orders the heap: the smaller total first, then the smaller estimate, then the lower cell index; a tie holds the estimate in its high and the
     * cell in its low half.
     */
    private static boolean isBefore(long total, long tie, long otherTotal, long otherTie) {
        return total != otherTotal ? total < otherTotal : tie < otherTie;
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
