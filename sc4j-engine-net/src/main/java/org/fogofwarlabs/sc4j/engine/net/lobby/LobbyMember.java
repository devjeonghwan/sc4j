package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one member of a network lobby room as the host announced it: the participant it will be in the game (the host is peer {@code 0}),
 * its name, the player slot it takes, or {@link NetworkLobby#OBSERVER_SLOT} for an observer, the faction it chose for its slot, whether it is
 * ready to start and whether it has the room's map.
 */
public final class LobbyMember {
    private final @NotNull LockstepPeer peer;
    private final @NotNull String name;
    private final int slotIndex;

    private final @Nullable Faction faction;
    private final boolean ready;
    private final @NotNull LobbyMapState mapState;

    /**
     * Creates a {@link LobbyMember} instance of a room that has no faction choice, ready flag or map check: no faction, not ready, map state
     * {@link LobbyMapState#UNKNOWN}.
     *
     * @param peer      the participant the member will be in the game
     * @param name      the member's name
     * @param slotIndex the player slot index the member takes, or {@link NetworkLobby#OBSERVER_SLOT} for an observer
     */
    public LobbyMember(@NotNull LockstepPeer peer, @NotNull String name, int slotIndex) {
        this(peer, name, slotIndex, null, false, LobbyMapState.UNKNOWN);
    }

    /**
     * Creates a {@link LobbyMember} instance.
     *
     * @param peer      the participant the member will be in the game
     * @param name      the member's name
     * @param slotIndex the player slot index the member takes, or {@link NetworkLobby#OBSERVER_SLOT} for an observer
     * @param faction   the faction the member chose for its slot, or {@code null} if it chose none
     * @param ready     {@code true} if the member is ready to start, {@code false} otherwise
     * @param mapState  whether the member has the room's map
     */
    public LobbyMember(@NotNull LockstepPeer peer, @NotNull String name, int slotIndex,
                       @Nullable Faction faction, boolean ready, @NotNull LobbyMapState mapState) {
        this.peer      = peer;
        this.name      = name;
        this.slotIndex = slotIndex;

        this.faction  = faction;
        this.ready    = ready;
        this.mapState = mapState;
    }

    /**
     * @return the participant the member will be in the game
     */
    public @NotNull LockstepPeer getPeer() {
        return this.peer;
    }

    /**
     * @return the member's name
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the player slot index the member takes, or {@link NetworkLobby#OBSERVER_SLOT} for an observer
     */
    public int getSlotIndex() {
        return this.slotIndex;
    }

    /**
     * @return the faction the member chose for its slot, or {@code null} if it chose none
     */
    public @Nullable Faction getFaction() {
        return this.faction;
    }

    /**
     * @return {@code true} if the member is ready to start, {@code false} otherwise
     */
    public boolean isReady() {
        return this.ready;
    }

    /**
     * @return whether the member has the room's map
     */
    public @NotNull LobbyMapState getMapState() {
        return this.mapState;
    }

    /**
     * @return {@code true} if the member takes no player slot, {@code false} otherwise
     */
    public boolean isObserver() {
        return this.slotIndex == NetworkLobby.OBSERVER_SLOT;
    }


    /**
     * @return the participant, the name and the slot, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.name + " (" + this.peer + (this.isObserver() ? ", observer)" : ", slot " + this.slotIndex + ")");
    }
}
