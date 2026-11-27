package org.fogofwarlabs.sc4j.translate.service.indextranslate;

import org.fogofwarlabs.sc4j.translate.service.llama.TextGenerator;
import org.fogofwarlabs.sc4j.translate.service.model.ModelException;
import org.fogofwarlabs.sc4j.translate.service.model.TranslationInput;
import org.fogofwarlabs.sc4j.translate.service.model.TranslationModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates with Index-Translate: each text gets a prompt of its own (the constrained instruction when its context gives constraints, else
 * the plain one) and the texts of a batch are generated greedily as parallel sequences, as many at once as the generator takes. The output
 * of a text is limited to twice its tokens and a fixed margin. A constrained prompt that does not leave a sequence room for that output
 * falls back to the plain prompt.
 */
public final class IndexTranslationModel implements TranslationModel {
    /**
     * The largest number of tokens of one input; the service splits longer texts at sentence boundaries.
     */
    public static final int MAXIMUM_INPUT_TOKENS = 128;

    private static final int OUTPUT_TOKENS_PER_INPUT_TOKEN = 2;
    private static final int EXTRA_OUTPUT_TOKENS = 16;

    private final @NotNull TextGenerator generator;

    /**
     * Creates a {@link IndexTranslationModel} instance that owns a text generator.
     *
     * @param generator the generator of the loaded model, closed with this model
     */
    public IndexTranslationModel(@NotNull TextGenerator generator) {
        this.generator = generator;
    }

    /**
     * @return the largest number of tokens one input may have, as {@link #countTokens} counts them
     */
    @Override
    public int getMaximumInputTokens() {
        return MAXIMUM_INPUT_TOKENS;
    }

    /**
     * Counts the tokens of a text as {@link #translate} feeds it to the model, the prompt around it not included.
     *
     * @param text the text
     * @return the number of tokens
     */
    @Override
    public int countTokens(@NotNull String text) {
        return this.generator.countTokens(text);
    }

    /**
     * Translates a batch of texts that fit {@link #getMaximumInputTokens()}; a longer input whose prompt does not fit a sequence fails.
     *
     * @param inputs the texts with their languages and contexts
     * @return the translated texts, in the order of the inputs
     * @throws ModelException           if the inference fails
     * @throws IllegalArgumentException if the preset does not support a language
     */
    @Override
    public @NotNull List<@NotNull String> translate(@NotNull List<@NotNull TranslationInput> inputs) throws ModelException {
        List<String> prompts = new ArrayList<>(inputs.size());
        int[] outputLimits = new int[inputs.size()];

        for (int index = 0; index < inputs.size(); index++) {
            TranslationInput input = inputs.get(index);
            int outputLimit = this.countTokens(input.getText()) * OUTPUT_TOKENS_PER_INPUT_TOKEN + EXTRA_OUTPUT_TOKENS;
            String prompt = IndexTranslatePromptUtils.createPrompt(input, true);

            if (input.getContext() != null && this.generator.countPromptTokens(prompt) + outputLimit > this.generator.getSequenceContextSize()) {
                prompt = IndexTranslatePromptUtils.createPrompt(input, false);
            }

            prompts.add(prompt);
            outputLimits[index] = outputLimit;
        }

        List<String> translations = new ArrayList<>(inputs.size());
        int groupSize = this.generator.getSequenceCount();

        for (int start = 0; start < prompts.size(); start += groupSize) {
            int end = Math.min(prompts.size(), start + groupSize);
            int maximumTokens = 0;

            for (int index = start; index < end; index++) {
                maximumTokens = Math.max(maximumTokens, outputLimits[index]);
            }

            for (String output : this.generator.generate(prompts.subList(start, end), maximumTokens)) {
                translations.add(IndexTranslatePromptUtils.cleanOutput(output));
            }
        }

        return translations;
    }

    /**
     * Releases the native resources.
     */
    @Override
    public void close() {
        this.generator.close();
    }
}
