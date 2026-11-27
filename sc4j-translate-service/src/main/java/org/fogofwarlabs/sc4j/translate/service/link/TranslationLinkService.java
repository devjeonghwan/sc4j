package org.fogofwarlabs.sc4j.translate.service.link;

import org.fogofwarlabs.sc4j.launcher.link.LinkRequestHandler;
import org.fogofwarlabs.sc4j.launcher.link.LinkSession;
import org.fogofwarlabs.sc4j.translate.TranslationContext;
import org.fogofwarlabs.sc4j.translate.TranslationRequest;
import org.fogofwarlabs.sc4j.translate.Translator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers the translation requests that the runtimes send over the launcher link with the launcher's {@link Translator}: the handler of
 * the service {@value #SERVICE_NAME}. A payload that is not a request answers with a failure.
 * <p>
 * It keeps the contexts each session sent, at most {@link #MAXIMUM_SESSION_CONTEXTS} per session, the least recently used ones dropped
 * first, and drops them when the session closes. A request that names a context id the session's contexts lack is answered with the unknown
 * context answer, after which the runtime sends the context again.
 */
public final class TranslationLinkService implements LinkRequestHandler {
    /**
     * The name of the translation service on the launcher link.
     */
    public static final @NotNull String SERVICE_NAME = "translation";

    /**
     * The number of contexts kept per session.
     */
    public static final int MAXIMUM_SESSION_CONTEXTS = 64;

    private static @NotNull Map<@NotNull String, @NotNull TranslationContext> createContexts() {
        return new LinkedHashMap<>(MAXIMUM_SESSION_CONTEXTS, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(@NotNull Map.Entry<@NotNull String, @NotNull TranslationContext> eldest) {
                return this.size() > MAXIMUM_SESSION_CONTEXTS;
            }
        };
    }


    private final @NotNull Translator translator;
    private final @NotNull Map<@NotNull LinkSession, @NotNull Map<@NotNull String, @NotNull TranslationContext>> sessionContexts;

    /**
     * Creates a {@link TranslationLinkService} instance.
     *
     * @param translator the translator that answers the requests
     */
    public TranslationLinkService(@NotNull Translator translator) {
        this.translator      = translator;
        this.sessionContexts = new ConcurrentHashMap<>();
    }

    /**
     * Handles a request. A future that fails, or an exception thrown here, answers the client with a failure that carries the message.
     *
     * @param session the session of the client that sent the request
     * @param payload the request payload
     * @return the future of the response payload
     */
    @Override
    public @NotNull CompletableFuture<byte @NotNull []> handle(@NotNull LinkSession session, byte @NotNull [] payload) {
        Map<String, TranslationContext> contexts = this.sessionContexts.computeIfAbsent(session, newSession -> createContexts());
        TranslationRequest request;

        try {
            request = TranslationCodecUtils.decodeRequest(payload, contexts);
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        if (request == null) {
            return CompletableFuture.completedFuture(TranslationCodecUtils.encodeUnknownContext());
        }

        return this.translator.translate(request).thenApply(TranslationCodecUtils::encodeTranslation);
    }

    /**
     * Called on the session's reader thread after the connection of a session closed, once per session that was accepted; no request of
     * the session follows.
     * <p>
     * Drops the contexts of the session.
     *
     * @param session the closed session
     */
    @Override
    public void onSessionClosed(@NotNull LinkSession session) {
        this.sessionContexts.remove(session);
    }
}
