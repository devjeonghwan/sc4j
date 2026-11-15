package org.fogofwarlabs.sc4j.engine.client.host;

import org.jetbrains.annotations.NotNull;

/**
 * Receives the composition events of a window's {@link TextInputMethod} while text input is enabled, on the thread that polls the window's
 * events. One platform message may bring a committed result and then the next composition.
 */
public interface TextCompositionListener {
    /**
     * Handles composed text that is final: the edit control takes it as typed text.
     *
     * @param text the result text, possibly empty
     */
    void onCompositionCommitted(@NotNull String text);

    /**
     * Handles a changed composition: the edit control shows it in place of the previous one.
     *
     * @param composition the composition; an empty text ends it
     */
    void onCompositionChanged(@NotNull TextComposition composition);

    /**
     * Handles a composition the platform could not report: the edit control forgets the composition it shows.
     */
    void onCompositionCanceled();
}
