package org.fogofwarlabs.sc4j.engine.client.community;

import org.fogofwarlabs.sc4j.engine.net.beaconlink.*;
import org.fogofwarlabs.sc4j.engine.net.lobby.JoinRefusal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests the room list's filter: the order of the listed rooms, the search text in names and in the mod's texts, the latency, the largest
 * capacity, the closed rooms (full, started or of another version), the mod's own choices, and that reset passes every room again.
 */
final class RoomFilterTest {
    private static final long GOOD_MILLISECONDS = 40;
    private static final long FAIR_MILLISECONDS = 150;
    private static final long POOR_MILLISECONDS = 320;
    private static final int TWO_PLAYERS = 2;
    private static final int FOUR_PLAYERS = 4;
    private static final int EIGHT_PLAYERS = 8;

    private static final @NotNull List<@NotNull FoundRoom> ROOMS = List.of(
            createRoom(1, "beta", EIGHT_PLAYERS, null, LatencyEstimate.of(POOR_MILLISECONDS)),
            createRoom(2, "Duel", TWO_PLAYERS, null, LatencyEstimate.UNKNOWN),
            createRoom(3, "Old version", FOUR_PLAYERS, JoinRefusal.MOD_VERSION, LatencyEstimate.of(GOOD_MILLISECONDS)),
            createRoom(4, "Alpha", FOUR_PLAYERS, null, LatencyEstimate.of(FAIR_MILLISECONDS)),
            createRoom(5, "alpha two", FOUR_PLAYERS, null, LatencyEstimate.of(POOR_MILLISECONDS))
    );

    /**
     * Finds the search text also in the host's name of the duel and passes only the rooms of at most four players.
     */
    private static final RoomFilter.@NotNull ModFilter SMALL_ROOMS = new RoomFilter.ModFilter() {
        @Override
        public boolean accepts(@NotNull FoundRoom room) {
            return room.getSettings().getCapacity() <= FOUR_PLAYERS;
        }

        @Override
        public @NotNull List<@NotNull String> getSearchTexts(@NotNull FoundRoom room) {
            return room.getSettings().getRoomName().equals("Duel") ? List.of("Rowan#1a2b3c") : List.of();
        }
    };

    private static @NotNull FoundRoom createRoom(int key, @NotNull String name, int capacity, @Nullable JoinRefusal refusal,
                                                 @NotNull LatencyEstimate latency) {
        byte[] bytes = new byte[LinkPlayer.KEY_LENGTH];

        Arrays.fill(bytes, (byte) key);

        return new FoundRoom(LinkParticipant.of(LinkPlayer.of(bytes), 1L), 1, new RoomSettings(name, "Ashen Ford", 1, capacity), refusal, latency);
    }

    private static @NotNull List<String> select(@NotNull RoomFilter filter) {
        return filter.select(ROOMS).stream().map(room -> room.getSettings().getRoomName()).toList();
    }

    @Test
    void select_should_list_full_and_started_rooms_as_closed_rooms_after_the_joinable_ones() {
        LinkParticipant host = LinkParticipant.of(LinkPlayer.of(new byte[LinkPlayer.KEY_LENGTH]), 1L);
        FoundRoom open = new FoundRoom(host, 1, new RoomSettings("Open", "Ashen Ford", 1, TWO_PLAYERS), null,
                                       LatencyEstimate.of(POOR_MILLISECONDS));
        FoundRoom full = new FoundRoom(host, 2, new RoomSettings("Full", "Ashen Ford", TWO_PLAYERS, TWO_PLAYERS), null,
                                       LatencyEstimate.of(GOOD_MILLISECONDS));
        FoundRoom started = new FoundRoom(host, 3, new RoomSettings("Started", "Ashen Ford", 1, TWO_PLAYERS, true, new byte[0]),
                                          null, LatencyEstimate.of(GOOD_MILLISECONDS));
        RoomFilter filter = new RoomFilter();

        Assertions.assertTrue(open.isJoinable());
        Assertions.assertFalse(full.isJoinable());
        Assertions.assertFalse(started.isJoinable());
        Assertions.assertEquals(List.of("Open", "Full", "Started"),
                                filter.select(List.of(started, full, open)).stream().map(room -> room.getSettings().getRoomName()).toList());

        filter.setClosedRoomsShown(false);

        Assertions.assertEquals(List.of("Open"),
                                filter.select(List.of(started, full, open)).stream().map(room -> room.getSettings().getRoomName()).toList());
    }

    @Test
    void select_should_list_joinable_rooms_by_latency_and_name_before_other_versions() {
        Assertions.assertEquals(List.of("Alpha", "alpha two", "beta", "Duel", "Old version"), select(new RoomFilter()));
    }

    @Test
    void select_should_find_the_search_text_in_the_names_and_the_mod_texts() {
        RoomFilter filter = new RoomFilter();

        filter.setQuery("ALPHA");

        Assertions.assertEquals(List.of("Alpha", "alpha two"), select(filter));

        filter.setQuery("ashen");

        Assertions.assertEquals(ROOMS.size(), select(filter).size(), "every room's map");

        filter.setQuery("rowan");

        Assertions.assertEquals(List.of(), select(filter));
        Assertions.assertEquals(List.of("Duel"), filter.select(ROOMS, SMALL_ROOMS).stream().map(room -> room.getSettings().getRoomName()).toList());
    }

    @Test
    void select_should_filter_by_latency_capacity_version_and_mod_choices() {
        RoomFilter filter = new RoomFilter();

        filter.setWorstLatency(LatencyClass.FAIR);

        Assertions.assertEquals(List.of("Alpha", "Duel", "Old version"), select(filter), "an unknown latency passes");

        filter.setWorstLatency(null);
        filter.setMaximumCapacity(FOUR_PLAYERS);

        Assertions.assertEquals(List.of("Alpha", "alpha two", "Duel", "Old version"), select(filter));

        filter.setClosedRoomsShown(false);

        Assertions.assertEquals(List.of("Alpha", "alpha two", "Duel"), select(filter));

        filter.setQuery("duel");
        filter.reset();

        Assertions.assertEquals(List.of("Duel"), select(filter), "reset keeps the search text");

        filter.setQuery("");

        Assertions.assertEquals(List.of("Alpha", "alpha two", "Duel", "Old version"),
                                filter.select(ROOMS, SMALL_ROOMS).stream().map(room -> room.getSettings().getRoomName()).toList());
    }
}
