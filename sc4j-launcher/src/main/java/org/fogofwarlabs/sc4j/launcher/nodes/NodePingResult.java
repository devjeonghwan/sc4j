package org.fogofwarlabs.sc4j.launcher.nodes;

import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

/**
 * Holds the outcome of pinging one node: the status and, for an answer ({@link NodePingStatus#REACHABLE}), the round-trip delay, the
 * address the node observed for the ping and the node software version.
 */
public final class NodePingResult {
    /**
     * The round-trip delay of a node that did not answer.
     */
    public static final long NO_ROUND_TRIP = -1L;

    private final @NotNull ConfiguredNode node;
    private final @NotNull NodePingStatus status;

    private final long roundTripMillis;
    private final @Nullable InetSocketAddress observedAddress;
    private final @Nullable String software;

    /**
     * Creates a {@link NodePingResult} instance for a node that did not answer.
     *
     * @param node   the pinged node
     * @param status the outcome, not an answer
     */
    NodePingResult(@NotNull ConfiguredNode node, @NotNull NodePingStatus status) {
        this(node, status, NO_ROUND_TRIP, null, null);
    }

    /**
     * Creates a {@link NodePingResult} instance.
     *
     * @param node            the pinged node
     * @param status          the outcome
     * @param roundTripMillis the delay from the answered ping to its answer in milliseconds, or {@link #NO_ROUND_TRIP}
     * @param observedAddress the address the node observed for the ping, or {@code null} without an answer
     * @param software        the node software version, or {@code null} without an answer
     */
    NodePingResult(@NotNull ConfiguredNode node, @NotNull NodePingStatus status,
                   long roundTripMillis, @Nullable InetSocketAddress observedAddress, @Nullable String software) {
        this.node   = node;
        this.status = status;

        this.roundTripMillis = roundTripMillis;
        this.observedAddress = observedAddress;
        this.software        = software;
    }

    /**
     * @return the pinged node
     */
    public @NotNull ConfiguredNode getNode() {
        return this.node;
    }

    /**
     * @return the outcome
     */
    public @NotNull NodePingStatus getStatus() {
        return this.status;
    }

    /**
     * @return the delay from the answered ping to its answer in milliseconds, or {@link #NO_ROUND_TRIP} without an answer
     */
    public long getRoundTripMillis() {
        return this.roundTripMillis;
    }

    /**
     * @return the address the node observed for the ping, or {@code null} without an answer
     */
    public @Nullable InetSocketAddress getObservedAddress() {
        return this.observedAddress;
    }

    /**
     * @return the node software version, or {@code null} without an answer
     */
    public @Nullable String getSoftware() {
        return this.software;
    }
}
