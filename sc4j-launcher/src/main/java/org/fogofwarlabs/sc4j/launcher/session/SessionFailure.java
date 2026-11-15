package org.fogofwarlabs.sc4j.launcher.session;

/**
 * Lists why the logged-in account's session has no Beacon.Link connection.
 */
public enum SessionFailure {
    /**
     * The session could not start: the account's key could not be read, the account is being deleted, or no socket could be opened.
     */
    NOT_STARTED,
    /**
     * Neither the node list nor the learned peers name a node.
     */
    NO_NODES,
    /**
     * No node answered.
     */
    NO_ANSWER,
    /**
     * Every node refused the connection.
     */
    REFUSED,
    /**
     * A node refused the connection because the account uses the most devices a node allows.
     */
    TOO_MANY_DEVICES,
    /**
     * Another device of the account signed out everywhere else after this launcher logged in.
     */
    SIGNED_OUT
}
