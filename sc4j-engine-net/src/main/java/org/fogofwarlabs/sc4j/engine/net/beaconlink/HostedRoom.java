package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds a room a Beacon.Link session publishes. The session keeps it at every registered node and refreshes it before it expires, until
 * {@link #withdraw()} or the session's close.
 */
public interface HostedRoom {
    /**
     * @return the room's current settings
     */
    @NotNull RoomSettings getSettings();

    /**
     * @return the publication state
     */
    @NotNull HostedRoomState getState();

    /**
     * @return why the room is not published, or {@code null} unless the state is {@link HostedRoomState#FAILED}
     */
    @Nullable LinkFailure getFailure();


    /**
     * Replaces the room's settings, for example its player count; the nodes receive the new settings with the next poll.
     *
     * @param settings the new settings
     * @throws IllegalArgumentException if a name is longer than the room advertisement allows
     * @throws IllegalStateException    if the room was withdrawn
     */
    void update(@NotNull RoomSettings settings);

    /**
     * Withdraws the room: the nodes that hold it receive a signed withdrawal with the next poll. Calling it again does nothing.
     */
    void withdraw();

    /**
     * Takes the channel of the next participant that joined the room: it passed the join check (the engine version and the mod identity are
     * the room's) and its channel opened. Returns at once when none waits.
     *
     * @return the channel, or {@code null} if no joined participant waits
     */
    @Nullable ParticipantChannel acceptChannel();
}
