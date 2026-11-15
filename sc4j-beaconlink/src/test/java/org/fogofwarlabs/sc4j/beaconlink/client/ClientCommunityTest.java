package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.beaconlink.channel.EphemeralKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.channel.WhisperSealUtils;
import org.fogofwarlabs.sc4j.beaconlink.peer.DropReason;
import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.community.ChatLineRecord;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.SealedMessage;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.fogofwarlabs.sc4j.beaconlink.test.client.ClientScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.nat.NatType;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.CommunityScenario;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.SimulatedClient;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.SimulatedPeer;
import org.fogofwarlabs.sc4j.beaconlink.test.simulation.TopicFlooder;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.*;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * Tests the community part of the client on the in-memory network: chat lines reach every member of a channel once through the members'
 * home nodes, members show their mod, a late member gets a bounded backlog, a forged line and lines over their author's rate are dropped by
 * the relays, a whisper reaches its target sealed so that the home peers that carry it cannot read it, a joiner registered at other nodes
 * than a room's host joins through the host's home nodes, a friend of another mod is followed in its player topic, and a player who appears
 * offline leaves the member lists while its rooms still work. Over real nodes on 127.0.0.1: a friend sees a player on several connections
 * merged and never offline while the player moves from the launcher to a game and back, a whisper reaches every connection of its target and
 * is delivered while one is reachable, signing out everywhere else ends the older connections, which the node then refuses, while later
 * ones work, and a friend and the kept connection drop a signed-out connection at once, not when its presence expires.
 */
final class ClientCommunityTest {
    private static final @NotNull Logger LOGGER = Logger.of(ClientCommunityTest.class);
    private static final long RANDOM_SEED = 20261111L;
    private static final double LOSS_RATE = 0.01;
    private static final int PEER_COUNT = 15;
    private static final int MEMBER_COUNT = 6;
    private static final int NODES_PER_CLIENT = 2;
    private static final int LINES_PER_MEMBER = 2;
    private static final int BACKLOG_LINES = 40;
    private static final int BACKLOG = 32;
    private static final int SPAM_LINES = 10;
    private static final int CHAT_BURST = 5;
    private static final long STEP_MILLIS = 500L;
    private static final long JOIN_MILLIS = 30_000L;
    private static final long DELIVERY_MILLIS = 15_000L;
    private static final long LINE_INTERVAL_MILLIS = 1_100L;
    private static final long GRAFT_MILLIS = 2_000L;
    private static final long CONNECT_MILLIS = 30_000L;
    private static final long QUIET_MILLIS = 20_000L;
    private static final @NotNull String CHANNEL = "General";
    private static final @NotNull String WHISPER_TEXT = "meet me in the room at nine";
    private static final @NotNull RoomSettings SETTINGS = new RoomSettings("Evening game", "Ashen Ford", 1, 4);
    private static final @Nullable NatType @NotNull [] NAT_MIX = {null, NatType.PORT_RESTRICTED, NatType.ADDRESS_RESTRICTED, NatType.SYMMETRIC};
    private static final long SETTLE_MILLIS = 60_000L;
    private static final long UNREGISTER_MILLIS = 2_000L;
    private static final long BEHIND_MILLIS = 1_000L;
    /**
     * A short wait, well under the presence lifetime (120 s) and longer than a leaving presence lives (5 s).
     */
    private static final long SIGN_OUT_MILLIS = 10_000L;
    private static final @NotNull ModIdentity GAME = ClientScenario.createIdentity("1.0.0");
    private static final @NotNull ModIdentity LAUNCHER = ClientScenario.createIdentity(EngineVersion.CURRENT, BeaconLink.LAUNCHER_MOD_ID, "1.0.0");

    /**
     * Counts the lines and whispers a client's listener gets.
     */
    private static final class Recorder implements CommunityListener {
        private final @NotNull Map<@NotNull String, @NotNull Integer> lines;
        private final @NotNull List<@NotNull Whisper> whispers;
        private final @NotNull Map<@NotNull String, @NotNull WhisperResult> results;

        private Recorder() {
            this.lines    = new HashMap<>();
            this.whispers = new ArrayList<>();
            this.results  = new HashMap<>();
        }

        @Override
        public void onChatLine(@NotNull ChatChannel channel, @NotNull ChatLine line) {
            this.lines.merge(line.getText(), 1, Integer::sum);
        }

        @Override
        public void onWhisper(@NotNull Whisper whisper) {
            this.whispers.add(whisper);
        }

        @Override
        public void onWhisperResult(@NotNull Whisper whisper, @NotNull WhisperResult result) {
            this.results.put(whisper.getText(), result);
        }
    }

    private static boolean isRegistered(@NotNull BeaconLinkSession session) {
        for (LinkNode node : session.getNodes()) {
            if (node.getState() == LinkNodeState.REGISTERED) {
                return true;
            }
        }

        return false;
    }

    private static @NotNull List<@NotNull ChatChannel> joinAll(@NotNull CommunityScenario scenario, @NotNull List<@NotNull SimulatedClient> clients) {
        List<ChatChannel> channels = new ArrayList<>();

        for (SimulatedClient client : clients) {
            channels.add(client.getClient().getCommunity().joinChannel(CHANNEL));
        }

        scenario.runUntil(() -> channels.stream().allMatch(channel -> channel.getMembers().size() == clients.size()), JOIN_MILLIS, STEP_MILLIS,
                          "every member sees every member");

        return channels;
    }

    private static @NotNull List<@NotNull SimulatedClient> addMembers(@NotNull CommunityScenario scenario, int count) {
        List<SimulatedClient> members = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            members.add(scenario.addClient("Member" + index, NAT_MIX[index % NAT_MIX.length], NODES_PER_CLIENT));
        }

        return members;
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

    private static boolean contains(byte @NotNull [] data, byte @NotNull [] part) {
        for (int start = 0; start + part.length <= data.length; start++) {
            boolean equal = true;

            for (int index = 0; index < part.length && equal; index++) {
                equal = data[start + index] == part[index];
            }

            if (equal) {
                return true;
            }
        }

        return false;
    }

    private static boolean isShown(@Nullable PlayerPresence presence, @NotNull PlayerStatus status, @NotNull String modId, int deviceCount) {
        return presence != null && presence.getStatus() == status && presence.getModId().equals(modId) && presence.getDeviceCount() == deviceCount;
    }

    @Test
    void lines_reach_every_member_once_and_members_show_their_mod() {
        CommunityScenario scenario = new CommunityScenario(RANDOM_SEED, LOSS_RATE, PEER_COUNT);
        List<SimulatedClient> members = addMembers(scenario, MEMBER_COUNT - 1);
        SimulatedClient otherVersion = scenario.addClient("Other", null, ClientScenario.createIdentity("1.1.0"),
                                                          List.of(scenario.getPeers().getLast().getAddress()));
        List<Recorder> recorders = new ArrayList<>();

        members.add(otherVersion);

        for (SimulatedClient member : members) {
            Recorder recorder = new Recorder();

            member.getClient().getCommunity().addListener(recorder);
            recorders.add(recorder);
        }

        List<ChatChannel> channels = joinAll(scenario, members);

        for (int line = 0; line < LINES_PER_MEMBER; line++) {
            for (int index = 0; index < channels.size(); index++) {
                Assertions.assertTrue(channels.get(index).say("line " + line + " of member " + index));
            }

            scenario.run(LINE_INTERVAL_MILLIS);
        }

        int expected = MEMBER_COUNT * LINES_PER_MEMBER;

        scenario.runUntil(() -> recorders.stream().allMatch(recorder -> recorder.lines.size() == expected), DELIVERY_MILLIS, STEP_MILLIS,
                          "every member gets every line");

        for (Recorder recorder : recorders) {
            Assertions.assertTrue(recorder.lines.values().stream().allMatch(count -> count == 1), "a line is handed out once: " + recorder.lines);
        }

        CommunitySession first = members.getFirst().getClient().getCommunity();
        PlayerPresence other = first.getPresence(otherVersion.getClient().getParticipant().getPlayer());
        PlayerPresence second = first.getPresence(members.get(1).getClient().getParticipant().getPlayer());

        Assertions.assertNotNull(other);
        Assertions.assertNotNull(second);
        Assertions.assertEquals("beacon-test", other.getModId());
        Assertions.assertEquals("1.1.0", other.getModVersion());
        Assertions.assertFalse(other.canPlayTogether(), "another mod version cannot play together");
        Assertions.assertTrue(second.canPlayTogether());
        Assertions.assertEquals(CHANNEL, second.getChannel());
        Assertions.assertEquals("Member1#" + members.get(1).getClient().getParticipant().getPlayer().getTag(), second.getTaggedName());
        Assertions.assertEquals(first.getSelf().getPlayer(), channels.getFirst().getMembers().getFirst().getPlayer(), "the own player first");
        Assertions.assertEquals(expected, channels.getFirst().getLines().size());

        ChannelSummary summary = first.listChannels().getFirst();

        Assertions.assertEquals(CHANNEL, summary.getName());
        Assertions.assertEquals(MEMBER_COUNT, summary.getMemberCount());
    }

    @Test
    void a_late_member_gets_a_bounded_backlog() {
        CommunityScenario scenario = new CommunityScenario(RANDOM_SEED + 1L, LOSS_RATE, PEER_COUNT);
        List<SimulatedClient> members = addMembers(scenario, 2);

        // Clients that answer at once choose other nodes than with tick polling: the late member's nodes join the channel only for it and
        // pull the backlog from their neighbours.
        for (SimulatedClient member : members) {
            member.setPromptPolling(true);
        }

        ChatChannel speaker = joinAll(scenario, members).getFirst();

        for (int line = 0; line < BACKLOG_LINES; line++) {
            Assertions.assertTrue(speaker.say("backlog line " + line));
            scenario.run(LINE_INTERVAL_MILLIS);
        }

        scenario.run(DELIVERY_MILLIS);

        SimulatedClient late = scenario.addClient("Late", NatType.PORT_RESTRICTED, NODES_PER_CLIENT);

        late.setPromptPolling(true);

        ChatChannel lateChannel = late.getClient().getCommunity().joinChannel(CHANNEL);

        scenario.runUntil(() -> lateChannel.getLines().size() >= BACKLOG, JOIN_MILLIS, STEP_MILLIS, "the late member gets the backlog");
        scenario.run(DELIVERY_MILLIS);

        List<ChatLine> lines = lateChannel.getLines();

        Assertions.assertEquals(BACKLOG, lines.size(), "the backlog holds the newest lines only");
        Assertions.assertEquals("backlog line " + (BACKLOG_LINES - BACKLOG), lines.getFirst().getText());
        Assertions.assertEquals("backlog line " + (BACKLOG_LINES - 1), lines.getLast().getText());
        Assertions.assertEquals(members.size() + 1, lateChannel.getMembers().size(), "the presences come with the backlog");
    }

    @Test
    void a_forged_line_and_lines_over_the_author_rate_are_dropped() {
        CommunityScenario scenario = new CommunityScenario(RANDOM_SEED + 2L, LOSS_RATE, PEER_COUNT);
        SimulatedPeer victim = scenario.getPeers().get(PEER_COUNT / 2);
        SimulatedClient listener = scenario.addClient("Listener", NatType.PORT_RESTRICTED, scenario.getIdentity(), List.of(victim.getAddress()));
        Recorder recorder = new Recorder();
        TopicKey topic = TopicKey.ofChannel(CHANNEL);

        listener.getClient().getCommunity().addListener(recorder);
        joinAll(scenario, List.of(listener));
        scenario.runUntil(() -> victim.getPeer().isInTopic(topic), JOIN_MILLIS, STEP_MILLIS, "the victim holds the channel's topic");

        TopicFlooder flooder = new TopicFlooder(scenario.getNetwork(), topic, victim.getAddress(), CommunityScenario.PROOF_DIFFICULTY);

        flooder.join();
        scenario.run(GRAFT_MILLIS);

        Assertions.assertTrue(flooder.isGrafted(), "the flooder joins the victim's mesh as a member would");

        IdentityKeyPair spammer = scenario.getNetwork().createKeyPair();
        PlayerStamp stamp = PlayerStamp.solve(spammer.getPublicKey(), "Spammer", CommunityScenario.STAMP_DIFFICULTY);
        long nowMillis = scenario.getNetwork().getClock().getEpochMillis();
        List<ChatLineRecord> spam = new ArrayList<>();

        for (int line = 0; line < SPAM_LINES; line++) {
            spam.add(ChatLineRecord.sign(spammer, stamp, topic, line, nowMillis, false, "spam " + line));
        }

        ChatLineRecord genuine = ChatLineRecord.sign(spammer, stamp, topic, SPAM_LINES, nowMillis, false, "forged");

        spam.add(forge(genuine));
        flooder.flood(spam);
        scenario.run(DELIVERY_MILLIS);

        MeshPeer victimPeer = victim.getPeer();

        LOGGER.info("Spam: the listener got {0}; the victim dropped {1} records", recorder.lines.keySet(),
                    victimPeer.getStatistics().getDroppedCount(DropReason.INVALID_RECORD));

        Assertions.assertEquals(CHAT_BURST, recorder.lines.size(), "only the author's burst passes the relay");
        Assertions.assertFalse(recorder.lines.containsKey("forged"), "the forged line is dropped");
        Assertions.assertEquals(SPAM_LINES - CHAT_BURST + 1, victimPeer.getStatistics().getDroppedCount(DropReason.INVALID_RECORD));
    }

    @Test
    void a_whisper_reaches_its_target_sealed_from_its_home_peers() {
        CommunityScenario scenario = new CommunityScenario(RANDOM_SEED + 3L, LOSS_RATE, PEER_COUNT);
        List<SimulatedClient> members = addMembers(scenario, 2);
        SimulatedClient sender = members.getFirst();
        SimulatedClient target = members.getLast();
        Recorder senderRecorder = new Recorder();
        Recorder targetRecorder = new Recorder();
        Set<InetSocketAddress> targetNodes = new HashSet<>();
        List<SealedMessage> relayed = new ArrayList<>();

        sender.getClient().getCommunity().addListener(senderRecorder);
        target.getClient().getCommunity().addListener(targetRecorder);
        joinAll(scenario, members);

        for (NodeEndpoint homeNode : target.getClient().getHomeEndpoints()) {
            targetNodes.add(homeNode.getAddress());
        }

        scenario.getNetwork().setTap((datagram, source, destination) -> {
            Envelope envelope = Envelope.decode(datagram, datagram.length).getEnvelope();

            if (targetNodes.contains(destination) && envelope != null && envelope.getBody() instanceof SealedMessage sealed) {
                relayed.add(sealed);
            }
        });

        LinkPlayer stranger = LinkPlayer.of(scenario.getNetwork().createKeyPair().getPublicKey().getBytes());

        sender.getClient().getCommunity().whisper(target.getClient().getParticipant().getPlayer(), WHISPER_TEXT);
        sender.getClient().getCommunity().whisper(stranger, "nobody hears this");
        scenario.runUntil(() -> senderRecorder.results.size() == 2, DELIVERY_MILLIS, STEP_MILLIS, "both whispers end");

        Assertions.assertEquals(WhisperResult.DELIVERED, senderRecorder.results.get(WHISPER_TEXT));
        Assertions.assertEquals(WhisperResult.NOT_ONLINE, senderRecorder.results.get("nobody hears this"));
        Assertions.assertEquals(1, targetRecorder.whispers.size(), "the target gets the whisper once");
        Assertions.assertEquals(WHISPER_TEXT, targetRecorder.whispers.getFirst().getText());
        Assertions.assertEquals(sender.getClient().getParticipant().getPlayer(), targetRecorder.whispers.getFirst().getOther());
        Assertions.assertFalse(relayed.isEmpty(), "the whisper passes the target's home peers");

        byte[] plaintext = WHISPER_TEXT.getBytes(StandardCharsets.UTF_8);
        IdentityKey senderKey = sender.getClient().getKey();
        IdentityKey targetKey = target.getClient().getKey();

        for (SealedMessage sealed : relayed) {
            Assertions.assertFalse(contains(sealed.getSealed(), plaintext), "a home peer sees the text");
            Assertions.assertNull(WhisperSealUtils.open(EphemeralKeyPair.generate(new SecureRandom()), senderKey, targetKey, sealed),
                                  "another key opens the whisper");
        }
    }

    @Test
    void a_friend_never_sees_the_player_offline_while_it_moves_from_the_launcher_to_a_game_and_back() throws IOException {
        try (ClientScenario scenario = new ClientScenario()) {
            PeerSocket node = scenario.openNode(ClientScenario.createNodeConfiguration());
            List<ConfiguredNode> listed = List.of(ClientScenario.listed(node));
            BeaconLinkClient launcher = scenario.openClient(LAUNCHER, listed);
            BeaconLinkClient friend = scenario.openClient(GAME, listed);
            LinkPlayer player = launcher.getParticipant().getPlayer();
            CommunitySession launcherCommunity = launcher.getCommunity();
            CommunitySession friendCommunity = friend.getCommunity();
            boolean[] seenOffline = {false};

            friendCommunity.addFriend(player);
            scenario.advanceUntil(() -> isShown(friendCommunity.getPresence(player), PlayerStatus.AVAILABLE, BeaconLink.LAUNCHER_MOD_ID, 1),
                                  SETTLE_MILLIS, "the friend sees the player in the launcher");
            scenario.addPollAction(() -> seenOffline[0] |= friendCommunity.getPresence(player) == null);

            BeaconLinkClient game = scenario.openConnection(launcher, GAME, listed);

            game.getCommunity().setStatus(PlayerStatus.IN_GAME);
            scenario.advanceUntil(() -> isShown(friendCommunity.getPresence(player), PlayerStatus.IN_GAME, GAME.getModId(), 2), SETTLE_MILLIS,
                                  "the friend sees the player in a game on two devices");
            scenario.advanceUntil(() -> launcherCommunity.getSelf().getDeviceCount() == 2, SETTLE_MILLIS,
                                  "the launcher counts the game as the account's other device");

            Assertions.assertTrue(isShown(launcherCommunity.getPresence(player), PlayerStatus.IN_GAME, GAME.getModId(), 2),
                                  "the launcher shows its own account in the game");
            Assertions.assertEquals(PlayerStatus.AVAILABLE, launcherCommunity.getSelf().getStatus(), "the launcher's own connection is available");

            game.close();
            scenario.advanceUntil(() -> isShown(friendCommunity.getPresence(player), PlayerStatus.AVAILABLE, BeaconLink.LAUNCHER_MOD_ID, 1),
                                  SETTLE_MILLIS, "the friend sees the player back in the launcher");

            Assertions.assertFalse(seenOffline[0], "the friend never sees the player offline");
        }
    }

    @Test
    void a_whisper_reaches_every_connection_of_its_target_and_is_delivered_while_one_is_reachable() throws IOException {
        try (ClientScenario scenario = new ClientScenario()) {
            PeerSocket node = scenario.openNode(ClientScenario.createNodeConfiguration());
            List<ConfiguredNode> listed = List.of(ClientScenario.listed(node));
            BeaconLinkClient launcher = scenario.openClient(LAUNCHER, listed);
            BeaconLinkClient game = scenario.openConnection(launcher, GAME, listed);
            BeaconLinkClient sender = scenario.openClient(GAME, listed);
            LinkPlayer target = launcher.getParticipant().getPlayer();
            CommunitySession sending = sender.getCommunity();
            Recorder launcherRecorder = new Recorder();
            Recorder gameRecorder = new Recorder();
            Recorder senderRecorder = new Recorder();

            launcher.getCommunity().addListener(launcherRecorder);
            game.getCommunity().addListener(gameRecorder);
            sending.addListener(senderRecorder);
            sending.addFriend(target);
            scenario.advanceUntil(() -> {
                PlayerPresence presence = sending.getPresence(target);

                return presence != null && presence.getDeviceCount() == 2;
            }, SETTLE_MILLIS, "the sender sees both connections of the target");

            sending.whisper(target, "to both");
            scenario.advanceUntil(() -> senderRecorder.results.containsKey("to both") && launcherRecorder.whispers.size() == 1 &&
                                        gameRecorder.whispers.size() == 1, SETTLE_MILLIS, "the whisper reaches both connections");

            Assertions.assertEquals(WhisperResult.DELIVERED, senderRecorder.results.get("to both"));
            Assertions.assertEquals("to both", launcherRecorder.whispers.getFirst().getText());
            Assertions.assertEquals("to both", gameRecorder.whispers.getFirst().getText());

            for (NodeSession gameNode : game.getNodeSessions()) {
                gameNode.unregister();
            }

            scenario.advance(UNREGISTER_MILLIS);
            sending.whisper(target, "to one");
            scenario.advanceUntil(() -> senderRecorder.results.containsKey("to one") && launcherRecorder.whispers.size() == 2, SETTLE_MILLIS,
                                  "the whisper reaches the connection that is still registered");
            scenario.advance(UNREGISTER_MILLIS);

            Assertions.assertEquals(WhisperResult.DELIVERED, senderRecorder.results.get("to one"), "delivered while one connection is reachable");
            Assertions.assertEquals(1, gameRecorder.whispers.size(), "the connection the node no longer holds gets nothing");
        }
    }

    @Test
    void signing_out_everywhere_else_ends_the_older_connections_which_the_node_then_refuses() throws IOException {
        try (ClientScenario scenario = new ClientScenario()) {
            PeerSocket node = scenario.openNode(ClientScenario.createNodeConfiguration());
            List<ConfiguredNode> listed = List.of(ClientScenario.listed(node));
            BeaconLinkClient game = scenario.openClient(GAME, listed);
            BeaconLinkClient launcher = scenario.openConnection(game, LAUNCHER, listed);

            game.getCommunity();
            scenario.advanceUntil(() -> launcher.getCommunity().getSelf().getDeviceCount() == 2 &&
                                        game.getCommunity().getSelf().getDeviceCount() == 2,
                                  SETTLE_MILLIS, "both connections see each other in the account's topic");
            scenario.advance(ClientScenario.STEP_MILLIS);

            long signOutMillis = scenario.getClock().getEpochMillis();

            launcher.signOutOtherDevices();
            scenario.advanceUntil(() -> game.getFailure() == LinkFailure.SIGNED_OUT, SETTLE_MILLIS, "the game connection is signed out");

            Assertions.assertTrue(game.isSignedOut());
            Assertions.assertTrue(game.getNodes().stream().allMatch(gameNode -> gameNode.getState() == LinkNodeState.SIGNED_OUT),
                                  "the signed-out connection registers nowhere");
            Assertions.assertNull(launcher.getFailure(), "the connection that signed out the others stays");
            Assertions.assertTrue(isRegistered(launcher));
            Assertions.assertThrows(IllegalStateException.class, game::signOutOtherDevices);

            long behindMillis = scenario.getClock().getEpochMillis() - signOutMillis + BEHIND_MILLIS;
            ProtocolClock behind = () -> scenario.getClock().getEpochMillis() - behindMillis;
            BeaconLinkClient missed = scenario.openConnection(launcher, GAME, listed, behind);

            scenario.advanceUntil(() -> missed.getFailure() == LinkFailure.SIGNED_OUT, SETTLE_MILLIS,
                                  "a connection that started before the sign-out is refused by the node");

            Assertions.assertEquals(LinkNodeState.SIGNED_OUT, missed.getNodes().getFirst().getState());

            BeaconLinkClient later = scenario.openConnection(launcher, GAME, listed);

            scenario.advanceUntil(() -> isRegistered(later), SETTLE_MILLIS, "a game started after the sign-out registers");
            scenario.advance(UNREGISTER_MILLIS);

            Assertions.assertNull(later.getFailure(), "a game started after the sign-out works");
            Assertions.assertNull(launcher.getFailure());
        }
    }

    @Test
    void a_friend_and_the_kept_connection_drop_the_signed_out_connection_at_once() throws IOException {
        try (ClientScenario scenario = new ClientScenario()) {
            PeerSocket node = scenario.openNode(ClientScenario.createNodeConfiguration());
            List<ConfiguredNode> listed = List.of(ClientScenario.listed(node));
            BeaconLinkClient launcher = scenario.openClient(LAUNCHER, listed);
            BeaconLinkClient game = scenario.openConnection(launcher, GAME, listed);
            BeaconLinkClient friend = scenario.openClient(GAME, listed);
            LinkPlayer player = launcher.getParticipant().getPlayer();
            CommunitySession launcherCommunity = launcher.getCommunity();
            CommunitySession friendCommunity = friend.getCommunity();

            game.getCommunity();
            friendCommunity.addFriend(player);
            scenario.advanceUntil(() -> isShown(friendCommunity.getPresence(player), PlayerStatus.AVAILABLE, GAME.getModId(), 2) &&
                                        launcherCommunity.getSelf().getDeviceCount() == 2, SETTLE_MILLIS,
                                  "the friend and the launcher see both connections of the account");
            scenario.advance(ClientScenario.STEP_MILLIS);

            launcher.signOutOtherDevices();
            scenario.advanceUntil(() -> isShown(friendCommunity.getPresence(player), PlayerStatus.AVAILABLE, BeaconLink.LAUNCHER_MOD_ID, 1) &&
                                        launcherCommunity.getSelf().getDeviceCount() == 1, SIGN_OUT_MILLIS,
                                  "the friend and the launcher drop the signed-out game well before its presence expires");

            Assertions.assertTrue(game.isSignedOut());

            scenario.advance(SIGN_OUT_MILLIS);

            Assertions.assertTrue(isShown(friendCommunity.getPresence(player), PlayerStatus.AVAILABLE, BeaconLink.LAUNCHER_MOD_ID, 1),
                                  "the game's leaving presence does not bring it back");
            Assertions.assertEquals(1, launcherCommunity.getSelf().getDeviceCount());
        }
    }

    @Test
    void a_joiner_registered_at_other_nodes_joins_through_the_hosts_home_nodes() {
        CommunityScenario scenario = new CommunityScenario(RANDOM_SEED + 4L, LOSS_RATE, PEER_COUNT);
        List<SimulatedPeer> peers = scenario.getPeers();
        SimulatedClient host = scenario.addClient("Host", null, scenario.getIdentity(), List.of(peers.get(1).getAddress()));
        SimulatedClient joiner = scenario.addClient("Joiner", null, scenario.getIdentity(), List.of(peers.getLast().getAddress()));

        scenario.runUntil(() -> host.getClient().getObservedAddress() != null && joiner.getClient().getObservedAddress() != null, JOIN_MILLIS,
                          STEP_MILLIS, "both clients register");

        HostedRoom room = host.getClient().publishRoom(SETTINGS);
        FoundRoom[] found = new FoundRoom[1];

        scenario.runUntil(() -> {
            RoomSearch search = joiner.getClient().searchRooms();

            scenario.run(STEP_MILLIS * 2);
            found[0] = search.getRooms().isEmpty() ? null : search.getRooms().getFirst();

            return found[0] != null;
        }, JOIN_MILLIS, STEP_MILLIS, "the joiner finds the room at its own node");

        ParticipantChannel channel = joiner.getClient().joinRoom(found[0]);
        ParticipantChannel[] accepted = new ParticipantChannel[1];

        scenario.runUntil(() -> {
            if (accepted[0] == null) {
                accepted[0] = room.acceptChannel();
            }

            return channel.getState() == ChannelState.OPEN && accepted[0] != null;
        }, CONNECT_MILLIS, STEP_MILLIS, "the join reaches the host through its home node");

        Assertions.assertEquals(host.getClient().getParticipant(), channel.getPeer());
        Assertions.assertEquals(joiner.getClient().getParticipant(), accepted[0].getPeer());
    }

    @Test
    void a_friend_running_another_mod_is_followed_in_its_player_topic() {
        CommunityScenario scenario = new CommunityScenario(RANDOM_SEED + 5L, LOSS_RATE, PEER_COUNT);
        List<SimulatedPeer> peers = scenario.getPeers();
        SimulatedClient player = scenario.addClient("Player", NatType.PORT_RESTRICTED, NODES_PER_CLIENT);
        ModIdentity otherMod = ClientScenario.createIdentity(EngineVersion.CURRENT, "other-test", "2.0.0");
        SimulatedClient friend = scenario.addClient("Friend", NatType.SYMMETRIC, otherMod,
                                                    List.of(peers.get(2).getAddress(), peers.get(PEER_COUNT - 2).getAddress()));
        CommunitySession community = player.getClient().getCommunity();
        CommunitySession friendCommunity = friend.getClient().getCommunity();
        LinkPlayer friendPlayer = friend.getClient().getParticipant().getPlayer();

        scenario.runUntil(() -> player.getClient().getObservedAddress() != null && friend.getClient().getObservedAddress() != null, JOIN_MILLIS,
                          STEP_MILLIS, "both clients register");
        scenario.run(QUIET_MILLIS);

        Assertions.assertNull(community.getPresence(friendPlayer), "a player of another mod in no shared channel is not seen");

        community.addFriend(friendPlayer);
        scenario.runUntil(() -> community.getPresence(friendPlayer) != null, JOIN_MILLIS, STEP_MILLIS, "the friend's presence comes");

        PlayerPresence presence = community.getPresence(friendPlayer);

        Assertions.assertNotNull(presence);
        Assertions.assertEquals(List.of(friendPlayer), community.getFriends());
        Assertions.assertEquals("other-test", presence.getModId());
        Assertions.assertEquals("2.0.0", presence.getModVersion());
        Assertions.assertFalse(presence.canPlayTogether(), "another mod cannot play together");

        friendCommunity.setStatus(PlayerStatus.AWAY);
        scenario.runUntil(() -> {
            PlayerPresence current = community.getPresence(friendPlayer);

            return current != null && current.getStatus() == PlayerStatus.AWAY;
        }, JOIN_MILLIS, STEP_MILLIS, "the friend's new status comes");

        community.removeFriend(friendPlayer);

        Assertions.assertEquals(List.of(), community.getFriends());
        Assertions.assertNull(community.getPresence(friendPlayer), "a removed friend is no longer followed");
    }

    @Test
    void an_offline_appearing_player_leaves_the_member_lists_and_still_hosts_rooms() {
        CommunityScenario scenario = new CommunityScenario(RANDOM_SEED + 6L, LOSS_RATE, PEER_COUNT);
        List<SimulatedClient> members = addMembers(scenario, 2);
        SimulatedClient watcher = members.getFirst();
        SimulatedClient hider = members.getLast();
        Recorder recorder = new Recorder();
        ChatChannel channel = joinAll(scenario, members).getFirst();
        LinkPlayer hiderPlayer = hider.getClient().getParticipant().getPlayer();

        watcher.getClient().getCommunity().addListener(recorder);
        hider.getClient().getCommunity().setAppearOffline(true);
        scenario.runUntil(() -> channel.getMembers().size() == 1 && watcher.getClient().getCommunity().getPresence(hiderPlayer) == null, JOIN_MILLIS,
                          STEP_MILLIS, "the hiding player leaves the member list");

        watcher.getClient().getCommunity().whisper(hiderPlayer, "are you there");

        HostedRoom room = hider.getClient().publishRoom(SETTINGS);

        scenario.runUntil(() -> recorder.results.containsKey("are you there"), DELIVERY_MILLIS, STEP_MILLIS, "the whisper ends");

        Assertions.assertEquals(WhisperResult.NOT_ONLINE, recorder.results.get("are you there"));

        FoundRoom[] found = new FoundRoom[1];

        scenario.runUntil(() -> {
            RoomSearch search = watcher.getClient().searchRooms();

            scenario.run(STEP_MILLIS * 2);
            found[0] = search.getRooms().isEmpty() ? null : search.getRooms().getFirst();

            return found[0] != null;
        }, JOIN_MILLIS, STEP_MILLIS, "the hidden player's room is found");

        Assertions.assertEquals(hiderPlayer, found[0].getHost().getPlayer());
        Assertions.assertNotNull(room);
        Assertions.assertNull(watcher.getClient().getCommunity().getPresence(hiderPlayer), "the room shows no presence");

        hider.getClient().getCommunity().setAppearOffline(false);
        scenario.runUntil(() -> channel.getMembers().size() == 2, JOIN_MILLIS, STEP_MILLIS, "the player comes back");
    }
}
