package org.fogofwarlabs.sc4j.beaconlink.peer;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.random.RandomGenerator;

/**
 * Runs one {@link MeshPeer} alone on its own UDP socket: the headless node program, the launcher's peer and the tests' peers. Every
 * received datagram belongs to the peer.
 * <p>
 * The socket is driven from one thread: {@link #poll} waits for datagrams, hands them to the peer and runs its due maintenance; the run loop
 * calls it until {@link #stop} is called from any thread.
 */
public final class PeerSocket implements Closeable {
    private static final @NotNull Logger LOGGER = Logger.of(PeerSocket.class);
    private static final int DATAGRAMS_PER_POLL = 256;

    /**
     * Creates a {@link PeerSocket} instance: a peer on a socket bound to a local address.
     *
     * @param configuration the peer's limits and timings
     * @param keyPair       the peer's key pair
     * @param bindAddress   the address the peer listens on
     * @param clock         the protocol clock
     * @param random        the source of nonces, token secret and peer choices
     * @return the created {@link PeerSocket} instance with its peer
     * @throws IOException if the address cannot be bound
     */
    public static @NotNull PeerSocket open(@NotNull PeerConfiguration configuration, @NotNull IdentityKeyPair keyPair,
                                           @NotNull InetSocketAddress bindAddress, @NotNull ProtocolClock clock,
                                           @NotNull RandomGenerator random) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        Selector selector = null;

        try {
            channel.configureBlocking(false);
            channel.bind(bindAddress);
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
        } catch (IOException exception) {
            if (selector != null) {
                selector.close();
            }

            channel.close();

            throw exception;
        }

        return new PeerSocket(configuration, keyPair, clock, random, channel, selector);
    }


    private final @NotNull DatagramChannel channel;
    private final @NotNull Selector selector;
    private final @NotNull ByteBuffer receiveBuffer;

    private final @NotNull MeshPeer peer;

    private volatile boolean stopped;

    private PeerSocket(@NotNull PeerConfiguration configuration, @NotNull IdentityKeyPair keyPair, @NotNull ProtocolClock clock,
                       @NotNull RandomGenerator random, @NotNull DatagramChannel channel, @NotNull Selector selector) {
        this.channel       = channel;
        this.selector      = selector;
        this.receiveBuffer = ByteBuffer.allocate(Envelope.MAXIMUM_DATAGRAM_LENGTH + 1);

        this.peer = new MeshPeer(configuration, keyPair, clock, random, this::send);

        this.stopped = false;
    }

    /**
     * @return the address the peer listens on
     * @throws IOException if the socket is closed
     */
    public @NotNull InetSocketAddress getLocalAddress() throws IOException {
        return (InetSocketAddress) this.channel.getLocalAddress();
    }

    /**
     * @return the peer that runs on this socket
     */
    public @NotNull MeshPeer getPeer() {
        return this.peer;
    }

    /**
     * @return {@code true} if {@link #stop} was called, {@code false} otherwise
     */
    public boolean isStopped() {
        return this.stopped;
    }


    /**
     * Waits up to a time for datagrams, hands every waiting datagram to the peer (at most 256 per poll), then runs the peer's due
     * maintenance.
     *
     * @param waitMillis the longest wait for a first datagram, {@code 0} for none
     * @throws IOException if the socket fails
     */
    public void poll(long waitMillis) throws IOException {
        if (waitMillis > 0L) {
            this.selector.select(waitMillis);
        } else {
            this.selector.selectNow();
        }

        this.selector.selectedKeys().clear();

        for (int count = 0; count < DATAGRAMS_PER_POLL; count++) {
            SocketAddress source;

            this.receiveBuffer.clear();

            try {
                source = this.channel.receive(this.receiveBuffer);
            } catch (PortUnreachableException exception) {
                continue;
            }

            if (!(source instanceof InetSocketAddress address)) {
                break;
            }

            this.peer.receive(this.receiveBuffer.array(), this.receiveBuffer.position(), address);
        }

        this.peer.maintain();
    }

    /**
     * Asks the socket's run loop to end: {@link #isStopped()} becomes {@code true} and a waiting {@link #poll} returns. May be called from any
     * thread.
     */
    public void stop() {
        this.stopped = true;
        this.selector.wakeup();
    }

    private void send(byte @NotNull [] datagram, @NotNull InetSocketAddress target) {
        try {
            this.channel.send(ByteBuffer.wrap(datagram), target);
        } catch (IOException exception) {
            LOGGER.debug("Cannot send a datagram to {0}: {1}", target, exception.getMessage());
        }
    }

    /**
     * Closes the socket; the peer handles nothing afterwards.
     */
    @Override
    public void close() {
        LOGGER.info("Peer {0} stops: {1}", this.peer.getKey(), this.peer.getStatistics());
        IgnoreException.execute(
                this.selector::close,
                this.channel::close
        );
    }
}
