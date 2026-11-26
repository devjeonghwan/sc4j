package org.fogofwarlabs.sc4j.translate.service.model;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Translates text between the languages of a loaded preset. It owns the native resources of its sequence model until it is closed and is
 * used by one thread at a time.
 */
public interface TranslationModel extends AutoCloseable {
    /**
     * @return the largest number of tokens one input may have, language and end tokens included
     */
    int getMaximumInputTokens();

    /**
     * Counts the tokens of a text as {@link #translate} feeds it to the model, language and end tokens included.
     *
     * @param text the text
     * @return the number of tokens
     */
    int countTokens(@NotNull String text);

    /**
     * Translates a text that fits {@link #getMaximumInputTokens()}; longer input is cut there.
     *
     * @param text   the text
     * @param source the language of the text, one the preset supports
     * @param target the language to translate into, one the preset supports
     * @return the translated text
     * @throws ModelException           if the inference fails
     * @throws IllegalArgumentException if the preset does not support a language
     */
    @NotNull String translate(@NotNull String text, @NotNull Locale source, @NotNull Locale target) throws ModelException;

    /**
     * Releases the native resources.
     */
    @Override
    void close();
}
