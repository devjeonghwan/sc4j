package org.fogofwarlabs.sc4j.engine.net.beaconlink.community;

import org.jetbrains.annotations.NotNull;

/**
 * Receives what happens in a community session: new chat lines, changed members of a channel, whispers and the outcome of sent whispers. The
 * session calls its listeners on the owner's thread, inside {@link org.fogofwarlabs.sc4j.engine.net.beaconlink.BeaconLinkSession#poll()}. Every
 * method does nothing by default.
 */
public interface CommunityListener {
    /**
     * Called when a joined channel got a new line, the session's own lines included; lines of ignored players are not handed out.
     *
     * @param channel the channel
     * @param line    the line
     */
    default void onChatLine(@NotNull ChatChannel channel, @NotNull ChatLine line) {
    }

    /**
     * Called when a player came into a joined channel, left it, or its presence changed (status, mod, name).
     *
     * @param channel the channel
     */
    default void onMembersChanged(@NotNull ChatChannel channel) {
    }

    /**
     * Called when a whisper arrived; whispers of ignored players are not handed out.
     *
     * @param whisper the whisper
     */
    default void onWhisper(@NotNull Whisper whisper) {
    }

    /**
     * Called when the outcome of a sent whisper is known.
     *
     * @param whisper the sent whisper
     * @param result  the outcome
     */
    default void onWhisperResult(@NotNull Whisper whisper, @NotNull WhisperResult result) {
    }
}
