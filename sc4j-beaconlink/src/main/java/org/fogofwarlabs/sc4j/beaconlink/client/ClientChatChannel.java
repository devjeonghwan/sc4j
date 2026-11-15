package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.ChatChannel;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.ChatLine;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.community.PlayerPresence;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Holds a chat channel a client joined: a view of its {@link CommunityTopic} through the client's {@link ClientCommunity}, which sends its
 * lines and holds its members.
 */
final class ClientChatChannel implements ChatChannel {
    private final @NotNull ClientCommunity community;
    private final @NotNull CommunityTopic topic;

    private boolean joined;

    /**
     * Creates a {@link ClientChatChannel} instance of a joined channel.
     *
     * @param community the client's community part
     * @param topic     the channel's topic
     */
    ClientChatChannel(@NotNull ClientCommunity community, @NotNull CommunityTopic topic) {
        this.community = community;
        this.topic     = topic;

        this.joined = true;
    }

    /**
     * @return the channel's topic
     */
    @NotNull CommunityTopic getTopic() {
        return this.topic;
    }

    /**
     * @return the channel name, as this session joined it
     */
    @Override
    public @NotNull String getName() {
        String name = this.topic.getChannelName();

        return name == null ? "" : name;
    }

    /**
     * @return {@code true} if the session takes part in the channel, {@code false} otherwise (after {@link #leave()})
     */
    @Override
    public boolean isJoined() {
        return this.joined;
    }

    /**
     * @return the players in the channel whose presence the session holds, this session's player first, then by name
     */
    @Override
    public @NotNull List<@NotNull PlayerPresence> getMembers() {
        return this.community.listMembers(this.topic);
    }

    /**
     * @return the recent lines of the channel, oldest first: the backlog the session got when it joined and the lines since, bounded
     */
    @Override
    public @NotNull List<@NotNull ChatLine> getLines() {
        return this.community.listLines(this.topic);
    }


    /**
     * Says a line in the channel.
     *
     * @param text the text, at most {@value #MAXIMUM_TEXT_LENGTH} UTF-8 bytes
     * @return {@code true} if the line is sent or queued, {@code false} otherwise (the queue of lines over the chat rate is full)
     * @throws IllegalArgumentException if the text is empty or too long
     * @throws IllegalStateException    if the session left the channel
     */
    @Override
    public boolean say(@NotNull String text) {
        return this.community.sendLine(this, text, false);
    }

    /**
     * Sends an emote in the channel ({@code /me}).
     *
     * @param text the text, at most {@value #MAXIMUM_TEXT_LENGTH} UTF-8 bytes
     * @return {@code true} if the emote is sent or queued, {@code false} otherwise (the queue of lines over the chat rate is full)
     * @throws IllegalArgumentException if the text is empty or too long
     * @throws IllegalStateException    if the session left the channel
     */
    @Override
    public boolean emote(@NotNull String text) {
        return this.community.sendLine(this, text, true);
    }

    /**
     * Leaves the channel: the session's presence there lapses and its home peers stop pushing the channel's lines. Calling it again does
     * nothing.
     */
    @Override
    public void leave() {
        if (this.joined) {
            this.joined = false;
            this.community.leaveChannel(this);
        }
    }

    /**
     * @return the channel name, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.getName();
    }
}
