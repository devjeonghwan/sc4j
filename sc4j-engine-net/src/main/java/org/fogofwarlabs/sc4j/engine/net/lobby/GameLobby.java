package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reads the room of a network game at this machine before the game starts: whether this machine hosts it, the room's state and its members.
 * The engine's room is the {@link NetworkLobby}; a mod with its own lobby protocol implements this interface for the views that show a room,
 * and keeps the rest of its protocol (its commands, map transfer, chat and start) to itself.
 * <p>
 * A lobby is read on the thread that drives it.
 */
public interface GameLobby {
    /**
     * @return {@code true} if this machine hosts the room, {@code false} otherwise (it joined)
     */
    boolean isHost();

    /**
     * @return the room's state at this machine
     */
    @NotNull LobbyState getLobbyState();

    /**
     * @return the room's members in the room's order, each with its slot index or {@link NetworkLobby#OBSERVER_SLOT}
     */
    @NotNull List<@NotNull LobbyMember> getMembers();

    /**
     * @return this machine's member, or {@code null} while this machine holds no place in the room
     */
    @Nullable LobbyMember getLocalMember();
}
