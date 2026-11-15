package org.fogofwarlabs.sc4j.beaconlink.peer;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.*;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.ModFingerprint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Runs nodes and protocol clients on 127.0.0.1 in the test thread: the test polls the nodes itself, the protocol clock is manual, and every
 * wait has a deadline. Clients are raw protocol endpoints that encode requests and decode what they receive.
 */
final class PeerScenario implements AutoCloseable {
    static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    static final long RANDOM_SEED = 20261108L;
    static final int REGISTRATION_SECONDS = 120;
    /**
     * The connection number a client registers with unless a test names another.
     */
    static final long DEFAULT_CONNECTION = 0L;
    /**
     * The player stamp difficulty of the scenario's nodes and clients: low, so that a client's stamp costs little time, and high enough
     * that another name with the same nonce practically never passes by chance.
     */
    static final int STAMP_DIFFICULTY = 16;
    /**
     * The name of every client of the scenario.
     */
    static final @NotNull String PLAYER_NAME = "player";

    private static final long WAIT_MILLIS = 10_000L;
    private static final long POLL_WAIT_MILLIS = 2L;
    private static final int POLLS_PER_ROUND = 10;

    /**
     * A protocol client with its own key and socket.
     */
    final class Client implements AutoCloseable {
        private final @NotNull IdentityKeyPair keyPair;
        private final @NotNull MessageEncoder encoder;
        private final @NotNull DatagramChannel channel;
        private final @NotNull ByteBuffer buffer;

        private long nextRequestNumber;
        private @Nullable PlayerStamp stamp;

        private Client(@NotNull IdentityKeyPair keyPair, @NotNull ProtocolClock clock) throws IOException {
            this.keyPair = keyPair;
            this.encoder = new MessageEncoder(keyPair, clock, PeerScenario.this.random.split());
            this.channel = DatagramChannel.open(StandardProtocolFamily.INET);
            this.buffer  = ByteBuffer.allocate(Envelope.MAXIMUM_DATAGRAM_LENGTH + 1);

            this.nextRequestNumber = 1L;
            this.stamp             = null;

            this.channel.configureBlocking(false);
            this.channel.bind(new InetSocketAddress(LOOPBACK, 0));
        }

        @NotNull IdentityKeyPair getKeyPair() {
            return this.keyPair;
        }

        @NotNull IdentityKey getKey() {
            return this.keyPair.getPublicKey();
        }

        @NotNull MessageEncoder getEncoder() {
            return this.encoder;
        }

        @NotNull InetSocketAddress getLocalAddress() throws IOException {
            return (InetSocketAddress) this.channel.getLocalAddress();
        }

        long takeRequestNumber() {
            return this.nextRequestNumber++;
        }

        void send(byte @NotNull [] datagram, @NotNull PeerSocket node) throws IOException {
            this.channel.send(ByteBuffer.wrap(datagram), node.getLocalAddress());
        }

        /**
         * Receives the next waiting datagram as an envelope; the signature is not checked.
         */
        @Nullable Envelope receive() throws IOException {
            this.buffer.clear();

            SocketAddress source = this.channel.receive(this.buffer);

            if (source == null) {
                return null;
            }

            Envelope envelope = Envelope.decode(this.buffer.array(), this.buffer.position()).getEnvelope();

            Assertions.assertNotNull(envelope, "a datagram the client cannot decode");

            return envelope;
        }

        /**
         * Polls the nodes until the client receives a datagram that matches, skipping others.
         */
        @NotNull Envelope await(@NotNull Predicate<@NotNull Envelope> condition) throws IOException {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS);

            while (System.nanoTime() < deadline) {
                PeerScenario.this.pollNodes();

                Envelope envelope = this.receive();

                if (envelope != null && condition.test(envelope)) {
                    return envelope;
                }
            }

            throw Exceptions.newAssertionError("The client received no matching datagram in time.");
        }

        /**
         * Sends a request (padded when asked) and waits for the reply with its request number; the reply's signature must be valid.
         */
        @NotNull Envelope request(@NotNull Message message, @NotNull PeerSocket node, boolean padded) throws IOException {
            long request = this.takeRequestNumber();
            byte[] datagram = padded ? this.encoder.encodePaddedRequest(message, request) : this.encoder.encodeRequest(message, request);

            this.send(datagram, node);

            Envelope reply = this.await(envelope -> envelope.isReply() && envelope.getRequestNumber() == request);

            Assertions.assertTrue(reply.isSignatureValid());
            Assertions.assertEquals(node.getPeer().getKey(), reply.getSender());

            return reply;
        }

        /**
         * Returns the client's player stamp of the name {@value #PLAYER_NAME}, found at the first call at the scenario's difficulty.
         */
        @NotNull PlayerStamp getStamp() {
            PlayerStamp found = this.stamp;

            if (found == null) {
                found      = PlayerStamp.solve(this.keyPair.getPublicKey(), PLAYER_NAME, STAMP_DIFFICULTY);
                this.stamp = found;
            }

            return found;
        }

        /**
         * Creates a registration of the client carrying its stamp; the connection starts at the scenario clock's time.
         */
        @NotNull RegisterMessage createRegister(byte @NotNull [] token, int lifetimeSeconds, @NotNull ModFingerprint fingerprint, long connection) {
            return this.createRegister(token, lifetimeSeconds, fingerprint, connection, PeerScenario.this.clock.getEpochMillis());
        }

        /**
         * Creates a registration of the client carrying its stamp and the time its connection started.
         */
        @NotNull RegisterMessage createRegister(byte @NotNull [] token, int lifetimeSeconds, @NotNull ModFingerprint fingerprint,
                                                long connection, long connectionStartMillis) {
            PlayerStamp found = this.getStamp();

            return new RegisterMessage(token, lifetimeSeconds, found.getName(), found.getNonce(), fingerprint, connection, connectionStartMillis);
        }

        /**
         * Registers at a node with the address token exchange and the connection {@value PeerScenario#DEFAULT_CONNECTION}.
         */
        void register(@NotNull PeerSocket node, @NotNull ModFingerprint fingerprint) throws IOException {
            Assertions.assertEquals(ObservedAddressStatus.REGISTERED, this.register(node, fingerprint, DEFAULT_CONNECTION).getStatus());
        }

        /**
         * Registers a connection at a node with the address token exchange.
         *
         * @return the node's answer to the registration with the token
         */
        @NotNull ObservedAddressMessage register(@NotNull PeerSocket node, @NotNull ModFingerprint fingerprint, long connection) throws IOException {
            return this.register(node, fingerprint, connection, PeerScenario.this.clock.getEpochMillis());
        }

        /**
         * Registers a connection that started at the given time at a node with the address token exchange.
         *
         * @return the node's answer to the registration with the token
         */
        @NotNull ObservedAddressMessage register(@NotNull PeerSocket node, @NotNull ModFingerprint fingerprint,
                                                 long connection, long connectionStartMillis) throws IOException {
            RegisterMessage first = this.createRegister(new byte[0], REGISTRATION_SECONDS, fingerprint, connection, connectionStartMillis);
            ObservedAddressMessage challenge = (ObservedAddressMessage) this.request(first, node, true).getBody();

            Assertions.assertEquals(ObservedAddressStatus.TOKEN_REQUIRED, challenge.getStatus());

            RegisterMessage second = this.createRegister(challenge.getToken(), REGISTRATION_SECONDS, fingerprint, connection, connectionStartMillis);

            return (ObservedAddressMessage) this.request(second, node, true).getBody();
        }

        /**
         * Receives every datagram that is still waiting, without polling the nodes.
         */
        @NotNull List<@NotNull Envelope> drain() throws IOException {
            List<Envelope> envelopes = new ArrayList<>();
            Envelope envelope = this.receive();

            while (envelope != null) {
                envelopes.add(envelope);
                envelope = this.receive();
            }

            return envelopes;
        }

        @Override
        public void close() throws IOException {
            this.channel.close();
        }
    }

    /**
     * Creates the configuration builder of a node that checks player stamps at the scenario's difficulty.
     */
    static @NotNull PeerConfiguration.Builder createNodeBuilder() {
        return new PeerConfiguration.Builder().setPlayerStampDifficulty(STAMP_DIFFICULTY);
    }

    /**
     * Creates the default configuration of a node, checking player stamps at the scenario's difficulty.
     */
    static @NotNull PeerConfiguration createNodeConfiguration() {
        return createNodeBuilder().build();
    }


    private final @NotNull ManualProtocolClock clock;
    private final @NotNull SplittableRandom random;
    private final @NotNull List<@NotNull PeerSocket> nodes;
    private final @NotNull List<@NotNull Client> clients;

    PeerScenario() {
        this.clock   = new ManualProtocolClock();
        this.random  = new SplittableRandom(RANDOM_SEED);
        this.nodes   = new ArrayList<>();
        this.clients = new ArrayList<>();
    }

    @NotNull ManualProtocolClock getClock() {
        return this.clock;
    }

    @NotNull PeerSocket openNode(@NotNull PeerConfiguration configuration) throws IOException {
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());
        PeerSocket node = PeerSocket.open(configuration, keyPair, new InetSocketAddress(LOOPBACK, 0), this.clock, this.random.split());

        this.nodes.add(node);

        return node;
    }

    @NotNull Client openClient() throws IOException {
        return this.openClient(IdentityKeyPair.generate(new SecureRandom()), this.clock);
    }

    @NotNull Client openClient(@NotNull ProtocolClock clock) throws IOException {
        return this.openClient(IdentityKeyPair.generate(new SecureRandom()), clock);
    }

    @NotNull Client openClient(@NotNull IdentityKeyPair keyPair) throws IOException {
        return this.openClient(keyPair, this.clock);
    }

    private @NotNull Client openClient(@NotNull IdentityKeyPair keyPair, @NotNull ProtocolClock clock) throws IOException {
        Client client = new Client(keyPair, clock);

        this.clients.add(client);

        return client;
    }

    void closeNode(@NotNull PeerSocket node) {
        this.nodes.remove(node);
        node.close();
    }

    void pollNodes() throws IOException {
        for (PeerSocket node : this.nodes) {
            node.poll(POLL_WAIT_MILLIS);
        }
    }

    /**
     * Polls the nodes until a condition holds.
     */
    void await(@NotNull BooleanSupplier condition, @NotNull String description) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS);

        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                throw Exceptions.newAssertionError("Not reached in time: {0}", description);
            }

            this.pollNodes();
        }
    }

    /**
     * Advances the clock round by round, polling the nodes after each step, until a condition holds.
     */
    void awaitRounds(@NotNull BooleanSupplier condition, long roundMillis, int maximumRounds, @NotNull String description) throws IOException {
        for (int round = 0; round < maximumRounds; round++) {
            if (condition.getAsBoolean()) {
                return;
            }

            this.clock.advance(roundMillis);

            for (int poll = 0; poll < POLLS_PER_ROUND; poll++) {
                this.pollNodes();
            }
        }

        Assertions.assertTrue(condition.getAsBoolean(), "Not reached in " + maximumRounds + " rounds: " + description);
    }

    @Override
    public void close() {
        for (Client client : this.clients) {
            IgnoreException.execute(
                    client::close
            );
        }

        for (PeerSocket node : this.nodes) {
            IgnoreException.execute(
                    node::close
            );
        }
    }
}
