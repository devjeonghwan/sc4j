package org.fogofwarlabs.sc4j.translate.service;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.base.log.LogLevel;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.translate.*;
import org.fogofwarlabs.sc4j.translate.service.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Translates with the models of one preset on one worker thread of low priority. Requests wait in a queue, {@link
 * org.fogofwarlabs.sc4j.translate.TranslationPriority#CHAT} before {@link org.fogofwarlabs.sc4j.translate.TranslationPriority#BATCH}, each
 * priority in arrival order. The models are loaded when a request first needs them and closed after the idle time without requests; a
 * model that fails to load is tried again after the idle time.
 * <p>
 * A request is decided in this order: the text runs between the protected spans are what is translated, and a text longer than the
 * limit or without letters in them is skipped; the source language is the top label of the preset's language identifier for the runs; a
 * source language equal to the target is the same language. A preset without identifier, or a label that is no language of the preset,
 * gives {@link TranslationStatus#UNAVAILABLE}. Each run is translated sentence by sentence, a sentence longer than the model's input limit
 * in pieces ({@link TextSplitter}), and its translation is kept in a {@link TranslationCache}.
 */
public final class TranslationService implements Translator, AutoCloseable {
    /**
     * The time without requests after which the models are closed.
     */
    public static final @NotNull Duration IDLE_TIME = Duration.ofMinutes(10);

    /**
     * The longest text translated, in characters; longer texts are skipped.
     */
    public static final int MAXIMUM_TEXT_LENGTH = 4096;

    private static final @NotNull Logger LOGGER = Logger.of(TranslationService.class);
    private static final @NotNull String WORKER_NAME = "SC4J translation";
    private static final long CLOSE_WAIT_MILLIS = 10_000L;
    private static final @NotNull String SPACE = " ";
    /**
     * The languages written without spaces between sentences.
     */
    private static final @NotNull Set<@NotNull String> UNSPACED_LANGUAGES = Set.of("ja", "zh", "yue", "th", "lo", "km", "my");

    /**
     * A queued request with its future; a task without request stops the worker.
     */
    private static final class Task {
        private final @Nullable TranslationRequest request;
        private final @NotNull CompletableFuture<@NotNull Translation> future;
        private final long sequence;

        private Task(@Nullable TranslationRequest request, @NotNull CompletableFuture<@NotNull Translation> future, long sequence) {
            this.request  = request;
            this.future   = future;
            this.sequence = sequence;
        }

        private int getOrder() {
            return this.request == null ? -1 : this.request.getPriority().ordinal();
        }
    }


    private static boolean hasLetters(@NotNull String text) {
        return text.codePoints().anyMatch(Character::isLetter);
    }

    private static @NotNull Translation createUntranslated(@NotNull TranslationRequest request, @Nullable Locale source, @NotNull TranslationStatus status) {
        return Translation.untranslated(request.getText(), source, status);
    }


    /**
     * Finds the runs to translate: the text between the protected spans without its leading and trailing white space, where it has a
     * letter.
     *
     * @return the start and end index of each run, in text order
     */
    private static @NotNull List<int @NotNull []> findRuns(@NotNull TranslationRequest request) {
        String text = request.getText();
        List<int[]> runs = new ArrayList<>();
        int start = 0;

        for (int index = 0; index <= request.getProtectedSpans().size(); index++) {
            ProtectedSpan span = index < request.getProtectedSpans().size() ? request.getProtectedSpans().get(index) : null;
            int end = span == null ? text.length() : span.getStart();
            int runStart = start;
            int runEnd = end;

            while (runStart < runEnd && Character.isWhitespace(text.charAt(runStart))) {
                runStart++;
            }

            while (runEnd > runStart && Character.isWhitespace(text.charAt(runEnd - 1))) {
                runEnd--;
            }

            if (hasLetters(text.substring(runStart, runEnd))) {
                runs.add(new int[]{runStart, runEnd});
            }

            start = span == null ? text.length() : span.getEnd();
        }

        return runs;
    }


    private final @NotNull TranslationPreset preset;
    private final @NotNull Path presetFolder;
    private final long idleNanos;

    private final @NotNull TranslationCache cache;
    private final @NotNull PriorityBlockingQueue<@NotNull Task> queue;
    private final @NotNull AtomicLong sequence;
    private final @NotNull Thread worker;
    private volatile boolean closed;

    /**
     * The state of the worker thread: the loaded models, when they last failed to load, and the time of the last request.
     */
    private @Nullable TranslationModel model;
    private @Nullable LanguageIdentifier identifier;
    private boolean identifierLoaded;
    private long modelFailedNanos;
    private long identifierFailedNanos;
    private long lastRequestNanos;

    /**
     * Creates a {@link TranslationService} instance for a preset installed in the translation storage, with a disk cache there and the
     * idle time {@link #IDLE_TIME}, and starts its worker thread. No model is loaded until a request needs it.
     *
     * @param preset  the preset
     * @param storage the translation storage of the install
     */
    public TranslationService(@NotNull TranslationPreset preset, @NotNull TranslationStorage storage) {
        this(preset, storage.getPresetFolder(preset), storage.getCacheFolder(), IDLE_TIME);
    }

    /**
     * Creates a {@link TranslationService} instance and starts its worker thread. No model is loaded until a request needs it.
     *
     * @param preset       the preset
     * @param presetFolder the folder of the preset's installed files
     * @param cacheFolder  the folder of the disk cache, or {@code null} to keep translations in memory only
     * @param idleTime     the time without requests after which the models are closed
     */
    public TranslationService(@NotNull TranslationPreset preset, @NotNull Path presetFolder, @Nullable Path cacheFolder, @NotNull Duration idleTime) {
        this.preset       = preset;
        this.presetFolder = presetFolder;
        this.idleNanos    = idleTime.toNanos();

        this.cache    = new TranslationCache(cacheFolder);
        this.queue    = new PriorityBlockingQueue<>(16, Comparator.comparingInt(Task::getOrder).thenComparingLong(task -> task.sequence));
        this.sequence = new AtomicLong();
        this.worker   = new Thread(this::runWorker, WORKER_NAME);
        this.closed   = false;

        this.model                 = null;
        this.identifier            = null;
        this.identifierLoaded      = false;
        this.modelFailedNanos      = 0L;
        this.identifierFailedNanos = 0L;
        this.lastRequestNanos      = System.nanoTime();

        this.worker.setDaemon(true);
        this.worker.setPriority(Thread.MIN_PRIORITY);
        this.worker.start();
    }

    /**
     * @return the preset
     */
    public @NotNull TranslationPreset getPreset() {
        return this.preset;
    }

    /**
     * Translates a text.
     *
     * @param request the request
     * @return the future of the translation, completed on a worker thread or at once
     */
    @Override
    public @NotNull CompletableFuture<@NotNull Translation> translate(@NotNull TranslationRequest request) {
        if (this.closed) {
            return CompletableFuture.completedFuture(createUntranslated(request, null, TranslationStatus.UNAVAILABLE));
        }

        Task task = new Task(request, new CompletableFuture<>(), this.sequence.getAndIncrement());

        this.queue.add(task);

        // A request that raced with close is answered here unless the worker took it.
        if (this.closed && this.queue.remove(task)) {
            task.future.complete(createUntranslated(request, null, TranslationStatus.UNAVAILABLE));
        }

        return task.future;
    }

    private void runWorker() {
        while (true) {
            Task task;

            try {
                task = this.queue.poll(this.findIdleWaitNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException exception) {
                break;
            }

            if (task == null) {
                this.closeModelsIfIdle();

                continue;
            }

            if (task.request == null) {
                break;
            }

            this.lastRequestNanos = System.nanoTime();

            Translation translation;

            try {
                translation = this.process(task.request);
            } catch (RuntimeException exception) {
                LOGGER.exception(LogLevel.WARNING, exception, "The translation failed");
                translation = createUntranslated(task.request, null, TranslationStatus.UNAVAILABLE);
            }

            task.future.complete(translation);
        }

        this.closeModels();
    }

    private long findIdleWaitNanos() {
        if (this.model == null && this.identifier == null) {
            return Long.MAX_VALUE;
        }

        return Math.max(0L, this.lastRequestNanos + this.idleNanos - System.nanoTime());
    }

    private @NotNull Translation process(@NotNull TranslationRequest request) {
        String text = request.getText();
        String targetCode = this.preset.findCode(request.getTarget());

        if (targetCode == null) {
            return createUntranslated(request, null, TranslationStatus.UNAVAILABLE);
        }

        if (text.length() > MAXIMUM_TEXT_LENGTH) {
            return createUntranslated(request, null, TranslationStatus.SKIPPED);
        }

        List<int[]> runs = findRuns(request);
        StringBuilder joinedRuns = new StringBuilder();

        for (int[] run : runs) {
            if (!joinedRuns.isEmpty()) {
                joinedRuns.append(SPACE);
            }

            joinedRuns.append(text, run[0], run[1]);
        }

        String content = joinedRuns.toString();

        if (runs.isEmpty()) {
            return createUntranslated(request, null, TranslationStatus.SKIPPED);
        }

        Locale source = this.identify(content);

        if (source == null) {
            return createUntranslated(request, null, TranslationStatus.UNAVAILABLE);
        }

        String sourceCode = this.preset.findCode(source);

        if (sourceCode == null) {
            return createUntranslated(request, source, TranslationStatus.UNAVAILABLE);
        }

        if (sourceCode.equals(targetCode)) {
            return createUntranslated(request, source, TranslationStatus.SAME_LANGUAGE);
        }

        StringBuilder translated = new StringBuilder(text.length());
        int position = 0;

        try {
            for (int[] run : runs) {
                translated.append(text, position, run[0]);
                translated.append(this.translateRun(text.substring(run[0], run[1]), source, sourceCode, request.getTarget(), targetCode));
                position = run[1];
            }
        } catch (ModelException exception) {
            LOGGER.warning("The translation model failed: {0}", exception.getMessage());

            return createUntranslated(request, source, TranslationStatus.UNAVAILABLE);
        }

        translated.append(text, position, text.length());

        return new Translation(text, translated.toString(), source, TranslationStatus.TRANSLATED);
    }

    /**
     * Identifies the language of the text runs: the identifier's top label.
     *
     * @return the language, or {@code null} when the preset has no identifier, it cannot be loaded or its label is no language of the preset
     */
    private @Nullable Locale identify(@NotNull String content) {
        LanguageIdentifier languageIdentifier = this.loadIdentifier();
        IdentifiedLanguage identified = languageIdentifier == null ? null : languageIdentifier.identify(content.replace('\n', ' ').replace('\r', ' '));

        return identified == null ? null : this.preset.findLanguage(identified.getCode());
    }

    /**
     * Translates one run: from the cache, else with the model, which is loaded here when needed.
     *
     * @throws ModelException if the model cannot be loaded or fails
     */
    private @NotNull String translateRun(@NotNull String run, @NotNull Locale source, @NotNull String sourceCode, @NotNull Locale target,
                                         @NotNull String targetCode) throws ModelException {
        String cached = this.cache.find(this.preset.getId(), sourceCode, targetCode, run);

        if (cached != null) {
            return cached;
        }

        TranslationModel translationModel = this.loadModel();

        if (translationModel == null) {
            throw new ModelException(Exceptions.format("The translation model of {0} is not loaded", this.preset.getId()));
        }

        String separator = UNSPACED_LANGUAGES.contains(target.getLanguage()) ? "" : SPACE;
        StringBuilder translated = new StringBuilder();

        for (String piece : TextSplitter.split(run, source, translationModel::countTokens, translationModel.getMaximumInputTokens())) {
            String sentence = piece.strip();

            if (sentence.isEmpty()) {
                continue;
            }

            String translatedPiece = hasLetters(sentence) ? translationModel.translate(sentence, source, target) : sentence;

            if (!translated.isEmpty() && !translatedPiece.isEmpty()) {
                translated.append(separator);
            }

            translated.append(translatedPiece);
        }

        this.cache.store(this.preset.getId(), sourceCode, targetCode, run, translated.toString());

        return translated.toString();
    }

    private @Nullable TranslationModel loadModel() {
        if (this.model != null || this.isRetryWaiting(this.modelFailedNanos)) {
            return this.model;
        }

        try {
            this.model = this.preset.createTranslationModel(this.presetFolder);
        } catch (ModelException | RuntimeException | LinkageError exception) {
            LOGGER.warning("Cannot load the translation model of {0}: {1}", this.preset.getId(), exception.getMessage());
            this.modelFailedNanos = System.nanoTime();
        }

        return this.model;
    }

    private @Nullable LanguageIdentifier loadIdentifier() {
        if (this.identifierLoaded || this.isRetryWaiting(this.identifierFailedNanos)) {
            return this.identifier;
        }

        try {
            this.identifier       = this.preset.createLanguageIdentifier(this.presetFolder);
            this.identifierLoaded = true;
        } catch (ModelException | RuntimeException exception) {
            LOGGER.warning("Cannot load the language identifier of {0}: {1}", this.preset.getId(), exception.getMessage());
            this.identifierFailedNanos = System.nanoTime();
        }

        return this.identifier;
    }

    private boolean isRetryWaiting(long failedNanos) {
        return failedNanos != 0L && System.nanoTime() - failedNanos < this.idleNanos;
    }

    private void closeModelsIfIdle() {
        if (System.nanoTime() - this.lastRequestNanos >= this.idleNanos) {
            this.closeModels();
        }
    }

    private void closeModels() {
        LanguageIdentifier loadedIdentifier = this.identifier;
        TranslationModel loadedModel = this.model;

        this.identifier       = null;
        this.identifierLoaded = false;
        this.model            = null;

        IgnoreException.execute(
                () -> {
                    if (loadedIdentifier != null) {
                        loadedIdentifier.close();
                    }
                },
                () -> {
                    if (loadedModel != null) {
                        loadedModel.close();
                    }
                }
        );
    }

    /**
     * Stops the worker thread after the request it is translating, closes the models and answers the queued requests with
     * {@link TranslationStatus#UNAVAILABLE}.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        this.queue.add(new Task(null, new CompletableFuture<>(), this.sequence.getAndIncrement()));

        try {
            this.worker.join(CLOSE_WAIT_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        Task task = this.queue.poll();

        while (task != null) {
            if (task.request != null) {
                task.future.complete(createUntranslated(task.request, null, TranslationStatus.UNAVAILABLE));
            }

            task = this.queue.poll();
        }
    }
}
