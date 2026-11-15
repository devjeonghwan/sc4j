package org.fogofwarlabs.sc4j.launcher.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Holds one whisper as the session last saw it: the other player (its friend code and its name and tag), the text, when it was sent and its
 * state. A view never changes; the session publishes a new one.
 */
public final class WhisperView {
    private final @NotNull FriendCode otherCode;
    private final @NotNull String otherTaggedName;
    private final @NotNull String text;
    private final long timestampMillis;
    private final @NotNull WhisperState state;

    /**
     * Creates a {@link WhisperView} instance.
     *
     * @param otherCode       the friend code of the other player: the sender of a received whisper, the target of a sent one
     * @param otherTaggedName the other player's name and tag ({@code Name#abcd12})
     * @param text            the text
     * @param timestampMillis when the whisper was sent, in epoch milliseconds
     * @param state           the state
     */
    public WhisperView(@NotNull FriendCode otherCode, @NotNull String otherTaggedName, @NotNull String text, long timestampMillis,
                       @NotNull WhisperState state) {
        this.otherCode       = otherCode;
        this.otherTaggedName = otherTaggedName;
        this.text            = text;
        this.timestampMillis = timestampMillis;
        this.state           = state;
    }

    /**
     * @return the friend code of the other player: the sender of a received whisper, the target of a sent one
     */
    public @NotNull FriendCode getOtherCode() {
        return this.otherCode;
    }

    /**
     * @return the other player's name and tag ({@code Name#abcd12})
     */
    public @NotNull String getOtherTaggedName() {
        return this.otherTaggedName;
    }

    /**
     * @return the text
     */
    public @NotNull String getText() {
        return this.text;
    }

    /**
     * @return the state
     */
    public @NotNull WhisperState getState() {
        return this.state;
    }

    /**
     * @return {@code true} if another player whispered to the account, {@code false} if the account sent the whisper
     */
    public boolean isIncoming() {
        return this.state == WhisperState.RECEIVED;
    }


    /**
     * Indicates whether some other object is "equal to" this one: a view of the same whisper in the same state.
     *
     * @param other the reference object with which to compare
     * @return {@code true} if this object is the same as the other argument, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof WhisperView view && this.otherCode.equals(view.otherCode) && this.otherTaggedName.equals(view.otherTaggedName) &&
               this.text.equals(view.text) && this.timestampMillis == view.timestampMillis && this.state == view.state;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.otherCode, this.otherTaggedName, this.text, this.timestampMillis, this.state);
    }
}
