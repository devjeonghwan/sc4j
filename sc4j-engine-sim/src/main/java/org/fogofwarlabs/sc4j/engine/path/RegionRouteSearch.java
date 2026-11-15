package org.fogofwarlabs.sc4j.engine.path;

import org.fogofwarlabs.sc4j.engine.math.IntegerMathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Searches routes between regions of a {@link RegionGraph}: an A* search from region center to region center with bounded, reusable storage. The
 * search serves a mod's own region based path finding, for movement routes and area distances.
 * <p>
 * A search between regions of different components searches backward from the destination and routes to the reachable region nearest to it.
 * The search stops at {@code 125} heap entries or {@code 350} nodes, and an exhausted search routes to the discovered
 * region nearest to the goal. Equal totals keep the binary heap's order. Coordinates are sixteen-bit map pixels, read as signed values.
 * <p>
 * Determinism contract: a search depends only on the graph, the excluded regions and its arguments, draws no random value and allocates
 * nothing.
 */
public final class RegionRouteSearch {
    /**
     * The route count of a direct route, which holds only the start and the end region.
     */
    public static final int DIRECT_ROUTE_COUNT = 2;

    /**
     * The node capacity; the search stops when it allocates the last node.
     */
    private static final int NODE_CAPACITY = 350;
    /**
     * The heap entry limit; the search stops when the heap holds this many entries.
     */
    private static final int HEAP_LIMIT = 125;
    /**
     * The number of route regions kept.
     */
    private static final int ROUTE_CAPACITY = 50;
    /**
     * The estimate and total of the start node.
     */
    private static final int START_ESTIMATE = 0x400000;
    /**
     * The heap index of the first entry; the heap occupies the indices from this one up to the heap count.
     */
    private static final int HEAP_ROOT = 1;
    /**
     * The branching factor of the binary heap: an entry's parent index is its index divided by it and its first child index its index times it.
     */
    private static final int HEAP_BRANCHING = 2;

    private final @NotNull RegionGraph regions;
    private final boolean @NotNull [] excludedRegions;

    private final int @NotNull [] regionNodes;
    private final int @NotNull [] nodeRegions;
    private final int @NotNull [] predecessors;
    private final int @NotNull [] estimates;
    private final int @NotNull [] costs;
    private final int @NotNull [] totals;
    private final boolean @NotNull [] closed;
    private int nodeCount;

    private final int @NotNull [] heap;
    private int heapCount;
    private int maximumHeapCount;

    private final int @NotNull [] route;
    private int routeCount;
    private int fullRouteCount;
    private boolean incomplete;

    /**
     * Creates a {@link RegionRouteSearch} instance with reusable search storage for a graph.
     *
     * @param regions the region graph
     */
    public RegionRouteSearch(@NotNull RegionGraph regions) {
        this.regions         = regions;
        this.excludedRegions = new boolean[regions.getRegionCapacity()];

        this.regionNodes  = new int[regions.getRegionCapacity()];
        this.nodeRegions  = new int[NODE_CAPACITY];
        this.predecessors = new int[NODE_CAPACITY];
        this.estimates    = new int[NODE_CAPACITY];
        this.costs        = new int[NODE_CAPACITY];
        this.totals       = new int[NODE_CAPACITY];
        this.closed       = new boolean[NODE_CAPACITY];
        this.nodeCount    = 0;

        this.heap             = new int[HEAP_LIMIT + 1];
        this.heapCount        = 0;
        this.maximumHeapCount = 0;

        this.route          = new int[ROUTE_CAPACITY];
        this.routeCount     = 0;
        this.fullRouteCount = 0;
        this.incomplete     = false;

        Arrays.fill(this.regionNodes, -1);
    }

    /**
     * Sets whether searches skip a region when traversing neighbor lists. The retained lists remain unchanged.
     *
     * @param region   the region index
     * @param excluded whether searches skip the region as a neighbor
     */
    public void setExcludedRegion(int region, boolean excluded) {
        this.excludedRegions[region] = excluded;
    }

    /**
     * @param index the zero-based position below {@link #getRouteCount()}
     * @return the region identifier at the requested route position
     * @throws IndexOutOfBoundsException if the position is outside the current route
     */
    public int getRouteRegion(int index) {
        return this.route[Objects.checkIndex(index, this.routeCount)];
    }

    /**
     * @return the number of route regions kept, the full route count clamped to the route capacity {@code 50}
     */
    public int getRouteCount() {
        return this.routeCount;
    }

    /**
     * @return the number of regions of the whole route
     */
    public int getFullRouteCount() {
        return this.fullRouteCount;
    }

    /**
     * @return the number of nodes the last search allocated
     */
    public int getNodeCount() {
        return this.nodeCount;
    }

    /**
     * @return the largest number of heap entries of the last search
     */
    public int getMaximumHeapCount() {
        return this.maximumHeapCount;
    }

    /**
     * @return {@code true} if exhausting the heap required the closest discovered region, {@code false} otherwise
     */
    public boolean isIncomplete() {
        return this.incomplete;
    }


    /**
     * Searches a route between the regions of two positions ({@link RegionGraph#findRegion}), including the backward search and the
     * reachable-component fallback.
     *
     * @param sourceHorizontal      the source x coordinate
     * @param sourceVertical        the source y coordinate
     * @param destinationHorizontal the destination x coordinate
     * @param destinationVertical   the destination y coordinate
     * @return {@code true} if the search produces a nonempty route, {@code false} otherwise
     */
    public boolean search(int sourceHorizontal, int sourceVertical, int destinationHorizontal, int destinationVertical) {
        return this.search(this.regions.findRegion(sourceHorizontal, sourceVertical), sourceHorizontal, sourceVertical,
                           this.regions.findRegion(destinationHorizontal, destinationVertical), destinationHorizontal, destinationVertical);
    }

    /**
     * Searches a route between explicit source and destination regions; the positions need not lie in the regions (a caller may pass region
     * centers as positions).
     *
     * @param source                the source region
     * @param sourceHorizontal      the source x coordinate
     * @param sourceVertical        the source y coordinate
     * @param destination           the destination region
     * @param destinationHorizontal the destination x coordinate
     * @param destinationVertical   the destination y coordinate
     * @return {@code true} if the search produces a nonempty route, {@code false} otherwise
     */
    public boolean search(int source, int sourceHorizontal, int sourceVertical, int destination, int destinationHorizontal, int destinationVertical) {
        if (source == destination) {
            return false;
        }

        this.incomplete = false;

        boolean reversed = this.regions.getComponent(source) != this.regions.getComponent(destination);
        int selected = this.findRouteNode(source, destination,
                                          sourceHorizontal, sourceVertical, destinationHorizontal, destinationVertical,
                                          reversed);

        if (reversed && this.nodeRegions[selected] != source) {
            destination = this.nodeRegions[selected];
            selected    = this.findRouteNode(source, destination,
                                             sourceHorizontal, sourceVertical, destinationHorizontal, destinationVertical,
                                             false);
            reversed    = false;
        }

        this.fullRouteCount = 0;
        this.routeCount     = 0;

        if (reversed) {
            int component = this.regions.getComponent(this.nodeRegions[selected]);

            for (int node = selected; node >= 0; node = this.predecessors[node]) {
                if (this.regions.getComponent(this.nodeRegions[node]) != component) {
                    break;
                }

                if (this.routeCount < ROUTE_CAPACITY) {
                    this.route[this.routeCount++] = this.nodeRegions[node];
                }

                this.fullRouteCount++;
            }
        } else {
            for (int node = selected; node >= 0; node = this.predecessors[node]) {
                this.fullRouteCount++;
            }

            this.routeCount = Math.min(ROUTE_CAPACITY, this.fullRouteCount);

            for (int skipped = this.fullRouteCount - this.routeCount; skipped > 0; skipped--) {
                selected = this.predecessors[selected];
            }

            for (int routeIndex = this.routeCount - 1; routeIndex >= 0; routeIndex--) {
                this.route[routeIndex] = this.nodeRegions[selected];
                selected               = this.predecessors[selected];
            }
        }

        return this.fullRouteCount > 0;
    }

    private int findRouteNode(int source, int destination,
                              int sourceHorizontal, int sourceVertical,
                              int destinationHorizontal, int destinationVertical,
                              boolean reversed) {
        int component = this.regions.getComponent(source);
        int start = reversed ? destination : source;
        int goal = reversed ? source : destination;

        for (int nodeIndex = 0; nodeIndex < this.nodeCount; nodeIndex++) {
            this.regionNodes[this.nodeRegions[nodeIndex]] = -1;
        }

        this.nodeCount          = 1;
        this.heapCount          = 0;
        this.maximumHeapCount   = 0;
        this.nodeRegions[0]     = start;
        this.regionNodes[start] = 0;
        this.predecessors[0]    = -1;
        this.estimates[0]       = START_ESTIMATE;
        this.costs[0]           = 0;
        this.totals[0]          = START_ESTIMATE;
        this.closed[0]          = false;
        this.raiseHeapNode(++this.heapCount, 0);

        int sourceHorizontalFixed = (short) sourceHorizontal << RegionGraph.FIXED_POINT_SHIFT;
        int sourceVerticalFixed = (short) sourceVertical << RegionGraph.FIXED_POINT_SHIFT;
        int destinationHorizontalFixed = (short) destinationHorizontal << RegionGraph.FIXED_POINT_SHIFT;
        int destinationVerticalFixed = (short) destinationVertical << RegionGraph.FIXED_POINT_SHIFT;
        int goalHorizontalFixed = reversed ? sourceHorizontalFixed : destinationHorizontalFixed;
        int goalVerticalFixed = reversed ? sourceVerticalFixed : destinationVerticalFixed;
        int selected = -1;

        while (this.heapCount != 0) {
            int current = this.removeFirstHeapNode();
            int currentRegion = this.nodeRegions[current];

            if (currentRegion == goal) {
                selected = current;

                break;
            }

            this.closed[current] = true;

            int neighborCount = this.regions.getComponent(currentRegion) == component
                                ? this.regions.getWalkableNeighborCount(currentRegion)
                                : this.regions.getNeighborCount(currentRegion);
            int currentHorizontal = currentRegion == source
                                    ? sourceHorizontalFixed
                                    : currentRegion == destination
                                      ? destinationHorizontalFixed
                                      : this.regions.getCenterHorizontalFixed(currentRegion);
            int currentVertical = currentRegion == source
                                  ? sourceVerticalFixed
                                  : currentRegion == destination
                                    ? destinationVerticalFixed
                                    : this.regions.getCenterVerticalFixed(currentRegion);

            for (int neighborIndex = 0; neighborIndex < neighborCount; neighborIndex++) {
                int neighbor = this.regions.getNeighborRegion(currentRegion, neighborIndex);

                if (this.excludedRegions[neighbor]) {
                    continue;
                }

                int node = this.regionNodes[neighbor];

                if (node >= 0 && this.predecessors[current] == node) {
                    continue;
                }

                int neighborHorizontal = neighbor == source
                                         ? sourceHorizontalFixed
                                         : neighbor == destination
                                           ? destinationHorizontalFixed
                                           : this.regions.getCenterHorizontalFixed(neighbor);
                int neighborVertical = neighbor == source
                                       ? sourceVerticalFixed
                                       : neighbor == destination
                                         ? destinationVerticalFixed
                                         : this.regions.getCenterVerticalFixed(neighbor);
                int edgeCost = IntegerMathUtils.getApproximateDistance(currentHorizontal, currentVertical, neighborHorizontal, neighborVertical);

                if (this.regions.getComponent(neighbor) != component) {
                    edgeCost += edgeCost;
                }

                int cost = this.costs[current] + edgeCost;

                if (node < 0) {
                    node                       = this.nodeCount++;
                    this.regionNodes[neighbor] = node;
                    this.nodeRegions[node]     = neighbor;
                    this.predecessors[node]    = current;
                    this.estimates[node]       = IntegerMathUtils.getApproximateDistance(neighborHorizontal, neighborVertical,
                                                                                         goalHorizontalFixed, goalVerticalFixed);
                    this.costs[node]           = cost;
                    this.totals[node]          = cost + this.estimates[node];
                    this.closed[node]          = false;
                    this.raiseHeapNode(++this.heapCount, node);
                } else if (cost < this.costs[node]) {
                    this.predecessors[node] = current;
                    this.costs[node]        = cost;

                    int previousTotal = this.totals[node];

                    this.totals[node] = cost + this.estimates[node];

                    if (this.closed[node]) {
                        this.closed[node] = false;
                        this.raiseHeapNode(++this.heapCount, node);
                    } else if (previousTotal != this.totals[node]) {
                        int heapIndex = this.heapCount;

                        while (heapIndex > 0 && this.heap[heapIndex] != node) {
                            heapIndex--;
                        }

                        if (heapIndex > 0) {
                            if (previousTotal > this.totals[node]) {
                                this.raiseHeapNode(heapIndex, node);
                            } else {
                                this.lowerHeapNode(heapIndex, node);
                            }
                        }
                    }
                }

                if (this.heapCount == HEAP_LIMIT || this.nodeCount == NODE_CAPACITY) {
                    break;
                }
            }

            this.maximumHeapCount = Math.max(this.maximumHeapCount, this.heapCount);

            if (this.heapCount == HEAP_LIMIT || this.nodeCount == NODE_CAPACITY) {
                if (this.heapCount != 0) {
                    selected = this.removeFirstHeapNode();
                }

                break;
            }
        }

        if (selected < 0) {
            this.incomplete   = true;
            selected          = 0;
            this.estimates[0] = IntegerMathUtils.getApproximateDistance(this.regions.getCenterHorizontalFixed(start),
                                                                        this.regions.getCenterVerticalFixed(start),
                                                                        goalHorizontalFixed, goalVerticalFixed);

            for (int nodeIndex = 1; nodeIndex < this.nodeCount; nodeIndex++) {
                if (this.estimates[nodeIndex] < this.estimates[selected]) {
                    selected = nodeIndex;
                }
            }
        }

        return selected;
    }

    private void raiseHeapNode(int heapIndex, int node) {
        while (heapIndex > HEAP_ROOT && this.totals[this.heap[heapIndex / HEAP_BRANCHING]] >= this.totals[node]) {
            this.heap[heapIndex] = this.heap[heapIndex / HEAP_BRANCHING];
            heapIndex /= HEAP_BRANCHING;
        }

        this.heap[heapIndex] = node;
    }

    private int removeFirstHeapNode() {
        int result = this.heap[HEAP_ROOT];
        int last = this.heap[this.heapCount--];

        this.lowerHeapNode(HEAP_ROOT, last);

        return result;
    }

    private void lowerHeapNode(int heapIndex, int node) {
        while (heapIndex <= this.heapCount / HEAP_BRANCHING) {
            int child = heapIndex * HEAP_BRANCHING;

            if (child < this.heapCount && this.totals[this.heap[child]] > this.totals[this.heap[child + 1]]) {
                child++;
            }

            if (this.totals[node] <= this.totals[this.heap[child]]) {
                break;
            }

            this.heap[heapIndex] = this.heap[child];
            heapIndex            = child;
        }

        this.heap[heapIndex] = node;
    }
}
