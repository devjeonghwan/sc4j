package org.fogofwarlabs.sc4j.engine.client.community;

import org.fogofwarlabs.sc4j.engine.net.beaconlink.FoundRoom;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.LatencyClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Holds the filter of a Beacon.Link room list: the search text (found in the room name, the map name or a text the mod names), the worst
 * latency to show, the largest room capacity to show and whether the closed rooms are listed: the rooms this run cannot join
 * ({@link FoundRoom#isJoinable()}: full, started or of another version of the mod). A room whose latency is not estimated yet passes every
 * latency. A mod adds its own choices (for example a game type) and search texts through a {@link ModFilter}.
 * <p>
 * {@link #select} filters the found rooms and orders them: the joinable rooms by latency, best first, then by name, followed by the closed
 * rooms in the same order.
 */
public final class RoomFilter {
    /**
     * The largest capacity value that passes every room.
     */
    public static final int ANY_CAPACITY = 0;

    private static final @NotNull ModFilter NO_MOD_FILTER = new ModFilter() {
    };

    /**
     * Adds a mod's own choices and search texts to a room filter. Both methods have a default that adds nothing.
     */
    public interface ModFilter {
        /**
         * Checks whether the mod's own choices pass a room.
         *
         * @param room a found room
         * @return {@code true} if the mod's own choices pass the room, {@code false} otherwise
         */
        default boolean accepts(@NotNull FoundRoom room) {
            return true;
        }

        /**
         * Returns the room's texts the search text is also found in besides the room and map names, for example the host's name or the game
         * type.
         *
         * @param room a found room
         * @return the texts, empty for none
         */
        default @NotNull List<@NotNull String> getSearchTexts(@NotNull FoundRoom room) {
            return List.of();
        }
    }

    private static int getLatencyRank(@NotNull FoundRoom room) {
        LatencyClass latency = room.getLatencyEstimate().getLatencyClass();

        return latency == LatencyClass.UNKNOWN ? LatencyClass.values().length : latency.ordinal();
    }

    private static boolean contains(@NotNull String text, @NotNull String query) {
        return text.toLowerCase(Locale.ROOT).contains(query);
    }

    private @NotNull String query;
    private @Nullable LatencyClass worstLatency;
    private int maximumCapacity;
    private boolean closedRoomsShown;

    /**
     * Creates a {@link RoomFilter} instance that passes every room.
     */
    public RoomFilter() {
        this.query            = "";
        this.worstLatency     = null;
        this.maximumCapacity  = ANY_CAPACITY;
        this.closedRoomsShown = true;
    }

    /**
     * @return the search text
     */
    public @NotNull String getQuery() {
        return this.query;
    }

    /**
     * Sets the search text.
     *
     * @param query the text, found in a room's name, map name or the mod's search texts regardless of case; empty passes every room
     */
    public void setQuery(@NotNull String query) {
        this.query = query;
    }

    /**
     * @return the worst latency to show, or {@code null} for every latency
     */
    public @Nullable LatencyClass getWorstLatency() {
        return this.worstLatency;
    }

    /**
     * Sets the worst latency to show.
     *
     * @param worstLatency the worst latency class to show, or {@code null} for every latency
     */
    public void setWorstLatency(@Nullable LatencyClass worstLatency) {
        this.worstLatency = worstLatency;
    }

    /**
     * @return the largest room capacity to show, or {@link #ANY_CAPACITY}
     */
    public int getMaximumCapacity() {
        return this.maximumCapacity;
    }

    /**
     * Sets the largest room capacity to show.
     *
     * @param maximumCapacity the capacity, or {@link #ANY_CAPACITY} for every room
     */
    public void setMaximumCapacity(int maximumCapacity) {
        this.maximumCapacity = maximumCapacity;
    }

    /**
     * @return {@code true} if the closed rooms (full, started or of another version of the mod) are listed, {@code false} otherwise
     */
    public boolean isClosedRoomsShown() {
        return this.closedRoomsShown;
    }

    /**
     * Sets whether the closed rooms (full, started or of another version of the mod) are listed.
     *
     * @param closedRoomsShown whether they are listed
     */
    public void setClosedRoomsShown(boolean closedRoomsShown) {
        this.closedRoomsShown = closedRoomsShown;
    }

    /**
     * Sets every choice to pass every room; the search text stays.
     */
    public void reset() {
        this.worstLatency     = null;
        this.maximumCapacity  = ANY_CAPACITY;
        this.closedRoomsShown = true;
    }

    /**
     * Copies another filter's choices and search text.
     *
     * @param other the filter to copy
     */
    public void copy(@NotNull RoomFilter other) {
        this.query            = other.query;
        this.worstLatency     = other.worstLatency;
        this.maximumCapacity  = other.maximumCapacity;
        this.closedRoomsShown = other.closedRoomsShown;
    }


    /**
     * Filters and orders found rooms without choices of the mod.
     *
     * @param rooms the found rooms, joinable and closed
     * @return the rooms that pass the filter: the joinable ones first, each part by latency (best first, unknown last) and then by name
     */
    public @NotNull List<@NotNull FoundRoom> select(@NotNull List<@NotNull FoundRoom> rooms) {
        return this.select(rooms, NO_MOD_FILTER);
    }

    /**
     * Filters and orders found rooms.
     *
     * @param rooms     the found rooms, joinable and closed
     * @param modFilter the mod's own choices and search texts
     * @return the rooms that pass the filter: the joinable ones first, each part by latency (best first, unknown last) and then by name
     */
    public @NotNull List<@NotNull FoundRoom> select(@NotNull List<@NotNull FoundRoom> rooms, @NotNull ModFilter modFilter) {
        List<FoundRoom> selected = new ArrayList<>(rooms.size());

        for (FoundRoom room : rooms) {
            if (this.accepts(room, modFilter)) {
                selected.add(room);
            }
        }

        selected.sort(Comparator.comparing((FoundRoom room) -> !room.isJoinable())
                                .thenComparingInt(RoomFilter::getLatencyRank)
                                .thenComparing(room -> room.getSettings().getRoomName().toLowerCase(Locale.ROOT)));

        return selected;
    }

    private boolean accepts(@NotNull FoundRoom room, @NotNull ModFilter modFilter) {
        LatencyClass latency = room.getLatencyEstimate().getLatencyClass();
        LatencyClass worst = this.worstLatency;

        if (!room.isJoinable() && !this.closedRoomsShown) {
            return false;
        }

        if (!modFilter.accepts(room)) {
            return false;
        }

        if (worst != null && latency != LatencyClass.UNKNOWN && latency.ordinal() > worst.ordinal()) {
            return false;
        }

        if (this.maximumCapacity != ANY_CAPACITY && room.getSettings().getCapacity() > this.maximumCapacity) {
            return false;
        }

        String search = this.query.trim().toLowerCase(Locale.ROOT);

        if (search.isEmpty() || contains(room.getSettings().getRoomName(), search) || contains(room.getSettings().getMapName(), search)) {
            return true;
        }

        for (String text : modFilter.getSearchTexts(room)) {
            if (contains(text, search)) {
                return true;
            }
        }

        return false;
    }
}
