package org.fogofwarlabs.sc4j.translate.service.link;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.launcher.link.LauncherLinkClient;
import org.fogofwarlabs.sc4j.translate.*;
import org.fogofwarlabs.sc4j.translate.service.TranslationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Translates through the {@value TranslationLinkService#SERVICE_NAME} service of the launcher that started the runtime. Once the link is
 * closed, and for a request that failed because the link closed, it translates with a local {@link TranslationService}, which it creates on
 * the first such request from its factory, so that the models are loaded in the runtime only without a launcher; without a factory those
 * requests answer {@link TranslationStatus#UNAVAILABLE}. A request that the launcher answers with a failure while the link is open, or whose
 * answer cannot be read, answers {@link TranslationStatus#UNAVAILABLE} too.
 * <p>
 * A request's {@link TranslationContext} is sent whole the first time its id is used on the link, later requests carry the id only. When
 * the launcher does not know an id, the request is sent again with the whole context.
 */
public final class LinkTranslator implements Translator, AutoCloseable {
    private static final @NotNull Logger LOGGER = Logger.of(LinkTranslator.class);

    private static @NotNull Translation createUnavailable(@NotNull TranslationRequest request) {
        return Translation.untranslated(request.getText(), null, TranslationStatus.UNAVAILABLE);
    }


    private final @NotNull LauncherLinkClient link;
    private final @Nullable Supplier<@NotNull TranslationService> localFactory;

    private final @NotNull Object lock;
    private final @NotNull Set<@NotNull String> sentContextIds;
    private @Nullable TranslationService local;
    private boolean closed;

    /**
     * Creates a {@link LinkTranslator} instance.
     *
     * @param link         the link to the launcher, which the caller closes
     * @param localFactory creates the local service used once the link is closed, or {@code null} when there is none
     */
    public LinkTranslator(@NotNull LauncherLinkClient link, @Nullable Supplier<@NotNull TranslationService> localFactory) {
        this.link         = link;
        this.localFactory = localFactory;

        this.lock           = new Object();
        this.sentContextIds = new HashSet<>();
        this.local          = null;
        this.closed         = false;
    }

    /**
     * @return {@code true} if the translator has created its local service, {@code false} otherwise
     */
    public boolean isLocal() {
        synchronized (this.lock) {
            return this.local != null;
        }
    }

    /**
     * Translates a text.
     *
     * @param request the request
     * @return the future of the translation, completed on a worker thread or at once
     */
    @Override
    public @NotNull CompletableFuture<@NotNull Translation> translate(@NotNull TranslationRequest request) {
        if (this.link.isClosed()) {
            return this.translateLocally(request);
        }

        CompletableFuture<Translation> answer = new CompletableFuture<>();

        this.send(request, false, answer);

        return answer;
    }

    /**
     * Sends a request to the launcher and completes the answer with its translation. The context goes whole when it is resent or its id
     * was not sent on the link before; a request whose context the launcher does not know is sent once more with the whole context.
     */
    private void send(@NotNull TranslationRequest request, boolean resent, @NotNull CompletableFuture<@NotNull Translation> answer) {
        TranslationContext context = request.getContext();
        CompletableFuture<byte[]> response;

        // The request is written under the lock, so that the launcher reads a whole context before the requests that carry its id.
        synchronized (this.lock) {
            boolean fullContext = context != null && (this.sentContextIds.add(context.getId()) || resent);

            response = this.link.request(TranslationLinkService.SERVICE_NAME, TranslationCodecUtils.encodeRequest(request, fullContext));
        }

        response.whenComplete((payload, failure) -> {
            if (failure == null) {
                this.receive(request, resent, payload, answer);
            } else if (this.link.isClosed()) {
                this.translateLocally(request).thenAccept(answer::complete);
            } else {
                LOGGER.warning("TRANSLATION: the launcher failed a request: {0}", failure.getMessage());
                answer.complete(createUnavailable(request));
            }
        });
    }

    private void receive(@NotNull TranslationRequest request, boolean resent, byte @NotNull [] payload,
                         @NotNull CompletableFuture<@NotNull Translation> answer) {
        Translation translation;

        try {
            translation = TranslationCodecUtils.decodeAnswer(payload);
        } catch (IOException exception) {
            LOGGER.warning("TRANSLATION: the launcher's answer cannot be read: {0}", exception.getMessage());
            answer.complete(createUnavailable(request));

            return;
        }

        if (translation != null) {
            answer.complete(translation);
        } else if (resent) {
            LOGGER.warning("TRANSLATION: the launcher does not keep the context of a request");
            answer.complete(createUnavailable(request));
        } else {
            this.send(request, true, answer);
        }
    }

    private @NotNull CompletableFuture<@NotNull Translation> translateLocally(@NotNull TranslationRequest request) {
        TranslationService service;

        synchronized (this.lock) {
            if (this.closed || this.localFactory == null) {
                return CompletableFuture.completedFuture(createUnavailable(request));
            }

            if (this.local == null) {
                this.local = this.localFactory.get();
                LOGGER.info("TRANSLATION: the launcher link is closed, translating here with {0}", this.local.getPreset().getId());
            }

            service = this.local;
        }

        return service.translate(request);
    }

    /**
     * Closes the local service when it was created; later requests answer {@link TranslationStatus#UNAVAILABLE} unless the link is open.
     */
    @Override
    public void close() {
        TranslationService service;

        synchronized (this.lock) {
            service     = this.local;
            this.local  = null;
            this.closed = true;
        }

        if (service != null) {
            service.close();
        }
    }
}
