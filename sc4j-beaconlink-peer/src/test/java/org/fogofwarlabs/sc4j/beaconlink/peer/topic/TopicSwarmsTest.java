package org.fogofwarlabs.sc4j.beaconlink.peer.topic;

import org.fogofwarlabs.sc4j.beaconlink.peer.DatagramSink;
import org.fogofwarlabs.sc4j.beaconlink.peer.DropReason;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.peer.limit.AddressTokens;
import org.fogofwarlabs.sc4j.beaconlink.peer.limit.Graylist;
import org.fogofwarlabs.sc4j.beaconlink.peer.routing.PeerRouting;
import org.fogofwarlabs.sc4j.beaconlink.peer.store.CommunityStore;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.ChatLineRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateEstimator;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PeerId;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.*;
import org.fogofwarlabs.sc4j.beaconlink.protocol.test.ManualProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Tests the penalties and the author rates of the topic layer on one peer driven by hand: penalties of an unproved source graylist only its
 * key at that address, a mesh neighbour is graylisted with its key and address, forged records do not spend their author's rate, a backlog
 * passes without penalties, and the mesh keeps members of the peer's own choice when strangers graft it first.
 */
final class TopicSwarmsTest {
    private static final long RANDOM_SEED = 20261110L;
    private static final int PROOF_DIFFICULTY = 4;
    private static final int STAMP_DIFFICULTY = 4;
    private static final int PORT = 7455;
    private static final int PENALTY_DATAGRAMS = 40;
    private static final int FORGED_LINES = 3;
    private static final int OVER_RATE_LINES = 3;
    private static final int BACKLOG_STEP_MILLIS = 5_000;
    private static final long BACKLOG_START_MILLIS = 200_000L;
    private static final long ROUND_MILLIS = 5_000L;
    private static final int LONELY_ROUNDS = 7;
    private static final @NotNull TopicKey CHANNEL = TopicKey.ofChannel("General");
    private static final @NotNull String NAME = "Author";

    /**
     * A remote peer: its key, card, address and encoder; it sends messages to the tested peer from its address or from another one.
     */
    private final class Remote {
        private final @NotNull IdentityKeyPair keyPair;
        private final @NotNull MessageEncoder encoder;
        private final @NotNull PeerCard card;
        private final @NotNull InetSocketAddress address;

        private long nextRequestNumber;

        private Remote() {
            this.keyPair = IdentityKeyPair.generate(new SecureRandom());
            this.encoder = new MessageEncoder(this.keyPair, TopicSwarmsTest.this.clock, TopicSwarmsTest.this.random.split());
            this.card    = new PeerCard(PeerId.solveProof(this.keyPair.getPublicKey(), PROOF_DIFFICULTY), true, CoordinateReport.UNKNOWN);
            this.address = TopicSwarmsTest.this.createAddress();

            this.nextRequestNumber = 1L;
        }

        private @NotNull IdentityKey getKey() {
            return this.keyPair.getPublicKey();
        }

        private @Nullable DropReason send(@NotNull Message message) {
            return this.send(message, this.address);
        }

        private @Nullable DropReason send(@NotNull Message message, @NotNull InetSocketAddress source) {
            byte[] datagram = this.encoder.encodeRequest(message, this.nextRequestNumber++);
            Envelope envelope = Envelope.decode(datagram, datagram.length).getEnvelope();

            Assertions.assertNotNull(envelope);

            return TopicSwarmsTest.this.topics.handle(envelope, source, TopicSwarmsTest.this.clock.getEpochMillis());
        }

        /**
         * Sends records in as few {@code GOSSIP} messages as they fit in.
         */
        private void gossip(@NotNull List<? extends @NotNull TopicRecord> records) {
            List<TopicRecord> batch = new ArrayList<>();
            int bytes = 0;

            for (TopicRecord record : records) {
                int length = GossipMessage.measure(record);

                if (bytes + length > GossipMessage.MAXIMUM_RECORD_BYTES) {
                    Assertions.assertNull(this.send(new GossipMessage(CHANNEL, batch)));
                    batch = new ArrayList<>();
                    bytes = 0;
                }

                batch.add(record);
                bytes += length;
            }

            Assertions.assertNull(this.send(new GossipMessage(CHANNEL, batch)));
        }

        private @NotNull ResultStatus graft(@NotNull TopicKey topic) {
            byte[] token = TopicSwarmsTest.this.addressTokens.create(this.address, this.getKey(), TopicSwarmsTest.this.clock.getEpochMillis());

            Assertions.assertNull(this.send(new GraftMessage(topic, this.card, token)));

            ResultStatus status = null;

            for (Sent sent : TopicSwarmsTest.this.sent) {
                if (sent.target.equals(this.address) && sent.envelope.getBody() instanceof ResultMessage result) {
                    status = result.getStatus();
                }
            }

            Assertions.assertNotNull(status);

            return status;
        }
    }

    /**
     * A datagram the tested peer sent.
     */
    private static final class Sent {
        private final @NotNull Envelope envelope;
        private final @NotNull InetSocketAddress target;

        private Sent(@NotNull Envelope envelope, @NotNull InetSocketAddress target) {
            this.envelope = envelope;
            this.target   = target;
        }
    }

    private static @NotNull ChatLineRecord forge(@NotNull ChatLineRecord genuine) {
        WireWriter writer = new WireWriter(ChatLineRecord.MAXIMUM_ENCODED_LENGTH);

        genuine.write(writer);

        byte[] bytes = writer.toByteArray();

        bytes[bytes.length - 1] ^= 1;

        ChatLineRecord forged = ChatLineRecord.read(new WireReader(bytes, 0, bytes.length));

        Assertions.assertNotNull(forged);

        return forged;
    }

    private static @NotNull List<@NotNull ChatLineRecord> signLines(@NotNull IdentityKeyPair author, @NotNull PlayerStamp stamp, int count,
                                                                    long firstTimestampMillis, long stepMillis) {
        List<ChatLineRecord> lines = new ArrayList<>();

        for (int line = 0; line < count; line++) {
            lines.add(ChatLineRecord.sign(author, stamp, CHANNEL, line, firstTimestampMillis + line * stepMillis, false, "line " + line));
        }

        return lines;
    }


    private final @NotNull ManualProtocolClock clock;
    private final @NotNull SplittableRandom random;
    private final @NotNull List<@NotNull Sent> sent;
    private final @NotNull AddressTokens addressTokens;
    private final @NotNull Graylist graylist;
    private final @NotNull TopicSwarms topics;

    private int nextHost;

    TopicSwarmsTest() {
        PeerConfiguration configuration = new PeerConfiguration.Builder().setProofDifficulty(PROOF_DIFFICULTY)
                                                                         .setPlayerStampDifficulty(STAMP_DIFFICULTY).build();
        IdentityKeyPair own = IdentityKeyPair.generate(new SecureRandom());
        byte[] secret = new byte[AddressTokens.SECRET_LENGTH];

        this.clock  = new ManualProtocolClock();
        this.random = new SplittableRandom(RANDOM_SEED);
        this.sent   = new ArrayList<>();

        this.random.nextBytes(secret);

        MessageEncoder encoder = new MessageEncoder(own, this.clock, this.random.split());
        DatagramSink sink = (datagram, target) -> {
            Envelope envelope = Envelope.decode(datagram, datagram.length).getEnvelope();

            Assertions.assertNotNull(envelope);
            this.sent.add(new Sent(envelope, target));
        };
        PeerRouting routing = new PeerRouting(configuration, encoder, PeerId.solveProof(own.getPublicKey(), PROOF_DIFFICULTY),
                                              new CoordinateEstimator(this.random.split()), () -> true, sink, this.random.split(), "test",
                                              reason -> {
                                              });

        this.addressTokens = new AddressTokens(secret);
        this.graylist      = new Graylist(configuration.getRateLimiterCapacity(), configuration.getPenaltyLimit(),
                                          configuration.getPenaltyMemoryMillis(), configuration.getGraylistMillis());
        this.topics        = new TopicSwarms(configuration, encoder, this.clock, routing, this.addressTokens, this.graylist, sink,
                                             this.random.split(), reason -> {
        }, signOut -> {
        });

        this.nextHost = 0;
    }

    private @NotNull InetSocketAddress createAddress() {
        this.nextHost++;

        return new InetSocketAddress(InetAddress.ofLiteral("192.0.2." + this.nextHost), PORT);
    }

    private @NotNull CommunityStore joinChannel() {
        CommunityStore store = this.topics.useCommunity(CHANNEL, this.clock.getEpochMillis());

        Assertions.assertNotNull(store);

        return store;
    }

    private long now() {
        return this.clock.getEpochMillis();
    }

    @Test
    void penalties_of_a_spoofed_source_graylist_only_the_spoofing_key_at_that_address() {
        Remote attacker = new Remote();
        InetSocketAddress victim = this.createAddress();

        this.joinChannel();

        for (int datagram = 0; datagram < PENALTY_DATAGRAMS; datagram++) {
            Assertions.assertEquals(DropReason.NOT_IN_TOPIC, attacker.send(new IHaveMessage(CHANNEL, List.of()), victim));
        }

        Assertions.assertTrue(this.graylist.isListed(attacker.getKey(), victim, this.now()), "the key at the spoofed address is graylisted");
        Assertions.assertFalse(this.graylist.isListed(victim, this.now()), "the spoofed address is not graylisted");
        Assertions.assertFalse(this.graylist.isListed(attacker.getKey(), this.now()), "the key alone is not graylisted");
    }

    @Test
    void forwarded_datagrams_of_a_neighbour_do_not_graylist_its_key() {
        Remote neighbour = new Remote();
        Remote forwarder = new Remote();

        this.joinChannel();

        Assertions.assertEquals(ResultStatus.ACCEPTED, neighbour.graft(CHANNEL));

        for (int datagram = 0; datagram < PENALTY_DATAGRAMS; datagram++) {
            Assertions.assertEquals(DropReason.NOT_IN_TOPIC, neighbour.send(new IHaveMessage(CHANNEL, List.of()), forwarder.address));
        }

        Assertions.assertFalse(this.graylist.isListed(neighbour.getKey(), this.now()), "the neighbour's key is not graylisted");
        Assertions.assertFalse(this.graylist.isListed(neighbour.getKey(), neighbour.address, this.now()));
        Assertions.assertNull(neighbour.send(new IHaveMessage(CHANNEL, List.of())), "the neighbour is still heard at its address");
    }

    @Test
    void a_neighbour_that_sends_forged_records_is_graylisted_with_its_key_and_address() {
        Remote neighbour = new Remote();
        IdentityKeyPair author = IdentityKeyPair.generate(new SecureRandom());
        PlayerStamp stamp = PlayerStamp.solve(author.getPublicKey(), NAME, STAMP_DIFFICULTY);
        List<TopicRecord> forged = new ArrayList<>();

        this.joinChannel();

        Assertions.assertEquals(ResultStatus.ACCEPTED, neighbour.graft(CHANNEL));

        for (ChatLineRecord line : signLines(author, stamp, PENALTY_DATAGRAMS / TopicSwarms.INVALID_RECORD_POINTS, this.now(), 1L)) {
            forged.add(forge(line));
        }

        neighbour.gossip(forged);

        Assertions.assertTrue(this.graylist.isListed(neighbour.getKey(), this.now()));
        Assertions.assertTrue(this.graylist.isListed(neighbour.address, this.now()));
    }

    @Test
    void forged_records_do_not_spend_the_rate_of_the_author_they_name() {
        Remote forger = new Remote();
        Remote relay = new Remote();
        IdentityKeyPair author = IdentityKeyPair.generate(new SecureRandom());
        PlayerStamp stamp = PlayerStamp.solve(author.getPublicKey(), NAME, STAMP_DIFFICULTY);
        CommunityStore store = this.joinChannel();
        int burst = PeerConfiguration.createDefault().getChatBurst();
        List<ChatLineRecord> genuine = signLines(author, stamp, burst + FORGED_LINES, this.now(), 1L);
        List<TopicRecord> forged = new ArrayList<>();

        Assertions.assertEquals(ResultStatus.ACCEPTED, forger.graft(CHANNEL));
        Assertions.assertEquals(ResultStatus.ACCEPTED, relay.graft(CHANNEL));

        for (ChatLineRecord line : genuine.subList(burst, burst + FORGED_LINES)) {
            forged.add(forge(line));
        }

        forger.gossip(forged);
        relay.gossip(genuine.subList(0, burst));

        Assertions.assertEquals(burst, store.getLines().size(), "the author's whole burst passes");
    }

    @Test
    void a_backlog_of_one_author_passes_without_penalties_while_new_lines_keep_the_burst() {
        Remote neighbour = new Remote();
        IdentityKeyPair author = IdentityKeyPair.generate(new SecureRandom());
        PlayerStamp stamp = PlayerStamp.solve(author.getPublicKey(), NAME, STAMP_DIFFICULTY);
        CommunityStore store = this.joinChannel();
        PeerConfiguration defaults = PeerConfiguration.createDefault();
        int backlog = defaults.getChatBacklog();
        int burst = defaults.getChatBurst();

        Assertions.assertEquals(ResultStatus.ACCEPTED, neighbour.graft(CHANNEL));

        List<ChatLineRecord> history = signLines(author, stamp, backlog, this.now() - BACKLOG_START_MILLIS, BACKLOG_STEP_MILLIS);

        neighbour.gossip(history);

        Assertions.assertEquals(backlog, store.getLines().size(), "the whole backlog of one author passes");
        Assertions.assertFalse(this.graylist.isListed(neighbour.getKey(), neighbour.address, this.now()), "the neighbour is not penalized");

        List<ChatLineRecord> live = new ArrayList<>();

        for (int line = 0; line < burst + OVER_RATE_LINES; line++) {
            live.add(ChatLineRecord.sign(author, stamp, CHANNEL, backlog + line, this.now(), false, "new " + line));
        }

        neighbour.gossip(live);

        long kept = store.getLines().stream().filter(line -> line.getText().startsWith("new ")).count();

        Assertions.assertEquals(burst, kept, "new lines keep the chat burst");
    }

    @Test
    void a_mesh_grafted_by_strangers_first_keeps_room_for_members_of_the_peers_own_choice() {
        PeerConfiguration defaults = PeerConfiguration.createDefault();
        int maximum = defaults.getMaximumMeshDegree();
        List<Remote> grafters = new ArrayList<>();

        this.joinChannel();

        for (int index = 0; index < maximum; index++) {
            Remote grafter = new Remote();

            if (grafter.graft(CHANNEL) == ResultStatus.ACCEPTED) {
                grafters.add(grafter);
            }
        }

        Assertions.assertTrue(grafters.size() < maximum, "grafts of others leave room for the peer's own: " + grafters.size());

        // A member announces itself here; the swarm takes it in when it joins again and grafts it, although the mesh holds the others.
        Remote chosen = new Remote();
        byte[] token = this.addressTokens.create(chosen.address, chosen.getKey(), this.now());

        Assertions.assertNull(chosen.send(new AnnounceMessage(CHANNEL, chosen.card, token)));
        this.sent.clear();

        for (int round = 0; round < LONELY_ROUNDS; round++) {
            this.clock.advance(ROUND_MILLIS);
            this.topics.runRound(this.now());

            for (Remote grafter : grafters) {
                Assertions.assertNull(grafter.send(new IHaveMessage(CHANNEL, List.of())), "a neighbour stays");
            }
        }

        boolean asked = this.sent.stream().anyMatch(sent -> sent.target.equals(chosen.address) && sent.envelope.getBody() instanceof GetTopicMessage);

        Assertions.assertTrue(asked, "the peer grafts a member of its own choice");
    }
}
