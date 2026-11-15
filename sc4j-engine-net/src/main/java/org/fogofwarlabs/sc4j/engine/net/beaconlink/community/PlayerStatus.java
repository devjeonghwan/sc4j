package org.fogofwarlabs.sc4j.engine.net.beaconlink.community;

/**
 * Lists what a player's presence says the player is doing.
 */
public enum PlayerStatus {
    /**
     * The player is online and can chat.
     */
    AVAILABLE,
    /**
     * The player is online but away.
     */
    AWAY,
    /**
     * The player is in a room, waiting for its game.
     */
    IN_ROOM,
    /**
     * The player is in a game.
     */
    IN_GAME
}
