package org.fogofwarlabs.sc4j.engine.net.beaconlink.community;

import org.jetbrains.annotations.NotNull;

/**
 * Holds a chat channel the session knows of: its name and how many players the session sees in it. Channels are shared by every mod.
 */
public final class ChannelSummary {
    private final @NotNull String name;
    private final int memberCount;

    /**
     * Creates a {@link ChannelSummary} instance.
     *
     * @param name        the channel name
     * @param memberCount the number of players the session sees in the channel
     */
    public ChannelSummary(@NotNull String name, int memberCount) {
        this.name        = name;
        this.memberCount = memberCount;
    }

    /**
     * @return the channel name
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the number of players the session sees in the channel: an estimate from the presences it holds
     */
    public int getMemberCount() {
        return this.memberCount;
    }


    /**
     * @return the name and the member count, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.name + " (" + this.memberCount + ")";
    }
}
