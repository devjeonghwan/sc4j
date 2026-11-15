package org.fogofwarlabs.sc4j.beaconlink.peer;

import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageEncoder;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.PeerCard;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Runs one {@link MeshPeer} driven by hand, without a socket: the test hands it datagrams from any source address (a spoofed one too),
 * reads every datagram it sends and moves its manual clock round by round.
 */
final class DirectPeerScenario {
    private static final long RANDOM_SEED = 20261110L;
    private static final int PORT = 7455;
    private static final int TIMESTAMP_OFFSET = 4 + 1 + 1 + 1 + Integer.BYTES;
    private static final int NONCE_OFFSET = TIMESTAMP_OFFSET + Long.BYTES;
    private static final int HOSTS_PER_NETWORK = 250;

    /**
     * A datagram the peer sent.
     */
    static final class Sent {
        private final byte @NotNull [] datagram;
        private final @NotNull InetSocketAddress target;

        private Sent(byte @NotNull [] datagram, @NotNull InetSocketAddress target) {
            this.datagram = datagram;
            this.target   = target;
        }

        @NotNull InetSocketAddress getTarget() {
            return this.target;
        }

        int getLength() {
            return this.datagram.length;
        }

        @NotNull Envelope getEnvelope() {
            Envelope envelope = Envelope.decode(this.datagram, this.datagram.length).getEnvelope();

            Assertions.assertNotNull(envelope, "the peer sent a datagram it cannot decode");

            return envelope;
        }
    }

    /**
     * A remote sender with its own key, a card with a valid proof and an address of its own.
     */
    final class Sender {
        private final @NotNull IdentityKeyPair keyPair;
        private final @NotNull MessageEncoder encoder;
        private final @NotNull PeerCard card;
        private final @NotNull InetSocketAddress address;

        private Sender() {
            this.keyPair = IdentityKeyPair.generate(new SecureRandom());
            this.encoder = new MessageEncoder(this.keyPair, DirectPeerScenario.this.clock, DirectPeerScenario.this.random.split());
            this.card    = new PeerCard(PeerId.solveProof(this.keyPair.getPublicKey(), DirectPeerScenario.this.configuration.getProofDifficulty()),
                                        true, CoordinateReport.UNKNOWN);
            this.address = DirectPeerScenario.this.createAddress();
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

        @NotNull PeerCard getCard() {
            return this.card;
        }

        @NotNull InetSocketAddress getAddress() {
            return this.address;
        }

        /**
         * Gives a changed datagram of the sender the current time and a new nonce and signs it again, so that the peer's checks pass up to
         * the body.
         */
        byte @NotNull [] resign(byte @NotNull [] datagram, long nonce) {
            int signatureOffset = datagram.length - IdentityKey.SIGNATURE_LENGTH;
            long timestampMillis = DirectPeerScenario.this.clock.getEpochMillis();

            for (int index = 0; index < Long.BYTES; index++) {
                int shift = Long.SIZE - Byte.SIZE * (index + 1);

                datagram[TIMESTAMP_OFFSET + index] = (byte) (timestampMillis >>> shift);
                datagram[NONCE_OFFSET + index]     = (byte) (nonce >>> shift);
            }

            System.arraycopy(this.keyPair.sign(datagram, 0, signatureOffset), 0, datagram, signatureOffset, IdentityKey.SIGNATURE_LENGTH);

            return datagram;
        }
    }


    private final @NotNull PeerConfiguration configuration;
    private final @NotNull ManualProtocolClock clock;
    private final @NotNull SplittableRandom random;
    private final @NotNull List<@NotNull Sent> sent;
    private final @NotNull MeshPeer peer;

    private int nextHost;

    DirectPeerScenario(@NotNull PeerConfiguration configuration) {
        this.configuration = configuration;
        this.clock         = new ManualProtocolClock();
        this.random        = new SplittableRandom(RANDOM_SEED);
        this.sent          = new ArrayList<>();
        this.peer          = new MeshPeer(configuration, IdentityKeyPair.generate(new SecureRandom()), this.clock, this.random.split(),
                                          (datagram, target) -> this.sent.add(new Sent(datagram, target)));

        this.nextHost = 0;
    }

    @NotNull ManualProtocolClock getClock() {
        return this.clock;
    }

    @NotNull MeshPeer getPeer() {
        return this.peer;
    }

    @NotNull List<@NotNull Sent> getSent() {
        return this.sent;
    }

    /**
     * Creates a public address of its own network for a remote host.
     */
    @NotNull InetSocketAddress createAddress() {
        this.nextHost++;

        String host = "198.51." + this.nextHost / HOSTS_PER_NETWORK + "." + (1 + this.nextHost % HOSTS_PER_NETWORK);

        return new InetSocketAddress(InetAddress.ofLiteral(host), PORT);
    }

    @NotNull Sender createSender() {
        return new Sender();
    }

    void receive(byte @NotNull [] datagram, @NotNull InetSocketAddress source) {
        this.peer.receive(datagram, datagram.length, source);
    }

    /**
     * Counts the bytes the peer sent to an address so far.
     */
    int countBytesTo(@NotNull InetSocketAddress target) {
        int bytes = 0;

        for (Sent datagram : this.sent) {
            if (datagram.getTarget().equals(target)) {
                bytes += datagram.getLength();
            }
        }

        return bytes;
    }

    /**
     * Moves the clock by one gossip interval and runs the peer's maintenance.
     */
    void advanceRound() {
        this.clock.advance(this.configuration.getGossipIntervalMillis());
        this.peer.maintain();
    }
}
