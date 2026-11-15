package org.fogofwarlabs.sc4j.beaconlink.test.nat;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.BooleanSupplier;

/**
 * Runs peers on 127.0.0.1 in the test thread on the manual protocol clock: outside peers on sockets of their own (public hosts), and peers
 * behind emulated NATs, each a {@link MeshPeer} driven on a {@link NatEndpoint} as an owner drives it on its socket. The test advances the
 * clock round by round; every peer is polled after each step.
 */
public final class PeerNetworkScenario implements AutoCloseable {
    private static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    private static final @NotNull String NAT_NAME_PREFIX = "nat-";
    private static final long RANDOM_SEED = 20261109L;
    private static final long POLL_WAIT_MILLIS = 2L;
    private static final int POLLS_PER_ROUND = 10;
    private static final int DATAGRAMS_PER_POLL = 256;

    /**
     * Runs one peer behind its own emulated NAT.
     */
    public static final class NatPeer {
        private final @NotNull LoopbackNat nat;
        private final @NotNull NatEndpoint endpoint;
        private final @NotNull MeshPeer peer;
        private final @NotNull ByteBuffer buffer;

        private boolean stopped;

        private NatPeer(@NotNull LoopbackNat nat, @NotNull NatEndpoint endpoint, @NotNull MeshPeer peer) {
            this.nat      = nat;
            this.endpoint = endpoint;
            this.peer     = peer;
            this.buffer   = ByteBuffer.allocate(Envelope.MAXIMUM_DATAGRAM_LENGTH + 1);

            this.stopped = false;
        }

        /**
         * @return the NAT the peer is behind
         */
        public @NotNull LoopbackNat getNat() {
            return this.nat;
        }

        /**
         * @return the peer
         */
        public @NotNull MeshPeer getPeer() {
            return this.peer;
        }

        private void poll() throws IOException {
            if (this.stopped) {
                return;
            }

            for (int count = 0; count < DATAGRAMS_PER_POLL; count++) {
                this.buffer.clear();

                InetSocketAddress source = this.endpoint.receive(this.buffer);

                if (source == null) {
                    break;
                }

                this.peer.receive(this.buffer.array(), this.buffer.position(), source);
            }

            this.peer.maintain();
        }
    }


    private final @NotNull ManualProtocolClock clock;
    private final @NotNull SplittableRandom random;
    private final @NotNull LoopbackHosts hosts;

    private final @NotNull List<@NotNull PeerSocket> outsidePeers;
    private final @NotNull List<@NotNull NatPeer> natPeers;

    /**
     * Creates a {@link PeerNetworkScenario} instance without peers.
     */
    public PeerNetworkScenario() {
        this.clock  = new ManualProtocolClock();
        this.random = new SplittableRandom(RANDOM_SEED);
        this.hosts  = new LoopbackHosts();

        this.outsidePeers = new ArrayList<>();
        this.natPeers     = new ArrayList<>();
    }

    /**
     * @return the manual protocol clock of every peer
     */
    public @NotNull ManualProtocolClock getClock() {
        return this.clock;
    }


    /**
     * Opens a peer outside every NAT, on a 127.0.0.1 socket of its own.
     *
     * @param configuration the peer's limits and timings
     * @return the peer's socket
     * @throws IOException if the socket cannot be bound
     */
    public @NotNull PeerSocket openOutsidePeer(@NotNull PeerConfiguration configuration) throws IOException {
        PeerSocket socket = PeerSocket.open(configuration, IdentityKeyPair.generate(new SecureRandom()), new InetSocketAddress(LOOPBACK, 0),
                                            this.clock, this.random.split());

        this.outsidePeers.add(socket);

        return socket;
    }

    /**
     * Opens a peer behind a NAT of its own.
     *
     * @param type          the NAT behaviour
     * @param configuration the peer's limits and timings
     * @return the peer behind its NAT
     * @throws IOException if a socket cannot be bound
     */
    public @NotNull NatPeer openNatPeer(@NotNull NatType type, @NotNull PeerConfiguration configuration) throws IOException {
        LoopbackNat nat = new LoopbackNat(type, NAT_NAME_PREFIX + this.natPeers.size(), this.hosts, this.clock, this.random.split());
        NatEndpoint endpoint = nat.openEndpoint();
        MeshPeer peer = new MeshPeer(configuration, IdentityKeyPair.generate(new SecureRandom()), this.clock, this.random.split(),
                                     (datagram, target) -> {
                                         try {
                                             endpoint.send(ByteBuffer.wrap(datagram), target);
                                         } catch (IOException exception) {
                                             // A failed send is a lost datagram, as on a real network.
                                         }
                                     });
        NatPeer natPeer = new NatPeer(nat, endpoint, peer);

        this.natPeers.add(natPeer);

        return natPeer;
    }

    /**
     * Stops an outside peer: its socket closes, so datagrams to it are lost.
     *
     * @param socket the peer's socket
     */
    public void stopOutsidePeer(@NotNull PeerSocket socket) {
        this.outsidePeers.remove(socket);
        socket.close();
    }

    /**
     * Stops a peer behind a NAT: it is not polled any more and its NAT closes.
     *
     * @param natPeer the peer behind its NAT
     */
    public void stopNatPeer(@NotNull NatPeer natPeer) {
        this.natPeers.remove(natPeer);
        natPeer.stopped = true;
        natPeer.nat.close();
    }

    /**
     * Polls every peer once.
     *
     * @throws IOException if a socket fails
     */
    public void poll() throws IOException {
        for (PeerSocket socket : this.outsidePeers) {
            socket.poll(POLL_WAIT_MILLIS);
        }

        for (NatPeer natPeer : this.natPeers) {
            natPeer.poll();
        }
    }

    /**
     * Advances the clock round by round, polling every peer after each step, until a condition holds.
     *
     * @param condition     the condition
     * @param roundMillis   the clock step of a round
     * @param maximumRounds the most rounds
     * @param description   the condition, for the failure message
     * @throws IOException    if a socket fails
     * @throws AssertionError if the condition does not hold after the last round
     */
    public void awaitRounds(@NotNull BooleanSupplier condition, long roundMillis, int maximumRounds, @NotNull String description) throws IOException {
        for (int round = 0; round < maximumRounds; round++) {
            if (condition.getAsBoolean()) {
                return;
            }

            this.clock.advance(roundMillis);

            for (int poll = 0; poll < POLLS_PER_ROUND; poll++) {
                this.poll();
            }
        }

        if (!condition.getAsBoolean()) {
            throw Exceptions.newAssertionError("Not reached in {0} rounds: {1}", maximumRounds, description);
        }
    }

    /**
     * Advances the clock by a number of rounds, polling every peer after each step.
     *
     * @param roundMillis the clock step of a round
     * @param rounds      the number of rounds
     * @throws IOException if a socket fails
     */
    public void runRounds(long roundMillis, int rounds) throws IOException {
        for (int round = 0; round < rounds; round++) {
            this.clock.advance(roundMillis);

            for (int poll = 0; poll < POLLS_PER_ROUND; poll++) {
                this.poll();
            }
        }
    }


    /**
     * Closes every peer, socket and NAT.
     */
    @Override
    public void close() {
        for (PeerSocket socket : this.outsidePeers) {
            IgnoreException.execute(
                    socket::close
            );
        }

        for (NatPeer natPeer : this.natPeers) {
            natPeer.stopped = true;
            IgnoreException.execute(
                    natPeer.nat::close
            );
        }
    }
}
