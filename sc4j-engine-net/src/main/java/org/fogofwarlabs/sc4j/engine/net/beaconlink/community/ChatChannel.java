package org.fogofwarlabs.sc4j.engine.net.beaconlink.community;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Holds a chat channel the session joined: its members (the players whose presence names the channel's topic, this session's player
 * included) and its recent lines. Channels are shared by every mod; each member shows the mod it runs. Lines go out at most at the network's
 * chat rate per author (one per second after a burst of five); a line over it waits in a short queue.
 */
public interface ChatChannel {
    /**
     * The largest UTF-8 length of a line's text.
     */
    int MAXIMUM_TEXT_LENGTH = 255;

    /**
     * @return the channel name, as this session joined it
     */
    @NotNull String getName();

    /**
     * @return {@code true} if the session takes part in the channel, {@code false} otherwise (after {@link #leave()})
     */
    boolean isJoined();

    /**
     * @return the players in the channel whose presence the session holds, this session's player first, then by name
     */
    @NotNull List<@NotNull PlayerPresence> getMembers();

    /**
     * @return the recent lines of the channel, oldest first: the backlog the session got when it joined and the lines since, bounded
     */
    @NotNull List<@NotNull ChatLine> getLines();


    /**
     * Says a line in the channel.
     *
     * @param text the text, at most {@value #MAXIMUM_TEXT_LENGTH} UTF-8 bytes
     * @return {@code true} if the line is sent or queued, {@code false} otherwise (the queue of lines over the chat rate is full)
     * @throws IllegalArgumentException if the text is empty or too long
     * @throws IllegalStateException    if the session left the channel
     */
    boolean say(@NotNull String text);

    /**
     * Sends an emote in the channel ({@code /me}).
     *
     * @param text the text, at most {@value #MAXIMUM_TEXT_LENGTH} UTF-8 bytes
     * @return {@code true} if the emote is sent or queued, {@code false} otherwise (the queue of lines over the chat rate is full)
     * @throws IllegalArgumentException if the text is empty or too long
     * @throws IllegalStateException    if the session left the channel
     */
    boolean emote(@NotNull String text);

    /**
     * Leaves the channel: the session's presence there lapses and its home peers stop pushing the channel's lines. Calling it again does
     * nothing.
     */
    void leave();
}
