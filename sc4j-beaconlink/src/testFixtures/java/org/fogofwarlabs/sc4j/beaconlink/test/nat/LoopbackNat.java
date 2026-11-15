package org.fogofwarlabs.sc4j.beaconlink.test.nat;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Emulates one NAT on 127.0.0.1. Each public mapping is a real loopback UDP socket: a datagram an inside endpoint sends leaves from the
 * mapping's socket, so its receiver observes the mapping's address, and a datagram that reaches a mapping's socket is passed to the inside
 * endpoint only if the {@link NatType} filter lets it through. Mappings close after an idle time on the protocol clock (counted from the last
 * datagram sent through them); loss and delay can be injected for both directions.
 * <p>
 * Inside endpoints of one NAT form a private network: a datagram to the inside address of an endpoint of the same NAT goes directly from inside
 * socket to inside socket, and a datagram to the inside address of another NAT's endpoint is dropped.
 * <p>
 * The emulator is driven by its endpoints in the test thread: every {@link NatEndpoint#receive} reads the public sockets first. Every socket
 * it opens binds 127.0.0.1.
 */
public final class LoopbackNat implements Closeable {
    /**
     * The idle time after which a mapping closes, in milliseconds.
     */
    public static final long IDLE_MILLIS = 30_000L;

    private static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    private static final int BUFFER_LENGTH = 65_536;

    private static @NotNull DatagramChannel openLoopbackChannel() throws IOException {
        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);

        try {
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(LOOPBACK, 0));
        } catch (IOException exception) {
            channel.close();

            throw exception;
        }

        return channel;
    }


    private final @NotNull NatType type;
    private final @NotNull String name;
    private final @NotNull LoopbackHosts hosts;
    private final @NotNull ProtocolClock clock;
    private final @NotNull RandomGenerator random;

    private final @NotNull List<@NotNull NatEndpoint> endpoints;
    private final @NotNull List<@NotNull NatMapping> mappings;
    private final @NotNull ByteBuffer buffer;

    private double lossRate;
    private long delayMillis;
    private int filteredCount;
    private int lostCount;

    /**
     * Creates a {@link LoopbackNat} instance without endpoints, loss or delay.
     *
     * @param type   the NAT behaviour
     * @param name   the emulated host name of the NAT's public side
     * @param hosts  the host names of the loopback sockets, shared by the NATs and outside sockets of a test
     * @param clock  the clock of the idle times and delays
     * @param random the source of the injected loss
     */
    public LoopbackNat(@NotNull NatType type, @NotNull String name, @NotNull LoopbackHosts hosts, @NotNull ProtocolClock clock,
                       @NotNull RandomGenerator random) {
        this.type   = type;
        this.name   = name;
        this.hosts  = hosts;
        this.clock  = clock;
        this.random = random;

        this.endpoints = new ArrayList<>();
        this.mappings  = new ArrayList<>();
        this.buffer    = ByteBuffer.allocate(BUFFER_LENGTH);

        this.lossRate      = 0.0;
        this.delayMillis   = 0L;
        this.filteredCount = 0;
        this.lostCount     = 0;
    }

    /**
     * @return the NAT behaviour
     */
    public @NotNull NatType getType() {
        return this.type;
    }

    /**
     * @return the emulated host name of the NAT's public side
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * Sets the probability that a datagram is lost, in either direction.
     *
     * @param lossRate the probability, {@code 0} to {@code 1}
     * @throws IllegalArgumentException if the probability is outside {@code 0} to {@code 1}
     */
    public void setLossRate(double lossRate) {
        if (!(lossRate >= 0.0 && lossRate <= 1.0)) {
            throw Exceptions.newIllegalArgumentException("The loss rate {0} is outside 0..1.", lossRate);
        }

        this.lossRate = lossRate;
    }

    /**
     * Sets the delay of datagrams that pass the NAT inbound, on the protocol clock.
     *
     * @param delayMillis the delay in milliseconds, {@code 0} for none
     * @throws IllegalArgumentException if the delay is negative
     */
    public void setDelayMillis(long delayMillis) {
        if (delayMillis < 0L) {
            throw Exceptions.newIllegalArgumentException("The delay {0} is negative.", delayMillis);
        }

        this.delayMillis = delayMillis;
    }

    /**
     * @return the public addresses of the open mappings, in the order they were opened
     */
    public @NotNull List<@NotNull InetSocketAddress> getPublicAddresses() {
        List<InetSocketAddress> addresses = new ArrayList<>(this.mappings.size());

        for (NatMapping mapping : this.mappings) {
            addresses.add(mapping.getPublicAddress());
        }

        return addresses;
    }

    /**
     * @return the number of inbound datagrams the filter dropped
     */
    public int getFilteredCount() {
        return this.filteredCount;
    }

    /**
     * @return the number of datagrams the injected loss dropped
     */
    public int getLostCount() {
        return this.lostCount;
    }

    /**
     * @return the current time of the NAT's clock
     */
    long getClockMillis() {
        return this.clock.getEpochMillis();
    }


    /**
     * Opens an endpoint of a new host behind the NAT, with its own 127.0.0.1 socket as its inside address.
     *
     * @return the opened endpoint
     * @throws IOException if the inside socket cannot be bound
     */
    public @NotNull NatEndpoint openEndpoint() throws IOException {
        DatagramChannel channel = openLoopbackChannel();
        NatEndpoint endpoint = new NatEndpoint(this, channel, (InetSocketAddress) channel.getLocalAddress());

        this.endpoints.add(endpoint);
        this.hosts.assignPrivate(endpoint.getLocalAddress(), this.name);

        return endpoint;
    }

    /**
     * Sends a datagram of an inside endpoint: directly to an inside address of the same NAT, nowhere to an inside address of another NAT,
     * otherwise through its mapping for the target, opening the mapping when there is none.
     *
     * @param inside   the inside endpoint
     * @param datagram the datagram, read from its position to its limit
     * @param target   the receiver's address
     * @throws IOException if a mapping cannot be opened or the datagram cannot be sent
     */
    void send(@NotNull NatEndpoint inside, @NotNull ByteBuffer datagram, @NotNull InetSocketAddress target) throws IOException {
        String network = this.hosts.findNetwork(target);

        if (network != null) {
            if (network.equals(this.name)) {
                inside.sendDirect(datagram, target);
            } else {
                datagram.position(datagram.limit());
            }

            return;
        }

        long nowMillis = this.clock.getEpochMillis();

        this.closeIdleMappings(nowMillis);

        NatMapping mapping = this.findMapping(inside, target);

        if (mapping == null) {
            DatagramChannel channel = openLoopbackChannel();
            InetSocketAddress publicAddress = (InetSocketAddress) channel.getLocalAddress();

            mapping = new NatMapping(channel, publicAddress, inside, this.type.isMappingPerDestination() ? target : null, nowMillis);
            this.mappings.add(mapping);
            this.hosts.assign(publicAddress, this.name);
        }

        mapping.recordSent(target, this.hosts.find(target), nowMillis);

        if (this.isLost()) {
            datagram.position(datagram.limit());

            return;
        }

        try {
            mapping.getChannel().send(datagram, target);
        } catch (PortUnreachableException exception) {
            // A closed receiver; the datagram is lost like on a real network.
        }
    }

    /**
     * Reads every waiting datagram of the public sockets and passes those the filter and the injected loss let through to their inside
     * endpoints.
     *
     * @throws IOException if a public socket fails
     */
    void pump() throws IOException {
        long nowMillis = this.clock.getEpochMillis();

        this.closeIdleMappings(nowMillis);

        for (NatMapping mapping : this.mappings) {
            SocketAddress received = this.receive(mapping.getChannel());

            while (received instanceof InetSocketAddress source) {
                if (!mapping.permits(this.type, source, this.hosts.find(source))) {
                    this.filteredCount++;
                } else if (!this.isLost()) {
                    this.buffer.flip();
                    mapping.getInside().deliver(Arrays.copyOf(this.buffer.array(), this.buffer.limit()), source, nowMillis + this.delayMillis);
                }

                received = this.receive(mapping.getChannel());
            }
        }
    }

    /**
     * Closes the mappings of an inside endpoint that is closed.
     *
     * @param inside the inside endpoint
     */
    void closeMappings(@NotNull NatEndpoint inside) {
        Iterator<NatMapping> iterator = this.mappings.iterator();

        while (iterator.hasNext()) {
            NatMapping mapping = iterator.next();

            if (mapping.getInside() == inside) {
                iterator.remove();
                this.closeMapping(mapping);
            }
        }

        this.hosts.release(inside.getLocalAddress());
        this.endpoints.remove(inside);
    }

    private @Nullable NatMapping findMapping(@NotNull NatEndpoint inside, @NotNull InetSocketAddress target) {
        for (NatMapping mapping : this.mappings) {
            if (mapping.serves(inside, target)) {
                return mapping;
            }
        }

        return null;
    }

    private @Nullable SocketAddress receive(@NotNull DatagramChannel channel) throws IOException {
        while (true) {
            this.buffer.clear();

            try {
                return channel.receive(this.buffer);
            } catch (PortUnreachableException exception) {
                // An earlier datagram met a closed port; the next waiting datagram is still readable.
            }
        }
    }

    private boolean isLost() {
        if (this.lossRate > 0.0 && this.random.nextDouble() < this.lossRate) {
            this.lostCount++;

            return true;
        }

        return false;
    }

    private void closeIdleMappings(long nowMillis) {
        Iterator<NatMapping> iterator = this.mappings.iterator();

        while (iterator.hasNext()) {
            NatMapping mapping = iterator.next();

            if (nowMillis - mapping.getLastSentMillis() > IDLE_MILLIS) {
                iterator.remove();
                this.closeMapping(mapping);
            }
        }
    }

    private void closeMapping(@NotNull NatMapping mapping) {
        this.hosts.release(mapping.getPublicAddress());
        IgnoreException.execute(
                mapping::close
        );
    }

    /**
     * Closes every endpoint and mapping of the NAT.
     */
    @Override
    public void close() {
        for (NatEndpoint endpoint : List.copyOf(this.endpoints)) {
            IgnoreException.execute(
                    endpoint::close
            );
        }

        for (NatMapping mapping : this.mappings) {
            this.closeMapping(mapping);
        }

        this.mappings.clear();
    }
}
