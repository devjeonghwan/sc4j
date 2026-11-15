package org.fogofwarlabs.sc4j.engine.net.discovery;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityMismatch;
import org.fogofwarlabs.sc4j.engine.net.DatagramEndpoint;
import org.fogofwarlabs.sc4j.engine.net.ManualNetworkClock;
import org.fogofwarlabs.sc4j.engine.net.UdpDatagramEndpoint;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests the LAN room discovery on 127.0.0.1: a client's unicast query finds the hosts' rooms with their lobby addresses and identity
 * mismatches, an announcement updates a found room, and a room that is not announced again expires.
 */
final class LanRoomDiscoveryTest {
    private static final @NotNull InetSocketAddress LOOPBACK = new InetSocketAddress("127.0.0.1", 0);
    private static final int DIGEST_LENGTH = 32;
    private static final int LOBBY_PORT = 40001;
    private static final int OTHER_LOBBY_PORT = 40002;
    private static final int CAPACITY = 4;
    private static final long WAIT_SECONDS = 10L;

    /**
     * Tests a condition of the polled discoveries.
     */
    @FunctionalInterface
    private interface Condition {
        boolean isMet();
    }

    private static @NotNull ModIdentity createIdentity(@NotNull String version) throws ManifestException {
        String text = "{\"format\": 1, \"id\": \"duel\", \"name\": {\"en\": \"Duel\"}, \"version\": \"" + version +
                      "\", \"engine\": \"0.1\", \"entry\": \"org.example.DuelMod\"}";
        ModManifest manifest = ModManifestReader.parse(text.getBytes(StandardCharsets.UTF_8));

        return ModIdentity.create(EngineVersion.CURRENT, manifest, new byte[DIGEST_LENGTH], List.of());
    }

    private final @NotNull List<@NotNull DatagramEndpoint> endpoints;

    LanRoomDiscoveryTest() {
        this.endpoints = new ArrayList<>();
    }

    @AfterEach
    void closeEndpoints() {
        for (DatagramEndpoint endpoint : this.endpoints) {
            IgnoreException.execute(
                    endpoint::close
            );
        }
    }

    @Test
    void query_should_find_the_rooms_by_unicast_with_their_lobby_addresses_and_identity_mismatches() throws IOException, ManifestException {
        ManualNetworkClock clock = new ManualNetworkClock();
        ModIdentity identity = createIdentity("1.0.0");
        LanRoomDiscovery host = new LanRoomDiscovery(this.open(), clock, identity);
        LanRoomDiscovery otherHost = new LanRoomDiscovery(this.open(), clock, createIdentity("1.1.0"));
        LanRoomDiscovery client = new LanRoomDiscovery(this.open(), clock, identity);

        host.setRoom(new LanRoom("Duel at noon", "host", LOBBY_PORT, 1, CAPACITY, true, identity));
        otherHost.setRoom(new LanRoom("Newer duel", "other", OTHER_LOBBY_PORT, 1, CAPACITY, true, createIdentity("1.1.0")));
        client.query(this.endpoints.get(0).getLocalAddress());
        client.query(this.endpoints.get(1).getLocalAddress());
        this.pollUntil(() -> client.getRooms().size() == 2, host, otherHost, client);

        DiscoveredRoom found = this.find(client, "Duel at noon");
        DiscoveredRoom other = this.find(client, "Newer duel");

        Assertions.assertEquals(new InetSocketAddress("127.0.0.1", LOBBY_PORT), found.getLobbyAddress());
        Assertions.assertNull(found.getMismatch());
        Assertions.assertTrue(found.isJoinable());
        Assertions.assertEquals(identity, found.getRoom().getIdentity());
        Assertions.assertEquals(ModIdentityMismatch.MOD_VERSION, other.getMismatch());
        Assertions.assertFalse(other.isJoinable());

        host.setRoom(new LanRoom("Duel at noon", "host", LOBBY_PORT, 2, CAPACITY, false, identity));
        host.announce(this.endpoints.get(2).getLocalAddress());
        this.pollUntil(() -> !this.find(client, "Duel at noon").getRoom().isOpen(), host, otherHost, client);

        Assertions.assertEquals(2, client.getRooms().size());
        Assertions.assertEquals(2, this.find(client, "Duel at noon").getRoom().getMemberCount());

        clock.advance(LanRoomDiscovery.ROOM_EXPIRY_MILLIS + 1L);
        client.poll();

        Assertions.assertEquals(List.of(), client.getRooms());
    }

    private @NotNull DatagramEndpoint open() throws IOException {
        DatagramEndpoint endpoint = UdpDatagramEndpoint.open(LOOPBACK);

        this.endpoints.add(endpoint);

        return endpoint;
    }

    private @NotNull DiscoveredRoom find(@NotNull LanRoomDiscovery discovery, @NotNull String name) {
        for (DiscoveredRoom room : discovery.getRooms()) {
            if (room.getRoom().getName().equals(name)) {
                return room;
            }
        }

        throw Exceptions.newAssertionError("No room {0} in {1}", name, discovery.getRooms());
    }

    private void pollUntil(@NotNull Condition condition, @NotNull LanRoomDiscovery @NotNull ... discoveries) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);

        while (!condition.isMet() && System.nanoTime() < deadline) {
            for (LanRoomDiscovery discovery : discoveries) {
                discovery.poll();
            }

            Thread.onSpinWait();
        }

        Assertions.assertTrue(condition.isMet(), "The discoveries did not answer.");
    }
}
