package org.fogofwarlabs.sc4j.engine.net.discovery;

import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityMismatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

/**
 * Holds a room the LAN room discovery found: the room as announced, the address of its network lobby (the announcing host's address with the
 * room's lobby port), how its mod identity differs from the local one, and when it was last announced.
 */
public final class DiscoveredRoom {
    private final @NotNull LanRoom room;
    private final @NotNull InetSocketAddress lobbyAddress;
    private final @Nullable ModIdentityMismatch mismatch;
    private final long seenTime;

    /**
     * Creates a {@link DiscoveredRoom} instance.
     *
     * @param room         the room as announced
     * @param lobbyAddress the address of the room's network lobby
     * @param mismatch     the first part in which the room's mod identity differs from the local one, or {@code null} if they are equal
     * @param seenTime     the time of the discovery's clock when the room was last announced
     */
    public DiscoveredRoom(@NotNull LanRoom room, @NotNull InetSocketAddress lobbyAddress, @Nullable ModIdentityMismatch mismatch, long seenTime) {
        this.room         = room;
        this.lobbyAddress = lobbyAddress;
        this.mismatch     = mismatch;
        this.seenTime     = seenTime;
    }

    /**
     * @return the room as announced
     */
    public @NotNull LanRoom getRoom() {
        return this.room;
    }

    /**
     * @return the address of the room's network lobby
     */
    public @NotNull InetSocketAddress getLobbyAddress() {
        return this.lobbyAddress;
    }

    /**
     * @return the first part in which the room's mod identity differs from the local one, or {@code null} if they are equal
     */
    public @Nullable ModIdentityMismatch getMismatch() {
        return this.mismatch;
    }

    /**
     * @return the time of the discovery's clock when the room was last announced
     */
    public long getSeenTime() {
        return this.seenTime;
    }

    /**
     * @return {@code true} if the room runs the local mod identity and takes joiners, {@code false} otherwise
     */
    public boolean isJoinable() {
        return this.mismatch == null && this.room.isOpen() && this.room.getMemberCount() < this.room.getCapacity();
    }


    /**
     * @return the room, its lobby address and the mismatch, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.room + " at " + this.lobbyAddress + (this.mismatch == null ? "" : ", other " + this.mismatch.getDescription());
    }
}
