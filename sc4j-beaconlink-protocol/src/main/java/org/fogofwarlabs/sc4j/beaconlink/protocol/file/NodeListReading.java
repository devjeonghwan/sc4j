package org.fogofwarlabs.sc4j.beaconlink.protocol.file;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds the outcome of reading a node list file: the nodes in list order, or the problem that made the whole file unusable (then no node).
 */
public final class NodeListReading {
    private final @NotNull List<@NotNull ConfiguredNode> nodes;
    private final @Nullable NodeListProblem problem;

    /**
     * Creates a {@link NodeListReading} instance.
     *
     * @param nodes   the nodes in list order, empty with a problem
     * @param problem why the file could not be read, or {@code null}
     */
    NodeListReading(@NotNull List<@NotNull ConfiguredNode> nodes, @Nullable NodeListProblem problem) {
        this.nodes   = List.copyOf(nodes);
        this.problem = problem;
    }

    /**
     * @return the nodes in list order; empty if the file does not exist or could not be read
     */
    public @NotNull List<@NotNull ConfiguredNode> getNodes() {
        return this.nodes;
    }

    /**
     * @return why the file could not be read, or {@code null} if it was read or does not exist
     */
    public @Nullable NodeListProblem getProblem() {
        return this.problem;
    }
}
