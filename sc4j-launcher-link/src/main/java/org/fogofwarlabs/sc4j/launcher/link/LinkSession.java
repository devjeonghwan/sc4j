package org.fogofwarlabs.sc4j.launcher.link;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one accepted client connection of a {@link LauncherLinkServer}, from its accepted hello until it closes. A service keeps the
 * state of a client under its session and drops it in {@link LinkRequestHandler#onSessionClosed}.
 */
public final class LinkSession {
    private final long number;

    /**
     * Creates a {@link LinkSession} instance.
     *
     * @param number the number of the session, counted from {@code 1} in the order the server accepted its clients
     */
    LinkSession(long number) {
        this.number = number;
    }

    /**
     * @return the number of the session, counted from {@code 1} in the order the server accepted its clients
     */
    public long getNumber() {
        return this.number;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return the session number
     */
    @Override
    public @NotNull String toString() {
        return "session " + this.number;
    }
}
