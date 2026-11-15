package org.fogofwarlabs.sc4j.engine.trigger;

/**
 * Names where a player stands in a game the standard trigger system decides: still playing, won or lost.
 */
public enum PlayerOutcome {
    /**
     * The player still plays; the game has not decided about the player.
     */
    PLAYING,
    /**
     * The player won.
     */
    VICTORY,
    /**
     * The player lost.
     */
    DEFEAT
}
