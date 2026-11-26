package org.fogofwarlabs.sc4j.translate.service.nllb;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.translate.service.model.ModelException;
import org.fogofwarlabs.sc4j.translate.service.model.SequenceModel;
import org.fogofwarlabs.sc4j.translate.service.model.TranslationModel;
import org.fogofwarlabs.sc4j.translate.service.sentencepiece.SentencePieceTokenizer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;

/**
 * Translates with an NLLB model: the input is the source language token, the SentencePiece pieces of the text and the end token; the
 * output starts with the forced target language token. Model ids are the SentencePiece ids plus the fairseq offset {@code 1}, with the
 * fairseq special tokens {@code <s>}, {@code <pad>}, {@code </s>} and {@code <unk>} at {@code 0} to {@code 3} and the language tokens after
 * the vocabulary.
 */
public final class NllbTranslationModel implements TranslationModel {
    /**
     * The token that starts the decoder input and ends a sequence, {@code </s>}.
     */
    public static final int END_TOKEN = 2;

    private static final int FAIRSEQ_OFFSET = 1;
    private static final int UNKNOWN_TOKEN = 3;
    private static final int FIRST_PIECE_TOKEN = 4;
    private static final int SENTENCEPIECE_UNKNOWN_ID = 0;
    private static final int MAXIMUM_INPUT_TOKENS = 512;
    /**
     * The output length allowed per input token, and the output tokens allowed beyond that, which end a greedy decoding that repeats itself.
     */
    private static final int OUTPUT_TOKENS_PER_INPUT_TOKEN = 2;
    private static final int EXTRA_OUTPUT_TOKENS = 16;
    private static final int LANGUAGE_AND_END_TOKENS = 2;

    private final @NotNull SentencePieceTokenizer tokenizer;
    private final @NotNull SequenceModel sequenceModel;
    private final int languageTokenBase;

    /**
     * Creates a {@link NllbTranslationModel} instance that owns a sequence model.
     *
     * @param tokenizer     the SentencePiece tokenizer of the model
     * @param sequenceModel the encoder and decoder, closed with this model
     */
    public NllbTranslationModel(@NotNull SentencePieceTokenizer tokenizer, @NotNull SequenceModel sequenceModel) {
        this.tokenizer         = tokenizer;
        this.sequenceModel     = sequenceModel;
        this.languageTokenBase = tokenizer.getPieceCount() + FAIRSEQ_OFFSET;
    }

    /**
     * @return the largest number of tokens one input may have, language and end tokens included
     */
    @Override
    public int getMaximumInputTokens() {
        return MAXIMUM_INPUT_TOKENS;
    }

    /**
     * Counts the tokens of a text as {@link #translate} feeds it to the model, language and end tokens included.
     *
     * @param text the text
     * @return the number of tokens
     */
    @Override
    public int countTokens(@NotNull String text) {
        return this.tokenizer.encode(text).length + LANGUAGE_AND_END_TOKENS;
    }

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
    @Override
    public @NotNull String translate(@NotNull String text, @NotNull Locale source, @NotNull Locale target) throws ModelException {
        int sourceToken = this.findLanguageToken(source);
        int targetToken = this.findLanguageToken(target);
        int[] pieces = this.tokenizer.encode(text);
        int pieceCount = Math.min(pieces.length, MAXIMUM_INPUT_TOKENS - LANGUAGE_AND_END_TOKENS);
        int[] input = new int[pieceCount + LANGUAGE_AND_END_TOKENS];

        input[0] = sourceToken;

        for (int index = 0; index < pieceCount; index++) {
            input[index + 1] = pieces[index] == SENTENCEPIECE_UNKNOWN_ID ? UNKNOWN_TOKEN : pieces[index] + FAIRSEQ_OFFSET;
        }

        input[pieceCount + 1] = END_TOKEN;

        int[] output = this.sequenceModel.generate(input, targetToken, input.length * OUTPUT_TOKENS_PER_INPUT_TOKEN + EXTRA_OUTPUT_TOKENS);
        int[] outputPieces = new int[output.length];
        int outputPieceCount = 0;

        for (int token : output) {
            if (token >= FIRST_PIECE_TOKEN && token < this.languageTokenBase) {
                outputPieces[outputPieceCount++] = token - FAIRSEQ_OFFSET;
            }
        }

        return this.tokenizer.decode(Arrays.copyOf(outputPieces, outputPieceCount));
    }

    private int findLanguageToken(@NotNull Locale language) {
        String code = NllbLanguages.findCode(language);

        if (code == null) {
            throw Exceptions.newIllegalArgumentException("NLLB does not support the language {0}", language);
        }

        return this.languageTokenBase + NllbLanguages.findIndex(code);
    }

    /**
     * Releases the native resources.
     */
    @Override
    public void close() {
        this.sequenceModel.close();
    }
}
