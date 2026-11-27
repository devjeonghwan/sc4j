package org.fogofwarlabs.sc4j.launcher.link;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Answers the requests of one service of a {@link LauncherLinkServer}. It is called on the reader thread of the client that sent the
 * request, so it returns at once and completes the future later, on any thread; the requests of one session reach it in the order the client
 * sent them.
 */
@FunctionalInterface
public interface LinkRequestHandler {
    /**
     * Handles a request. A future that fails, or an exception thrown here, answers the client with a failure that carries the message.
     *
     * @param session the session of the client that sent the request
     * @param payload the request payload
     * @return the future of the response payload
     */
    @NotNull CompletableFuture<byte @NotNull []> handle(@NotNull LinkSession session, byte @NotNull [] payload);

    /**
     * Called on the session's reader thread after the connection of a session closed, once per session that was accepted; no request of
     * the session follows. The default does nothing.
     *
     * @param session the closed session
     */
    default void onSessionClosed(@NotNull LinkSession session) {
    }
}
