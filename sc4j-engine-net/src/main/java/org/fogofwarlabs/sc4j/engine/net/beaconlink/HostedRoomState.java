package org.fogofwarlabs.sc4j.engine.net.beaconlink;

/**
 * Lists the states of a room a session publishes.
 */
public enum HostedRoomState {
    /**
     * No node holds the room yet; the session is sending it to the registered nodes.
     */
    PUBLISHING,
    /**
     * At least one registered node holds the room; searches through that node and through the other members of the room's mod topic find
     * it.
     */
    PUBLISHED,
    /**
     * No node holds the room and none can: the session cannot reach Beacon.Link or every registered node refused the room
     * ({@link HostedRoom#getFailure()}).
     */
    FAILED,
    /**
     * The room was withdrawn; the nodes keep the withdrawal until it expires.
     */
    WITHDRAWN
}
