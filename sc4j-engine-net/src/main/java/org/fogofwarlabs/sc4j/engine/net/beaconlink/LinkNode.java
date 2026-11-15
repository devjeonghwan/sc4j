package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

/**
 * Describes one node of a Beacon.Link session: its address as listed, how the session stands with it, and the address it observed.
 */
public interface LinkNode {
    /**
     * @return the node's address as listed, {@code host:port} with an IPv6 host in brackets
     */
    @NotNull String getAddress();

    /**
     * @return how the session stands with the node
     */
    @NotNull LinkNodeState getState();

    /**
     * @return the source address the node last observed for the session, or {@code null} if it never answered
     */
    @Nullable InetSocketAddress getObservedAddress();
}
