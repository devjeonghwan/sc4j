package org.fogofwarlabs.sc4j.beaconlink.test.client;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.beaconlink.client.BeaconLinkClient;
import org.fogofwarlabs.sc4j.beaconlink.client.ClientConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.KnownNodes;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.LoopbackHosts;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.LoopbackNat;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.DatagramEndpoint;
import org.fogofwarlabs.sc4j.engine.net.UdpDatagramEndpoint;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Runs real nodes and Beacon.Link clients on 127.0.0.1 in the test thread, directly or behind emulated NATs: the protocol clock is manual,
 * the test polls every node and client itself, and every wait has a real-time deadline. Clock steps are followed by a short real-time slice
 * of polls, so that loopback datagrams arrive before the next step. The tests of the client and of the protocols over Beacon.Link (the
 * engine's lobby and lockstep transport, a mod's network game) share it; their own parts join the polls ({@link #addPollAction}).
 */
public final class ClientScenario implements AutoCloseable {
    /**
     * The address every node, client and emulated NAT of the scenario binds to.
     */
    public static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    /**
     * The seed of the scenario's random sources.
     */
    public static final long RANDOM_SEED = 20261108L;
    /**
     * The clock step of {@link #advance} and {@link #advanceUntil(BooleanSupplier, long, String)}.
     */
    public static final long STEP_MILLIS = 1_000L;
    /**
     * The player stamp difficulty of the scenario's clients and of the nodes of {@link #createNodeBuilder()}: low, so that a client's stamp
     * costs little time.
     */
    public static final int STAMP_DIFFICULTY = 16;

    private static final long WAIT_MILLIS = 10_000L;
    private static final long SLICE_MILLIS = 15L;
    private static final int DIGEST_LENGTH = 32;
    private static final @NotNull String MOD_ID = "beacon-test";
    private static final @NotNull String MANIFEST = """
                                                    {"format": 1, "id": "%s", "name": {"en": "Beacon test"}, "version": "%s", "engine": "0.1", "entry": "test.Entry"}
                                                    """;

    /**
     * Polls one part of a test, for example a lobby, in the scenario's polls.
     */
    @FunctionalInterface
    public interface PollAction {
        /**
         * Polls the part once.
         *
         * @throws IOException if a socket fails
         */
        void poll() throws IOException;
    }

    /**
     * Creates the configuration builder of a node that checks player stamps at the scenario's difficulty.
     *
     * @return the created {@link PeerConfiguration.Builder} instance
     */
    public static @NotNull PeerConfiguration.Builder createNodeBuilder() {
        return new PeerConfiguration.Builder().setPlayerStampDifficulty(STAMP_DIFFICULTY);
    }

    /**
     * Creates the default configuration of a node, checking player stamps at the scenario's difficulty.
     *
     * @return the created {@link PeerConfiguration} instance
     */
    public static @NotNull PeerConfiguration createNodeConfiguration() {
        return createNodeBuilder().build();
    }

    /**
     * Creates the mod identity of a test mod version on the current engine.
     *
     * @param version the mod version, such as {@code 1.0.0}
     * @return the identity
     */
    public static @NotNull ModIdentity createIdentity(@NotNull String version) {
        return createIdentity(EngineVersion.CURRENT, version);
    }

    /**
     * Creates the mod identity of a test mod version on an engine version.
     *
     * @param engine  the engine version of the run
     * @param version the mod version, such as {@code 1.0.0}
     * @return the identity
     */
    public static @NotNull ModIdentity createIdentity(@NotNull EngineVersion engine, @NotNull String version) {
        return createIdentity(engine, MOD_ID, version);
    }

    /**
     * Creates the mod identity of a version of a test mod with another id on an engine version.
     *
     * @param engine  the engine version of the run
     * @param modId   the mod id, such as {@code other-test}
     * @param version the mod version, such as {@code 1.0.0}
     * @return the identity
     */
    public static @NotNull ModIdentity createIdentity(@NotNull EngineVersion engine, @NotNull String modId, @NotNull String version) {
        try {
            return ModIdentity.create(engine, ModManifestReader.parse(MANIFEST.formatted(modId, version).getBytes(StandardCharsets.UTF_8)),
                                      new byte[DIGEST_LENGTH], List.of());
        } catch (ManifestException exception) {
            throw Exceptions.newIllegalStateException(exception, "The test manifest is invalid.");
        }
    }

    /**
     * Lists a node at its loopback address without a pinned key.
     *
     * @param node the node
     * @return the node list entry
     * @throws IOException if the node is closed
     */
    public static @NotNull ConfiguredNode listed(@NotNull PeerSocket node) throws IOException {
        return ConfiguredNode.parse("127.0.0.1:" + node.getLocalAddress().getPort(), null);
    }


    private final @NotNull ManualProtocolClock clock;
    private final @NotNull SplittableRandom random;
    private final @NotNull LoopbackHosts hosts;
    private final @NotNull ClientConfiguration configuration;

    private final @NotNull List<@NotNull PeerSocket> nodes;
    private final @NotNull List<@NotNull BeaconLinkClient> clients;
    private final @NotNull Map<@NotNull BeaconLinkClient, @NotNull PlayerStamp> stamps;
    private final @NotNull Map<@NotNull BeaconLinkClient, @NotNull IdentityKeyPair> keyPairs;
    private final @NotNull List<@NotNull LoopbackNat> nats;
    private final @NotNull List<@NotNull PollAction> pollActions;

    /**
     * Creates a {@link ClientScenario} instance without nodes, clients and NATs.
     */
    public ClientScenario() {
        this.clock         = new ManualProtocolClock();
        this.random        = new SplittableRandom(RANDOM_SEED);
        this.hosts         = new LoopbackHosts();
        this.configuration = new ClientConfiguration.Builder().setStampDifficulty(STAMP_DIFFICULTY).build();

        this.nodes       = new ArrayList<>();
        this.clients     = new ArrayList<>();
        this.stamps      = new HashMap<>();
        this.keyPairs    = new HashMap<>();
        this.nats        = new ArrayList<>();
        this.pollActions = new ArrayList<>();
    }

    /**
     * @return the protocol clock of every node and client
     */
    public @NotNull ManualProtocolClock getClock() {
        return this.clock;
    }

    /**
     * @return the configuration of every client, with the scenario's stamp difficulty
     */
    public @NotNull ClientConfiguration getConfiguration() {
        return this.configuration;
    }


    /**
     * Opens a node on its own 127.0.0.1 socket.
     *
     * @param nodeConfiguration the node's configuration
     * @return the node
     * @throws IOException if the socket cannot be bound
     */
    public @NotNull PeerSocket openNode(@NotNull PeerConfiguration nodeConfiguration) throws IOException {
        PeerSocket node = PeerSocket.open(nodeConfiguration, IdentityKeyPair.generate(new SecureRandom()), new InetSocketAddress(LOOPBACK, 0),
                                          this.clock, this.random.split());

        this.nodes.add(node);
        this.hosts.assign(node.getLocalAddress(), "node-" + this.nodes.size());

        return node;
    }

    /**
     * Closes a node of the scenario.
     *
     * @param node the node
     */
    public void closeNode(@NotNull PeerSocket node) {
        this.nodes.remove(node);
        node.close();
    }

    /**
     * Opens an emulated NAT.
     *
     * @param type the NAT type
     * @return the NAT
     */
    public @NotNull LoopbackNat openNat(@NotNull NatType type) {
        LoopbackNat nat = new LoopbackNat(type, "nat-" + (this.nats.size() + 1), this.hosts, this.clock, this.random.split());

        this.nats.add(nat);

        return nat;
    }

    /**
     * Opens a client on its own 127.0.0.1 socket.
     *
     * @param identity the client's mod identity
     * @param listed   the client's node list
     * @return the client
     * @throws IOException if the socket cannot be bound
     */
    public @NotNull BeaconLinkClient openClient(@NotNull ModIdentity identity, @NotNull List<@NotNull ConfiguredNode> listed) throws IOException {
        return this.openClient(identity, listed, UdpDatagramEndpoint.open(new InetSocketAddress(LOOPBACK, 0)));
    }

    /**
     * Opens a client on a given endpoint, for example one behind an emulated NAT. The client has a new key and is named {@code player n}, with
     * its stamp of the scenario's difficulty, and a random connection number.
     *
     * @param identity the client's mod identity
     * @param listed   the client's node list
     * @param endpoint the client's endpoint, which the client closes
     * @return the client
     */
    public @NotNull BeaconLinkClient openClient(@NotNull ModIdentity identity, @NotNull List<@NotNull ConfiguredNode> listed,
                                                @NotNull DatagramEndpoint endpoint) {
        IdentityKeyPair keyPair = IdentityKeyPair.generate(new SecureRandom());
        PlayerStamp stamp = PlayerStamp.solve(keyPair.getPublicKey(), "player " + (this.clients.size() + 1), STAMP_DIFFICULTY);

        return this.openClient(keyPair, stamp, identity, listed, endpoint, this.clock);
    }

    /**
     * Opens another connection of a client's account on its own 127.0.0.1 socket: the same key and stamp with a random connection number of
     * its own, as a second device of the account.
     *
     * @param account  a client of the scenario whose account the connection uses
     * @param identity the connection's mod identity
     * @param listed   the connection's node list
     * @return the connection's client
     * @throws IOException if the socket cannot be bound
     */
    public @NotNull BeaconLinkClient openConnection(@NotNull BeaconLinkClient account, @NotNull ModIdentity identity,
                                                    @NotNull List<@NotNull ConfiguredNode> listed) throws IOException {
        return this.openConnection(account, identity, listed, this.clock);
    }

    /**
     * Opens another connection of a client's account on its own 127.0.0.1 socket with a clock of its own, for example one behind the
     * scenario's clock, whose connection started earlier than the others' clocks tell.
     *
     * @param account  a client of the scenario whose account the connection uses
     * @param identity the connection's mod identity
     * @param listed   the connection's node list
     * @param clock    the connection's protocol clock
     * @return the connection's client
     * @throws IOException if the socket cannot be bound
     */
    public @NotNull BeaconLinkClient openConnection(@NotNull BeaconLinkClient account, @NotNull ModIdentity identity,
                                                    @NotNull List<@NotNull ConfiguredNode> listed, @NotNull ProtocolClock clock) throws IOException {
        IdentityKeyPair keyPair = this.keyPairs.get(account);
        PlayerStamp stamp = this.stamps.get(account);

        if (keyPair == null || stamp == null) {
            throw Exceptions.newIllegalArgumentException("The client {0} is not of this scenario.", account.getParticipant());
        }

        return this.openClient(keyPair, stamp, identity, listed, UdpDatagramEndpoint.open(new InetSocketAddress(LOOPBACK, 0)), clock);
    }

    private @NotNull BeaconLinkClient openClient(@NotNull IdentityKeyPair keyPair, @NotNull PlayerStamp stamp, @NotNull ModIdentity identity,
                                                 @NotNull List<@NotNull ConfiguredNode> listed, @NotNull DatagramEndpoint endpoint,
                                                 @NotNull ProtocolClock clock) {
        SplittableRandom clientRandom = this.random.split();
        long connection = clientRandom.nextLong();
        BeaconLinkClient client = new BeaconLinkClient(this.configuration, clock, clientRandom, keyPair, stamp, connection, identity,
                                                       KnownNodes.open(listed, null), null, endpoint);

        this.clients.add(client);
        this.keyPairs.put(client, keyPair);
        this.stamps.put(client, stamp);

        return client;
    }

    /**
     * Adds a part that every poll of the scenario polls after the nodes and the clients, for example a lobby.
     *
     * @param action the part's poll
     */
    public void addPollAction(@NotNull PollAction action) {
        this.pollActions.add(action);
    }

    /**
     * Removes a part added with {@link #addPollAction}; the scenario's polls no longer poll it.
     *
     * @param action the part's poll, as it was added
     */
    public void removePollAction(@NotNull PollAction action) {
        this.pollActions.remove(action);
    }

    /**
     * Polls every node, every open client and every added part once.
     *
     * @throws IOException if a socket fails
     */
    public void pollAll() throws IOException {
        for (PeerSocket node : this.nodes) {
            node.poll(0L);
        }

        for (BeaconLinkClient client : this.clients) {
            if (!client.isClosed()) {
                client.poll();
            }
        }

        for (PollAction action : this.pollActions) {
            action.poll();
        }
    }

    /**
     * Polls everything without moving the clock until a condition holds.
     *
     * @param condition   the condition
     * @param description the condition's text in the failure
     * @throws IOException if a socket fails
     */
    public void await(@NotNull BooleanSupplier condition, @NotNull String description) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS);

        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                throw Exceptions.newAssertionError("Not reached in time: {0}", description);
            }

            this.pollAll();
        }
    }

    /**
     * Moves the clock in steps of {@value #STEP_MILLIS} ms, polling a real-time slice after each step.
     *
     * @param millis the protocol time to move
     * @throws IOException if a socket fails
     */
    public void advance(long millis) throws IOException {
        for (long moved = 0L; moved < millis; moved += STEP_MILLIS) {
            this.clock.advance(Math.min(STEP_MILLIS, millis - moved));
            this.pollSlice();
        }
    }

    /**
     * Moves the clock in steps of {@value #STEP_MILLIS} ms until a condition holds.
     *
     * @param condition     the condition
     * @param maximumMillis the most protocol time to move
     * @param description   the condition's text in the failure
     * @throws IOException if a socket fails
     */
    public void advanceUntil(@NotNull BooleanSupplier condition, long maximumMillis, @NotNull String description) throws IOException {
        this.advanceUntil(condition, maximumMillis, STEP_MILLIS, description);
    }

    /**
     * Moves the clock in steps of the given length until a condition holds.
     *
     * @param condition     the condition
     * @param maximumMillis the most protocol time to move
     * @param stepMillis    the clock step
     * @param description   the condition's text in the failure
     * @throws IOException if a socket fails
     */
    public void advanceUntil(@NotNull BooleanSupplier condition, long maximumMillis, long stepMillis, @NotNull String description) throws IOException {
        for (long moved = 0L; moved <= maximumMillis; moved += stepMillis) {
            this.pollSlice();

            if (condition.getAsBoolean()) {
                return;
            }

            this.clock.advance(stepMillis);
        }

        throw Exceptions.newAssertionError("Not reached in {0} ms of protocol time: {1}", maximumMillis, description);
    }

    private void pollSlice() throws IOException {
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SLICE_MILLIS);

        while (System.nanoTime() < end) {
            this.pollAll();
        }
    }

    /**
     * Closes every client, NAT and node of the scenario.
     */
    @Override
    public void close() {
        for (BeaconLinkClient client : this.clients) {
            client.close();
        }

        for (LoopbackNat nat : this.nats) {
            nat.close();
        }

        for (PeerSocket node : this.nodes) {
            IgnoreException.execute(
                    node::close
            );
        }
    }
}
