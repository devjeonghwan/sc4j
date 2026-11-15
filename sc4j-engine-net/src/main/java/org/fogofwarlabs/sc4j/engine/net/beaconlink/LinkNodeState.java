package org.fogofwarlabs.sc4j.engine.net.beaconlink;

/**
 * Lists how a Beacon.Link session stands with one node.
 */
public enum LinkNodeState {
    /**
     * The session is registering at the node for the first time, or again after the node forgot the registration.
     */
    CONNECTING,
    /**
     * The node holds a registration of the session at its observed address.
     */
    REGISTERED,
    /**
     * The node did not answer a request in time; the session tries again later.
     */
    NOT_ANSWERING,
    /**
     * The node refused the registration (it holds the most registrations it allows); the session tries again later.
     */
    REFUSED,
    /**
     * The node answered with another key than the one the node list pins for it; the session tries again later.
     */
    WRONG_KEY,
    /**
     * The node refused the registration because it holds the most live connections of the account it allows; the session tries again later,
     * when a connection's registration may have ended.
     */
    TOO_MANY_CONNECTIONS,
    /**
     * The node refused the registration because another connection of the account signed out everywhere else after the session started; the
     * session does not try again.
     */
    SIGNED_OUT,
    /**
     * The node's host name could not be resolved when the session opened.
     */
    UNRESOLVED
}
