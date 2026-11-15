package org.fogofwarlabs.sc4j.beaconlink.test.simulation;

import org.fogofwarlabs.sc4j.beaconlink.peer.DatagramSink;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs an eclipse attacker on a {@link SimulatedNetwork}: many peer IDs with valid proofs at addresses of one IPv4 /16 network (one per /24,
 * so that only the /16 limit can stop them). Every Sybil says it is reachable, answers hellos, and answers every lookup request with the
 * Sybils closest to the target and one entry with a forged proof, never with an honest peer. {@link #attack} makes every Sybil greet a
 * victim and ask it for its own ID, so that the victim greets them back and takes them into its table if it can.
 */
public final class SybilCluster {
    private static final @NotNull String SOFTWARE = "sybil";
    private static final int ENTRIES_PER_ANSWER = PeersMessage.MAXIMUM_COUNT - 1;
    private static final int LAST_OCTET = 1;

    /**
     * Holds one Sybil: its key, its proof and its endpoint on the network.
     */
    private final class Sybil implements SimulatedNetwork.Endpoint {
        private final @NotNull PeerContact contact;
        private final @NotNull PeerCard card;
        private final @NotNull MessageEncoder encoder;
        private final @NotNull PeerContact forged;

        private @Nullable DatagramSink sink;
        private long nextRequestNumber;

        private Sybil(@NotNull IdentityKeyPair keyPair, long proofNonce, @NotNull InetSocketAddress address, @NotNull PeerContact forged) {
            this.contact = new PeerContact(keyPair.getPublicKey(), proofNonce, address);
            this.card    = new PeerCard(proofNonce, true, CoordinateReport.UNKNOWN);
            this.encoder = new MessageEncoder(keyPair, SybilCluster.this.network.getClock(), SybilCluster.this.network.getRandom().split());
            this.forged  = forged;

            this.sink              = null;
            this.nextRequestNumber = 1L;
        }

        @Override
        public void receive(byte @NotNull [] datagram, @NotNull InetSocketAddress source) {
            Envelope envelope = Envelope.decode(datagram, datagram.length).getEnvelope();
            DatagramSink output = this.sink;

            if (envelope == null || envelope.isReply() || output == null) {
                return;
            }

            switch (envelope.getKind()) {
                case HELLO -> output.send(this.encoder.encodeReply(new HelloMessage(this.card, HelloMessage.ROUTING_CAPABILITY, SOFTWARE),
                                                                   envelope.getRequestNumber()), source);
                case FIND_PEERS -> {
                    List<PeerContact> answer = SybilCluster.this.findClosest(((FindPeersMessage) envelope.getBody()).getTarget(), this.contact);

                    answer.add(this.forged);
                    output.send(this.encoder.encodeReply(new PeersMessage(CoordinateReport.UNKNOWN, answer), envelope.getRequestNumber()), source);
                }
                default -> {
                    // The Sybils ignore everything else.
                }
            }
        }

        @Override
        public void tick() {
            // The Sybils act only when the test calls attack.
        }

        private void greet(@NotNull InetSocketAddress victim, @NotNull PeerId victimId) {
            DatagramSink output = this.sink;

            if (output == null) {
                return;
            }

            output.send(this.encoder.encodePaddedRequest(new HelloMessage(this.card, HelloMessage.ROUTING_CAPABILITY, SOFTWARE),
                                                         this.nextRequestNumber++), victim);
            output.send(this.encoder.encodePaddedRequest(new FindPeersMessage(victimId, this.card), this.nextRequestNumber++), victim);
        }
    }


    private final @NotNull SimulatedNetwork network;
    private final @NotNull List<@NotNull Sybil> sybils;
    private final @NotNull Set<@NotNull IdentityKey> keys;

    /**
     * Creates a {@link SybilCluster} instance and adds its hosts to the network: one Sybil per /24 network of {@code first.second.0.0/16}.
     *
     * @param network    the network
     * @param count      the number of Sybils, at most 256
     * @param first      the first octet of the /16 network
     * @param second     the second octet of the /16 network
     * @param difficulty the proof difficulty of the network
     */
    public SybilCluster(@NotNull SimulatedNetwork network, int count, int first, int second, int difficulty) {
        this.network = network;
        this.sybils  = new ArrayList<>(count);
        this.keys    = new HashSet<>();

        for (int index = 0; index < count; index++) {
            IdentityKeyPair keyPair = network.createKeyPair();
            IdentityKeyPair forgedKey = network.createKeyPair();
            long proofNonce = PeerId.solveProof(keyPair.getPublicKey(), difficulty);
            long forgedNonce = 0L;
            InetAddress host = InetAddress.ofLiteral(first + "." + second + "." + index + "." + LAST_OCTET);

            while (PeerId.isProofValid(forgedKey.getPublicKey(), forgedNonce, difficulty)) {
                forgedNonce++;
            }

            PeerContact forged = new PeerContact(forgedKey.getPublicKey(), forgedNonce, new InetSocketAddress(host, SimulatedNetwork.PORT + 1));
            Sybil[] created = new Sybil[1];
            InetSocketAddress address = new InetSocketAddress(host, SimulatedNetwork.PORT);

            network.addHost(host, null, sink -> {
                created[0]      = new Sybil(keyPair, proofNonce, address, forged);
                created[0].sink = sink;

                return created[0];
            });
            this.sybils.add(created[0]);
            this.keys.add(keyPair.getPublicKey());
        }
    }

    /**
     * Checks whether a key is a Sybil's.
     *
     * @param key the key
     * @return {@code true} if the key belongs to the cluster, {@code false} otherwise
     */
    public boolean isSybil(@NotNull IdentityKey key) {
        return this.keys.contains(key);
    }

    /**
     * Makes every Sybil greet the victim and ask it for its own ID.
     *
     * @param victim   the victim's public address
     * @param victimId the victim's ID
     */
    public void attack(@NotNull InetSocketAddress victim, @NotNull PeerId victimId) {
        for (Sybil sybil : this.sybils) {
            sybil.greet(victim, victimId);
        }
    }

    private @NotNull List<@NotNull PeerContact> findClosest(@NotNull PeerId target, @NotNull PeerContact answering) {
        List<PeerContact> all = new ArrayList<>(this.sybils.size());

        for (Sybil sybil : this.sybils) {
            if (sybil.contact != answering) {
                all.add(sybil.contact);
            }
        }

        all.sort((left, right) -> target.compareDistance(left.getId(), right.getId()));

        return new ArrayList<>(all.subList(0, Math.min(ENTRIES_PER_ANSWER, all.size())));
    }
}
