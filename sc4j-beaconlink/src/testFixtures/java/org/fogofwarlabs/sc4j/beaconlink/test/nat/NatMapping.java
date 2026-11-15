package org.fogofwarlabs.sc4j.beaconlink.test.nat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds one public mapping of an emulated NAT: the real loopback socket that stands for the public side, the inside endpoint it belongs to,
 * the destination of a symmetric mapping, the hosts and addresses the inside sent to through it, and when it was last used for sending.
 */
final class NatMapping {
    private final @NotNull DatagramChannel channel;
    private final @NotNull InetSocketAddress publicAddress;
    private final @NotNull NatEndpoint inside;
    private final @Nullable InetSocketAddress destination;

    private final @NotNull Set<@NotNull String> contactedHosts;
    private final @NotNull Set<@NotNull InetSocketAddress> contactedAddresses;
    private long lastSentMillis;

    /**
     * Creates a {@link NatMapping} instance.
     *
     * @param channel        the public socket, bound to 127.0.0.1 and not blocking
     * @param publicAddress  the public socket's address
     * @param inside         the inside endpoint
     * @param destination    the only destination of a symmetric mapping, or {@code null} for a mapping of every destination
     * @param lastSentMillis the clock time of the first datagram
     */
    NatMapping(@NotNull DatagramChannel channel, @NotNull InetSocketAddress publicAddress, @NotNull NatEndpoint inside,
               @Nullable InetSocketAddress destination, long lastSentMillis) {
        this.channel       = channel;
        this.publicAddress = publicAddress;
        this.inside        = inside;
        this.destination   = destination;

        this.contactedHosts     = new HashSet<>();
        this.contactedAddresses = new HashSet<>();
        this.lastSentMillis     = lastSentMillis;
    }

    /**
     * @return the public socket
     */
    @NotNull DatagramChannel getChannel() {
        return this.channel;
    }

    /**
     * @return the public address the mapping stands for
     */
    @NotNull InetSocketAddress getPublicAddress() {
        return this.publicAddress;
    }

    /**
     * @return the inside endpoint
     */
    @NotNull NatEndpoint getInside() {
        return this.inside;
    }

    /**
     * @return the clock time of the last datagram sent through the mapping
     */
    long getLastSentMillis() {
        return this.lastSentMillis;
    }


    /**
     * Checks whether the mapping serves a datagram of an inside endpoint to a destination.
     *
     * @param endpoint the inside endpoint
     * @param target   the destination
     * @return {@code true} if the mapping belongs to the endpoint and serves the destination, {@code false} otherwise
     */
    boolean serves(@NotNull NatEndpoint endpoint, @NotNull InetSocketAddress target) {
        return this.inside == endpoint && (this.destination == null || this.destination.equals(target));
    }

    /**
     * Records a datagram the inside sends through the mapping.
     *
     * @param target    the destination
     * @param host      the destination's host
     * @param nowMillis the clock time
     */
    void recordSent(@NotNull InetSocketAddress target, @NotNull String host, long nowMillis) {
        this.contactedHosts.add(host);
        this.contactedAddresses.add(target);
        this.lastSentMillis = nowMillis;
    }

    /**
     * Checks whether the NAT lets an incoming datagram through the mapping.
     *
     * @param type   the NAT behaviour
     * @param source the datagram's source
     * @param host   the source's host
     * @return {@code true} if the datagram passes the filter, {@code false} otherwise
     */
    boolean permits(@NotNull NatType type, @NotNull InetSocketAddress source, @NotNull String host) {
        if (type.isFilteringByAddress()) {
            return this.contactedAddresses.contains(source);
        }

        return !type.isFilteringByHost() || this.contactedHosts.contains(host);
    }

    /**
     * Closes the public socket.
     *
     * @throws IOException if the socket cannot be closed
     */
    void close() throws IOException {
        this.channel.close();
    }
}
