package org.fogofwarlabs.sc4j.engine.net.lobby;

/**
 * Names the state of a {@link NetworkLobby} at this machine.
 */
public enum LobbyState {
    /**
     * The joiner connects to the host or waits for its answer.
     */
    JOINING,
    /**
     * The machine is in the room: the members and their slots may change.
     */
    OPEN,
    /**
     * The host started the game ({@link NetworkLobby#getStart()}).
     */
    STARTED,
    /**
     * The host refused the joiner ({@link NetworkLobby#getRefusal()}).
     */
    REFUSED,
    /**
     * The connection to the host ended before the game started.
     */
    CLOSED
}
