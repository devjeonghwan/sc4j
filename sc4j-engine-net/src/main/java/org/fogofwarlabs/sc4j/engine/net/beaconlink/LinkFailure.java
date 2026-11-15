package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.jetbrains.annotations.NotNull;

/**
 * Lists why Beacon.Link, a room publication or a room search failed, with the text a mod can show.
 */
public enum LinkFailure {
    /**
     * The run names no seed and no node was learned earlier.
     */
    NO_NODES_CONFIGURED("No Beacon.Link node is listed; add a node in the launcher settings."),
    /**
     * No node registered the session: every node either did not answer or could not be resolved.
     */
    NO_NODE_ANSWERED("No Beacon.Link node answered."),
    /**
     * Every node that answered refused the session: it was full or answered with another key than the pinned one.
     */
    ALL_NODES_REFUSED("Every Beacon.Link node refused the connection."),
    /**
     * Every node that answered refused the session, and one of them because it holds the most live connections of the account it allows.
     */
    TOO_MANY_CONNECTIONS("Too many devices use this account."),
    /**
     * Another connection of the account signed out everywhere else after this session started; the session ended and registers nowhere.
     */
    SIGNED_OUT("Signed out from another device."),
    /**
     * Every registered node refused the room's advertisement.
     */
    ROOM_REFUSED("Every Beacon.Link node refused the room.");


    private final @NotNull String description;

    /**
     * Creates a {@link LinkFailure} instance.
     *
     * @param description the text a mod can show
     */
    LinkFailure(@NotNull String description) {
        this.description = description;
    }

    /**
     * @return the text a mod can show
     */
    public @NotNull String getDescription() {
        return this.description;
    }
}
