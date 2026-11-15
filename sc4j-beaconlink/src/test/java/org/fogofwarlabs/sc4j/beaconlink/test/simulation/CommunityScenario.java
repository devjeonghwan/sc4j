package org.fogofwarlabs.sc4j.beaconlink.test.simulation;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.client.ClientConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.BooleanSupplier;

/**
 * Runs the community tests' network on a {@link SimulatedNetwork}: three seeds and further full peers (public hosts) that settle into one
 * routing network, and Beacon.Link clients that list some of them as their nodes. Proofs and player stamps use a low difficulty so that the
 * run spends its time on the protocol, not on the proofs.
 */
public final class CommunityScenario {
    /**
     * The peer proof difficulty of the scenario.
     */
    public static final int PROOF_DIFFICULTY = 8;
    /**
     * The player stamp difficulty of the scenario.
     */
    public static final int STAMP_DIFFICULTY = 8;
    /**
     * The mod version of the clients that run the same mod identity.
     */
    public static final @NotNull String MOD_VERSION = "1.0.0";

    private static final int REGIONS = 8;
    private static final int SEED_COUNT = 3;
    private static final long JOIN_INTERVAL_MILLIS = 100L;
    private static final long SETTLE_MILLIS = 60_000L;

    private final @NotNull SimulatedNetwork network;
    private final @NotNull ClientConfiguration clientConfiguration;
    private final @NotNull ModIdentity identity;
    private final @NotNull List<@NotNull SimulatedPeer> peers;
    private final @NotNull List<@NotNull SimulatedClient> clients;

    /**
     * Creates a {@link CommunityScenario} instance: adds the seeds and the full peers one by one and lets them settle for a minute.
     *
     * @param seed      the seed of the run
     * @param lossRate  the probability that the network loses a datagram
     * @param peerCount the number of full peers, the three seeds included
     */
    public CommunityScenario(long seed, double lossRate, int peerCount) {
        PeerConfiguration peerConfiguration = new PeerConfiguration.Builder().setProofDifficulty(PROOF_DIFFICULTY)
                                                                             .setPlayerStampDifficulty(STAMP_DIFFICULTY)
                                                                             .build();

        this.network             = new SimulatedNetwork(seed, REGIONS, lossRate);
        this.clientConfiguration = new ClientConfiguration.Builder().setStampDifficulty(STAMP_DIFFICULTY).build();
        this.identity            = ClientScenario.createIdentity(MOD_VERSION);
        this.peers               = new ArrayList<>();
        this.clients             = new ArrayList<>();

        List<InetSocketAddress> seeds = new ArrayList<>();

        for (int index = 0; index < peerCount; index++) {
            List<InetSocketAddress> greeted = index < SEED_COUNT ? List.copyOf(seeds)
                                                                 : List.of(seeds.get(this.network.getRandom().nextInt(seeds.size())));
            SimulatedPeer peer = this.network.addPeer(peerConfiguration, null, greeted);

            this.peers.add(peer);

            if (index < SEED_COUNT) {
                seeds.add(peer.getAddress());
            }

            this.network.run(JOIN_INTERVAL_MILLIS);
        }

        this.network.run(SETTLE_MILLIS);
    }

    /**
     * @return the network
     */
    public @NotNull SimulatedNetwork getNetwork() {
        return this.network;
    }

    /**
     * @return the random source of the run, for the choices of a test
     */
    public @NotNull SplittableRandom getRandom() {
        return this.network.getRandom();
    }

    /**
     * @return the mod identity of the scenario's clients
     */
    public @NotNull ModIdentity getIdentity() {
        return this.identity;
    }

    /**
     * @return the full peers, the seeds first
     */
    public @NotNull List<@NotNull SimulatedPeer> getPeers() {
        return List.copyOf(this.peers);
    }

    /**
     * @return the clients in the order they were added
     */
    public @NotNull List<@NotNull SimulatedClient> getClients() {
        return List.copyOf(this.clients);
    }


    /**
     * Adds a client of the scenario's mod identity that lists a number of different random full peers as its nodes.
     *
     * @param displayName the client's display name
     * @param nat         the client's NAT, or {@code null} for a public host
     * @param nodeCount   the number of nodes it lists
     * @return the added client
     */
    public @NotNull SimulatedClient addClient(@NotNull String displayName, @Nullable NatType nat, int nodeCount) {
        List<SimulatedPeer> candidates = new ArrayList<>(this.peers);
        List<InetSocketAddress> nodes = new ArrayList<>();

        for (int index = 0; index < nodeCount && !candidates.isEmpty(); index++) {
            nodes.add(candidates.remove(this.network.getRandom().nextInt(candidates.size())).getAddress());
        }

        return this.addClient(displayName, nat, this.identity, nodes);
    }

    /**
     * Adds a client that lists the given nodes.
     *
     * @param displayName the client's display name
     * @param nat         the client's NAT, or {@code null} for a public host
     * @param identity    the client's mod identity
     * @param nodes       the addresses of its nodes
     * @return the added client
     */
    public @NotNull SimulatedClient addClient(@NotNull String displayName, @Nullable NatType nat, @NotNull ModIdentity identity,
                                              @NotNull List<@NotNull InetSocketAddress> nodes) {
        SimulatedClient client = this.network.addClient(this.clientConfiguration, nat, identity, displayName, nodes);

        this.clients.add(client);

        return client;
    }

    /**
     * Runs the network for a time.
     *
     * @param millis the virtual time to run, in milliseconds
     */
    public void run(long millis) {
        this.network.run(millis);
    }

    /**
     * Runs the network in steps until a condition holds.
     *
     * @param condition     the condition
     * @param maximumMillis the most virtual time to run
     * @param stepMillis    the step
     * @param description   the condition's text in the failure
     * @return the virtual time it took, in milliseconds
     * @throws AssertionError if the condition does not hold in time
     */
    public long runUntil(@NotNull BooleanSupplier condition, long maximumMillis, long stepMillis, @NotNull String description) {
        long waited = 0L;

        while (!condition.getAsBoolean()) {
            if (waited >= maximumMillis) {
                throw Exceptions.newAssertionError("Not reached in {0} ms of virtual time: {1}", maximumMillis, description);
            }

            this.network.run(stepMillis);
            waited += stepMillis;
        }

        return waited;
    }
}
