package org.fogofwarlabs.sc4j.beaconlink.test.nat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Names the emulated hosts of loopback sockets. Every socket on 127.0.0.1 shares one IP address, so the address-restricted filter compares
 * these host names instead: a socket belongs to the host it was assigned to, or is a host of its own.
 * <p>
 * It also names the private network of the inside sockets of each NAT: a datagram an inside endpoint sends to the inside address of another
 * NAT's endpoint goes nowhere, like a datagram to a private address of another local network.
 */
public final class LoopbackHosts {
    private final @NotNull Map<@NotNull InetSocketAddress, @NotNull String> hosts;
    private final @NotNull Map<@NotNull InetSocketAddress, @NotNull String> networks;

    /**
     * Creates a {@link LoopbackHosts} instance in which every socket is a host of its own and no socket is private.
     */
    public LoopbackHosts() {
        this.hosts    = new HashMap<>();
        this.networks = new HashMap<>();
    }

    /**
     * Assigns a socket to an emulated host.
     *
     * @param address the socket's address
     * @param host    the host name
     */
    public void assign(@NotNull InetSocketAddress address, @NotNull String host) {
        this.hosts.put(address, host);
    }

    /**
     * Assigns an inside socket to the private network of a NAT.
     *
     * @param address the socket's address
     * @param network the network name, the NAT's name
     */
    public void assignPrivate(@NotNull InetSocketAddress address, @NotNull String network) {
        this.networks.put(address, network);
    }

    /**
     * Forgets the host and the private network of a closed socket, so that a later socket on the same port is a public host of its own.
     *
     * @param address the socket's address
     */
    public void release(@NotNull InetSocketAddress address) {
        this.hosts.remove(address);
        this.networks.remove(address);
    }

    /**
     * Finds the host of a socket.
     *
     * @param address the socket's address
     * @return the assigned host name, or the address itself when it was not assigned
     */
    public @NotNull String find(@NotNull InetSocketAddress address) {
        String host = this.hosts.get(address);

        return host != null ? host : address.toString();
    }

    /**
     * Finds the private network of a socket.
     *
     * @param address the socket's address
     * @return the name of the NAT whose inside socket it is, or {@code null} for a public socket
     */
    public @Nullable String findNetwork(@NotNull InetSocketAddress address) {
        return this.networks.get(address);
    }
}
