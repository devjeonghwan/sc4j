package org.fogofwarlabs.sc4j.launcher.session;

/**
 * Lists the states of a whisper the session shows: received, or sent and waiting for its outcome, delivered or not delivered.
 */
public enum WhisperState {
    /**
     * Another player whispered to the account.
     */
    RECEIVED,
    /**
     * The account whispered to another player; the outcome is not known yet.
     */
    SENDING,
    /**
     * The account's whisper reached a device of the other player.
     */
    DELIVERED,
    /**
     * The account's whisper reached no device of the other player: the player is offline, or no node relayed it in time.
     */
    NOT_DELIVERED
}
