package org.fogofwarlabs.sc4j.launcher.nodes;

import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds the outcome of checking the node editor's fields: the entry they make, or every reason they make none.
 */
public final class NodeEntryCheck {
    private final @Nullable ConfiguredNode node;
    private final @NotNull List<@NotNull NodeEntryProblem> problems;

    /**
     * Creates a {@link NodeEntryCheck} instance.
     *
     * @param node     the entry the fields make, or {@code null} if there is a problem
     * @param problems the problems in the order of {@link NodeEntryProblem}, empty if the entry is valid
     */
    NodeEntryCheck(@Nullable ConfiguredNode node, @NotNull List<@NotNull NodeEntryProblem> problems) {
        this.node     = node;
        this.problems = List.copyOf(problems);
    }

    /**
     * @return the entry the fields make, or {@code null} if there is a problem
     */
    public @Nullable ConfiguredNode getNode() {
        return this.node;
    }

    /**
     * @return the problems in the order of {@link NodeEntryProblem}, empty if the entry is valid
     */
    public @NotNull List<@NotNull NodeEntryProblem> getProblems() {
        return this.problems;
    }
}
