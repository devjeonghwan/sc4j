package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.jetbrains.annotations.Nullable;

/**
 * Names whether a lobby member has the room's map, as the host knows it. The code is the state's byte on the wire.
 */
public enum LobbyMapState {
    /**
     * The member has not answered for the room's current map yet.
     */
    UNKNOWN(0),
    /**
     * The member misses the map and the host cannot send it, or the received map did not match its reference.
     */
    MISSING(1),
    /**
     * The host sends the map to the member.
     */
    TRANSFERRING(2),
    /**
     * The member has the map of the room's name and digest.
     */
    PRESENT(3);

    /**
     * Finds the map state of a wire code.
     *
     * @param code the code
     * @return the map state, or {@code null} if no map state has the code
     */
    public static @Nullable LobbyMapState find(int code) {
        for (LobbyMapState state : values()) {
            if (state.code == code) {
                return state;
            }
        }

        return null;
    }


    private final int code;

    LobbyMapState(int code) {
        this.code = code;
    }

    /**
     * @return the code of the map state on the wire
     */
    public int getCode() {
        return this.code;
    }
}
