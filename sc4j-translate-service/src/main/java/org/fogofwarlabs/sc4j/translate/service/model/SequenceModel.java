package org.fogofwarlabs.sc4j.translate.service.model;

import org.jetbrains.annotations.NotNull;

/**
 * Generates an output token sequence from an input token sequence with an encoder and a decoder. It owns native resources until it is
 * closed and is used by one thread at a time.
 */
public interface SequenceModel extends AutoCloseable {
    /**
     * Generates the output sequence greedily: the token of the highest score at each step, the first one forced, until the end token or the
     * length limit.
     *
     * @param input            the input token ids
     * @param forcedFirstToken the token the output starts with, such as a target language token
     * @param maximumLength    the largest number of output tokens, the forced one included
     * @return the output token ids, the forced first token included, without the decoder start token and the end token
     * @throws ModelException if the inference fails
     */
    int @NotNull [] generate(int @NotNull [] input, int forcedFirstToken, int maximumLength) throws ModelException;

    /**
     * Releases the native resources.
     */
    @Override
    void close();
}
