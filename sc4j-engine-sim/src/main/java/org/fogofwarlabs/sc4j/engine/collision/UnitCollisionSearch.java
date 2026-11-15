package org.fogofwarlabs.sc4j.engine.collision;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.spatial.SortedEdgeIndex;
import org.fogofwarlabs.sc4j.engine.spatial.UnitBounds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the units that block a unit, over the sorted edges of the engine spatial index ({@link SortedEdgeIndex}): the first unit a step runs
 * into, the largest unit that overlaps a rectangle, and the first unit that blocks a placement. Which units block is the caller's
 * {@link BlockingTest}; the default blocking rules of a game and their hooks ({@link CollisionSystem#BLOCKING},
 * {@link CollisionSystem#PLACEMENT_EXEMPTION}) are such tests.
 * <p>
 * Coordinates are signed 16-bit pixel values, as in the index. Determinism contract: candidates are visited in edge order, the searches draw no
 * random value and allocate nothing; the rectangle searches collect into the shared results of the index, so a test does not start another
 * rectangle query of the same index.
 *
 * @param <U> the unit class of the indexed pool
 */
public final class UnitCollisionSearch<U extends Unit> {
    /**
     * The number of axes of a step: horizontal, then vertical.
     */
    private static final int AXIS_COUNT = 2;
    /**
     * The axis index of the horizontal axis.
     */
    private static final int HORIZONTAL_AXIS_INDEX = 0;

    private final @NotNull SortedEdgeIndex<U> index;
    private final @NotNull UnitBounds<? super U> bounds;

    /**
     * Creates a {@link UnitCollisionSearch} instance over an index.
     *
     * @param index  the sorted-edge index of the units
     * @param bounds reads the bounds of a unit, the bounds the index reads
     */
    public UnitCollisionSearch(@NotNull SortedEdgeIndex<U> index, @NotNull UnitBounds<? super U> bounds) {
        this.index  = index;
        this.bounds = bounds;
    }

    /**
     * Finds the first unit a step of an indexed unit runs into. For each axis with a displacement, horizontal first, the search walks the edges
     * from the unit's leading edge (from the first edge at its coordinate) in the step direction while they lie within the step, and returns the
     * first other unit whose cross-axis edges meet the unit's displaced cross-axis edges and that the test accepts.
     *
     * @param unit             the indexed moving unit
     * @param horizontalChange the horizontal displacement of the step, {@code 0} for none
     * @param verticalChange   the vertical displacement of the step, {@code 0} for none
     * @param test             decides whether a candidate blocks the unit
     * @return the first blocking unit, or {@code null}
     * @throws IndexOutOfBoundsException if the unit is not indexed
     */
    public @Nullable U findStepBlocker(@NotNull U unit, int horizontalChange, int verticalChange, @NotNull BlockingTest<? super U> test) {
        SortedEdgeIndex<U> edges = this.index;

        for (int axis = 0; axis < AXIS_COUNT; axis++) {
            boolean horizontal = axis == HORIZONTAL_AXIS_INDEX;
            int delta = horizontal ? horizontalChange : verticalChange;

            if (delta == 0) {
                continue;
            }

            boolean forward = delta > 0;
            int edgeIndex = forward ? unit.getMaximumEdgeIndex(horizontal) : unit.getMinimumEdgeIndex(horizontal);
            int coordinate = (short) edges.getCoordinate(horizontal, edgeIndex);
            int limit = (short) (coordinate + delta);
            int crossDelta = horizontal ? verticalChange : horizontalChange;
            int first = (short) (edges.getCoordinate(!horizontal, unit.getMinimumEdgeIndex(!horizontal)) + crossDelta);
            int last = (short) (edges.getCoordinate(!horizontal, unit.getMaximumEdgeIndex(!horizontal)) + crossDelta);

            if (forward) {
                while (edgeIndex > 0 && edges.getCoordinate(horizontal, edgeIndex - 1) == coordinate) {
                    edgeIndex--;
                }
            } else {
                while (edgeIndex + 1 < edges.getCount() && edges.getCoordinate(horizontal, edgeIndex + 1) == coordinate) {
                    edgeIndex++;
                }
            }

            for (; edgeIndex >= 0 && edgeIndex < edges.getCount(); edgeIndex += forward ? 1 : -1) {
                int candidateCoordinate = edges.getCoordinate(horizontal, edgeIndex);

                if (forward ? candidateCoordinate > limit : candidateCoordinate < limit) {
                    break;
                }

                U candidate = edges.getUnit(edges.getUnitIndex(horizontal, edgeIndex));

                if (candidate == unit) {
                    continue;
                }

                if (edges.getCoordinate(!horizontal, candidate.getMaximumEdgeIndex(!horizontal)) < first ||
                    edges.getCoordinate(!horizontal, candidate.getMinimumEdgeIndex(!horizontal)) > last) {
                    continue;
                }

                if (test.blocks(unit, candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    /**
     * Finds the largest unit that blocks a unit inside a rectangle: among the candidates of the expanded rectangle query
     * ({@link SortedEdgeIndex#findCandidates(int, int, int, int)}) that the test accepts and whose stored edges overlap the rectangle, the first
     * with the largest area ({@link UnitBounds#getWidth} times {@link UnitBounds#getHeight}).
     *
     * @param unit   the unit that would stand in the rectangle
     * @param left   the left edge of the rectangle
     * @param top    the top edge of the rectangle
     * @param right  the right edge of the rectangle, inclusive
     * @param bottom the bottom edge of the rectangle, inclusive
     * @param test   decides whether a candidate blocks the unit; it runs before the overlap test
     * @return the first largest blocking unit, or {@code null}
     * @throws IllegalStateException if the minimum query dimensions of the index are not set
     */
    public @Nullable U findLargestBlocker(@NotNull U unit, int left, int top, int right, int bottom, @NotNull BlockingTest<? super U> test) {
        SortedEdgeIndex<U> edges = this.index;
        int resultCount = edges.findCandidates(left, top, right, bottom);
        U largest = null;
        int largestArea = 0;

        left   = (short) left;
        top    = (short) top;
        right  = (short) right;
        bottom = (short) bottom;

        for (int resultIndex = 0; resultIndex < resultCount; resultIndex++) {
            U candidate = edges.getUnit(edges.getQueryResult(resultIndex));

            if (!test.blocks(unit, candidate)) {
                continue;
            }

            if (edges.getStoredCoordinate(true, candidate.getMaximumHorizontalEdgeIndex()) < left ||
                edges.getStoredCoordinate(true, candidate.getMinimumHorizontalEdgeIndex()) > right ||
                edges.getStoredCoordinate(false, candidate.getMaximumVerticalEdgeIndex()) < top ||
                edges.getStoredCoordinate(false, candidate.getMinimumVerticalEdgeIndex()) > bottom) {
                continue;
            }

            int area = this.bounds.getWidth(candidate) * this.bounds.getHeight(candidate);

            if (area > largestArea) {
                largestArea = area;
                largest     = candidate;
            }
        }

        return largest;
    }

    /**
     * Finds the first unit that blocks a unit inside a rectangle: the first candidate of the expanded rectangle query
     * ({@link SortedEdgeIndex#findCandidates(int, int, int, int)}) that the test accepts.
     *
     * @param unit   the unit to put down in the rectangle
     * @param left   the left edge of the rectangle
     * @param top    the top edge of the rectangle
     * @param right  the right edge of the rectangle, inclusive
     * @param bottom the bottom edge of the rectangle, inclusive
     * @param test   decides whether a candidate blocks the unit
     * @return the first blocking unit, or {@code null}
     * @throws IllegalStateException if the minimum query dimensions of the index are not set
     */
    public @Nullable U findFirstBlocker(@NotNull U unit, int left, int top, int right, int bottom, @NotNull BlockingTest<? super U> test) {
        SortedEdgeIndex<U> edges = this.index;
        int resultCount = edges.findCandidates(left, top, right, bottom);

        for (int resultIndex = 0; resultIndex < resultCount; resultIndex++) {
            U candidate = edges.getUnit(edges.getQueryResult(resultIndex));

            if (test.blocks(unit, candidate)) {
                return candidate;
            }
        }

        return null;
    }
}
