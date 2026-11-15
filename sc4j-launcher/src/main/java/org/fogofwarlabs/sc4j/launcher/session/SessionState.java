package org.fogofwarlabs.sc4j.launcher.session;

/**
 * Lists the states of the logged-in account's Beacon.Link session.
 */
public enum SessionState {
    /**
     * The session has no Beacon.Link connection: no other player sees the account and no friend is shown.
     */
    OFFLINE,
    /**
     * The session is joining the Beacon.Link network.
     */
    CONNECTING,
    /**
     * The session is registered at its nodes: friends see the account and the account sees its friends.
     */
    CONNECTED
}
