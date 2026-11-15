package org.fogofwarlabs.sc4j.launcher.session;

/**
 * Lists the states a friend is shown in: the states of the friend's devices merged into one, the highest of them.
 */
public enum FriendState {
    /**
     * No device of the friend is online.
     */
    OFFLINE,
    /**
     * The friend is online but away.
     */
    AWAY,
    /**
     * The friend is logged in to a launcher only.
     */
    IN_LAUNCHER,
    /**
     * The friend is online in a mod.
     */
    ONLINE,
    /**
     * The friend is in a room, waiting for its game.
     */
    IN_ROOM,
    /**
     * The friend is in a game.
     */
    IN_GAME
}
