package org.fogofwarlabs.sc4j.translate.service.model;

import org.jetbrains.annotations.NotNull;

/**
 * Converts text to the token ids of a model's vocabulary and back.
 */
public interface TextTokenizer {
    /**
     * Converts text to token ids.
     *
     * @param text the text
     * @return the token ids, empty when the text has nothing but spaces
     */
    int @NotNull [] encode(@NotNull String text);

    /**
     * Converts token ids to text; ids of control tokens and of the unknown token give no text.
     *
     * @param tokens the token ids
     * @return the text
     */
    @NotNull String decode(int @NotNull [] tokens);
}
