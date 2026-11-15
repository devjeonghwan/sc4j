package org.fogofwarlabs.sc4j.launcher.peer;

import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.BootstrapState;
import org.fogofwarlabs.sc4j.beaconlink.peer.reachability.Reachability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

/**
 * Holds a snapshot of the launcher's node for the Settings screen: its settings, its local address, the address the Internet sees, the
 * result of its reachability self-test (which decides whether it is a full peer), how far it joined the network, and its peer counts.
 * Instances are immutable; the node's thread creates them.
 */
public final class LauncherNodeStatus {
    private final @NotNull NodeSettings settings;
    private final @NotNull InetSocketAddress localAddress;
    private final @Nullable InetSocketAddress observedAddress;

    private final @NotNull Reachability reachability;
    private final @NotNull BootstrapState bootstrapState;
    private final int livePeerCount;
    private final int learnedPeerCount;

    /**
     * Creates a {@link LauncherNodeStatus} instance.
     *
     * @param settings         the settings the node runs with
     * @param localAddress     the address the node listens on
     * @param observedAddress  the node's address as another peer observed it, or {@code null} if not known yet
     * @param reachability     the result of the reachability self-test so far
     * @param bootstrapState   how far the node joined the network
     * @param livePeerCount    the number of live peers
     * @param learnedPeerCount the number of learned peers kept for later starts
     */
    public LauncherNodeStatus(@NotNull NodeSettings settings, @NotNull InetSocketAddress localAddress, @Nullable InetSocketAddress observedAddress,
                              @NotNull Reachability reachability, @NotNull BootstrapState bootstrapState, int livePeerCount, int learnedPeerCount) {
        this.settings        = settings;
        this.localAddress    = localAddress;
        this.observedAddress = observedAddress;

        this.reachability     = reachability;
        this.bootstrapState   = bootstrapState;
        this.livePeerCount    = livePeerCount;
        this.learnedPeerCount = learnedPeerCount;
    }

    /**
     * @return the settings the node runs with
     */
    public @NotNull NodeSettings getSettings() {
        return this.settings;
    }

    /**
     * @return the address the node listens on
     */
    public @NotNull InetSocketAddress getLocalAddress() {
        return this.localAddress;
    }

    /**
     * @return the node's address as another peer observed it, or {@code null} if not known yet
     */
    public @Nullable InetSocketAddress getObservedAddress() {
        return this.observedAddress;
    }

    /**
     * @return the result of the reachability self-test so far; {@link Reachability#isFullPeer()} tells whether the node is a full peer
     */
    public @NotNull Reachability getReachability() {
        return this.reachability;
    }

    /**
     * @return how far the node joined the network
     */
    public @NotNull BootstrapState getBootstrapState() {
        return this.bootstrapState;
    }

    /**
     * @return the number of live peers
     */
    public int getLivePeerCount() {
        return this.livePeerCount;
    }

    /**
     * @return the number of learned peers kept for later starts
     */
    public int getLearnedPeerCount() {
        return this.learnedPeerCount;
    }
}
