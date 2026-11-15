package org.fogofwarlabs.sc4j.engine.net.beaconlink.community;

import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds the community part of a Beacon.Link session: this player's presence, the chat channels it joined, whispers, friends and the ignore
 * list. The session publishes a signed presence in its mod's topic, in every joined channel and in its own player topic, and subscribes to
 * those topics at its home peers, which push it every new presence and line. Every operation only starts work; the owning session's
 * {@link org.fogofwarlabs.sc4j.engine.net.beaconlink.BeaconLinkSession#poll()} carries it out and calls the listeners. It is used by the
 * session's thread.
 */
public interface CommunitySession {
    /**
     * The most channels a session takes part in at once.
     */
    int MAXIMUM_CHANNELS = 4;
    /**
     * The most friends of a session.
     */
    int MAXIMUM_FRIENDS = 16;
    /**
     * The largest UTF-8 length of an away text.
     */
    int MAXIMUM_AWAY_TEXT_LENGTH = 32;

    /**
     * @return this session's own presence as the others see it
     */
    @NotNull PlayerPresence getSelf();

    /**
     * @return the channels the session takes part in, in the order it joined them
     */
    @NotNull List<@NotNull ChatChannel> getChannels();

    /**
     * Lists the channels the session knows of: the channels named in the presences of the players of its mod and of its channels, and its
     * own channels.
     *
     * @return the channels, the most members first, then by name
     */
    @NotNull List<@NotNull ChannelSummary> listChannels();

    /**
     * Finds the presence of a player the session sees in any of its topics, merged over the player's connections (the launcher, a game,
     * another computer): the player is online while any connection's presence is live, and shows the state of the connection that ranks
     * highest (in a game, in a room, online in a mod, in the launcher, away), with that connection's mod and away text, and the number of its
     * live connections ({@link PlayerPresence#getDeviceCount()}). The session's own player is merged the same way with this session's
     * presence.
     *
     * @param player the player
     * @return the merged presence, or {@code null} if the session holds no live presence of the player
     */
    @Nullable PlayerPresence getPresence(@NotNull LinkPlayer player);

    /**
     * @return the friends: players whose presence the session follows in their player topics, in the order they were added
     */
    @NotNull List<@NotNull LinkPlayer> getFriends();

    /**
     * @param player the player
     * @return {@code true} if the account ignores the player, {@code false} otherwise
     */
    boolean isIgnored(@NotNull LinkPlayer player);

    /**
     * @return the players the account ignores, the oldest first
     */
    @NotNull List<@NotNull LinkPlayer> getIgnored();


    /**
     * Joins a channel, or returns the channel the session already takes part in under that name (names that differ in case or white space
     * only are one channel).
     *
     * @param name the channel name, at most 32 UTF-8 bytes
     * @return the channel
     * @throws IllegalArgumentException if the name is empty, too long or has a control character
     * @throws IllegalStateException    if the session takes part in {@value #MAXIMUM_CHANNELS} channels
     */
    @NotNull ChatChannel joinChannel(@NotNull String name);

    /**
     * Sets the status the session's presence shows.
     *
     * @param status the status
     */
    void setStatus(@NotNull PlayerStatus status);

    /**
     * Sets the text the session's presence shows while it is away ({@link PlayerStatus#AWAY}); the others see it in their user lists and when
     * they whisper to this player.
     *
     * @param text the away text, cut to {@value #MAXIMUM_AWAY_TEXT_LENGTH} UTF-8 bytes; empty for none
     */
    void setAwayText(@NotNull String text);

    /**
     * Turns the session's presence off or on: an offline-appearing session publishes no presence (whispers to it then fail), while its
     * channels, rooms and games still work.
     *
     * @param offline whether the session appears offline
     */
    void setAppearOffline(boolean offline);

    /**
     * Sends a whisper to a player whose presence the session holds; the outcome comes to the listeners
     * ({@link CommunityListener#onWhisperResult}).
     *
     * @param player the target
     * @param text   the text, at most {@value ChatChannel#MAXIMUM_TEXT_LENGTH} UTF-8 bytes
     * @return the sent whisper
     * @throws IllegalArgumentException if the player is this session's own, or the text is empty or too long
     */
    @NotNull Whisper whisper(@NotNull LinkPlayer player, @NotNull String text);

    /**
     * Ignores a player: its lines and whispers are no longer handed out, and mods hide its rooms. The ignore list belongs to the account: every
     * session of the account shares it and follows the others' changes within seconds (a guest's lives in memory for the run). Nothing of it
     * goes on the network.
     *
     * @param player the player
     */
    void ignore(@NotNull LinkPlayer player);

    /**
     * Stops ignoring a player, for every session of the account.
     *
     * @param player the player
     */
    void unignore(@NotNull LinkPlayer player);

    /**
     * Adds a friend: the session follows the player's presence in its player topic. The install keeps its friends.
     *
     * @param player the player
     * @throws IllegalArgumentException if the player is this session's own
     * @throws IllegalStateException    if the session has {@value #MAXIMUM_FRIENDS} friends
     */
    void addFriend(@NotNull LinkPlayer player);

    /**
     * Removes a friend.
     *
     * @param player the player
     */
    void removeFriend(@NotNull LinkPlayer player);

    /**
     * Adds a listener; it is called on the session's thread.
     *
     * @param listener the listener
     */
    void addListener(@NotNull CommunityListener listener);

    /**
     * Removes a listener.
     *
     * @param listener the listener
     */
    void removeListener(@NotNull CommunityListener listener);
}
