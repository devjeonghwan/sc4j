package org.fogofwarlabs.sc4j.launcher.nodes;

/**
 * Lists the outcomes of pinging one node ({@link NodePinger}).
 */
public enum NodePingStatus {
    /**
     * The node answered with a validly signed pong.
     */
    REACHABLE,
    /**
     * No valid answer arrived after every attempt.
     */
    NOT_ANSWERING,
    /**
     * The host name cannot be resolved.
     */
    UNRESOLVED,
    /**
     * The ping socket cannot be opened or the ping cannot be sent to the node's address.
     */
    FAILED
}
