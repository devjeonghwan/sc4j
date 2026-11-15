package org.fogofwarlabs.sc4j.engine.client.community;

import org.jetbrains.annotations.NotNull;

/**
 * Holds one channel chat log entry: its kind, the sender's prefix (empty for an entry without a sender) and the text.
 */
public final class ChatEntry {
    private final @NotNull ChatEntryKind kind;
    private final @NotNull String prefix;
    private final @NotNull String text;

    /**
     * Creates a {@link ChatEntry} instance.
     *
     * @param kind   the entry's kind
     * @param prefix the sender's prefix, for example {@code "Name#abcd12: "}, empty for an entry without a sender
     * @param text   the text
     */
    public ChatEntry(@NotNull ChatEntryKind kind, @NotNull String prefix, @NotNull String text) {
        this.kind   = kind;
        this.prefix = prefix;
        this.text   = text;
    }

    /**
     * @return the entry's kind
     */
    public @NotNull ChatEntryKind getKind() {
        return this.kind;
    }

    /**
     * @return the sender's prefix, empty for an entry without a sender
     */
    public @NotNull String getPrefix() {
        return this.prefix;
    }

    /**
     * @return the text
     */
    public @NotNull String getText() {
        return this.text;
    }

    /**
     * {@return a string representation of the object}
     * <p>
     * Satisfying this method's contract implies a non-{@code null} result must be returned.
     *
     * @apiNote In general, the {@code toString} method returns a string that "textually represents" this object. The result should be a concise
     * but informative representation that is easy for a person to read. It is recommended that all subclasses override this method. The string
     * output is not necessarily stable over time or across JVM invocations.
     * @implSpec Returns the kind, the prefix and the text.
     */
    @Override
    public @NotNull String toString() {
        return this.kind + " " + this.prefix + this.text;
    }
}
