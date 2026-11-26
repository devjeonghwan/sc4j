package org.fogofwarlabs.sc4j.translate.service.onnx;

import ai.onnxruntime.*;
import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.translate.service.model.ModelException;
import org.fogofwarlabs.sc4j.translate.service.model.SequenceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.*;

/**
 * Runs an encoder-decoder model exported to ONNX with a merged decoder: the encoder ({@code input_ids}, {@code attention_mask} to
 * {@code last_hidden_state}) runs once, then the decoder ({@code input_ids}, {@code encoder_attention_mask},
 * {@code encoder_hidden_states}, {@code past_key_values.*} and {@code use_cache_branch} to {@code logits} and {@code present.*}) runs one
 * token at a time. The first decoder step runs without cache on empty past values; later steps feed the decoder values of the previous step
 * and the encoder values of the first step back. Decoding is greedy.
 * <p>
 * The sessions use two intra-op threads and one inter-op thread. The logits are written into a buffer of this model, so a step allocates
 * no logits array.
 */
public final class OnnxSequenceModel implements SequenceModel {
    private static final @NotNull String INPUT_IDS = "input_ids";
    private static final @NotNull String ATTENTION_MASK = "attention_mask";
    private static final @NotNull String LAST_HIDDEN_STATE = "last_hidden_state";
    private static final @NotNull String ENCODER_ATTENTION_MASK = "encoder_attention_mask";
    private static final @NotNull String ENCODER_HIDDEN_STATES = "encoder_hidden_states";
    private static final @NotNull String USE_CACHE_BRANCH = "use_cache_branch";
    private static final @NotNull String LOGITS = "logits";
    private static final @NotNull String PAST_PREFIX = "past_key_values.";
    private static final @NotNull String PRESENT_PREFIX = "present.";
    private static final @NotNull String ENCODER_CACHE_PART = ".encoder.";

    private static final int INTRA_OP_THREADS = 2;
    private static final int INTER_OP_THREADS = 1;
    private static final int BATCH_SIZE = 1;
    private static final int HEAD_DIMENSION = 1;
    private static final int HEAD_SIZE_DIMENSION = 3;
    private static final int CACHE_RANK = 4;
    private static final int LOGITS_RANK = 3;
    private static final int VOCABULARY_DIMENSION = 2;

    /**
     * Opens the encoder and the merged decoder.
     *
     * @param encoderFile       the encoder model file
     * @param decoderFile       the merged decoder model file
     * @param decoderStartToken the token the decoder input starts with
     * @param endToken          the token that ends the output
     * @return the created {@link OnnxSequenceModel} instance, which the caller closes
     * @throws ModelException if a file cannot be loaded or does not have the expected inputs and outputs
     */
    public static @NotNull OnnxSequenceModel open(@NotNull Path encoderFile, @NotNull Path decoderFile, int decoderStartToken,
                                                  int endToken) throws ModelException {
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        OrtSession encoder = null;
        OrtSession decoder = null;

        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(INTRA_OP_THREADS);
            options.setInterOpNumThreads(INTER_OP_THREADS);
            options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

            encoder = environment.createSession(encoderFile.toString(), options);
            decoder = environment.createSession(decoderFile.toString(), options);

            return new OnnxSequenceModel(environment, encoder, decoder, decoderStartToken, endToken);
        } catch (OrtException exception) {
            closeSessions(decoder, encoder);

            throw new ModelException(Exceptions.format("Cannot load the models {0} and {1}: {2}", encoderFile, decoderFile, exception.getMessage()),
                                     exception);
        } catch (ModelException exception) {
            closeSessions(decoder, encoder);

            throw exception;
        }
    }

    private static void closeSessions(@Nullable OrtSession decoder, @Nullable OrtSession encoder) {
        IgnoreException.execute(
                () -> {
                    if (decoder != null) {
                        decoder.close();
                    }
                },
                () -> {
                    if (encoder != null) {
                        encoder.close();
                    }
                }
        );
    }

    private static @NotNull OnnxTensor createIdTensor(@NotNull OrtEnvironment environment, long @NotNull [] values) throws OrtException {
        return OnnxTensor.createTensor(environment, LongBuffer.wrap(values), new long[]{BATCH_SIZE, values.length});
    }


    private final @NotNull OrtEnvironment environment;
    private final @NotNull OrtSession encoder;
    private final @NotNull OrtSession decoder;
    private final int decoderStartToken;
    private final int endToken;

    /**
     * The past value inputs of the decoder, with the present outputs that feed them at the next step.
     */
    private final @NotNull String @NotNull [] pastNames;
    private final @NotNull String @NotNull [] presentNames;
    private final boolean @NotNull [] encoderCaches;
    private final long @NotNull [] @NotNull [] emptyCacheShapes;
    private final @NotNull Set<@NotNull String> requestedOutputs;

    private final int vocabularySize;
    private final @NotNull FloatBuffer logits;
    private final @NotNull OnnxTensor logitsTensor;

    private OnnxSequenceModel(@NotNull OrtEnvironment environment, @NotNull OrtSession encoder, @NotNull OrtSession decoder, int decoderStartToken,
                              int endToken) throws OrtException, ModelException {
        Map<String, NodeInfo> inputs = decoder.getInputInfo();
        Map<String, NodeInfo> outputs = decoder.getOutputInfo();
        List<String> pastNames = new ArrayList<>();

        for (String name : inputs.keySet()) {
            if (name.startsWith(PAST_PREFIX)) {
                pastNames.add(name);
            }
        }

        Collections.sort(pastNames);

        if (!encoder.getInputInfo().keySet().containsAll(List.of(INPUT_IDS, ATTENTION_MASK)) ||
            !encoder.getOutputInfo().containsKey(LAST_HIDDEN_STATE) ||
            !inputs.keySet().containsAll(List.of(INPUT_IDS, ENCODER_ATTENTION_MASK, ENCODER_HIDDEN_STATES, USE_CACHE_BRANCH)) ||
            pastNames.isEmpty() || !(outputs.get(LOGITS) instanceof NodeInfo logitsInfo) ||
            !(logitsInfo.getInfo() instanceof TensorInfo logitsTensorInfo) || logitsTensorInfo.getShape().length != LOGITS_RANK) {
            throw new ModelException("The ONNX files are not an encoder and a merged decoder with past values");
        }

        this.environment       = environment;
        this.encoder           = encoder;
        this.decoder           = decoder;
        this.decoderStartToken = decoderStartToken;
        this.endToken          = endToken;

        this.pastNames        = pastNames.toArray(new String[0]);
        this.presentNames     = new String[this.pastNames.length];
        this.encoderCaches    = new boolean[this.pastNames.length];
        this.emptyCacheShapes = new long[this.pastNames.length][];

        for (int index = 0; index < this.pastNames.length; index++) {
            String pastName = this.pastNames[index];

            this.presentNames[index]  = PRESENT_PREFIX + pastName.substring(PAST_PREFIX.length());
            this.encoderCaches[index] = pastName.contains(ENCODER_CACHE_PART);

            if (!outputs.containsKey(this.presentNames[index]) || !(inputs.get(pastName).getInfo() instanceof TensorInfo cacheInfo) ||
                cacheInfo.getShape().length != CACHE_RANK) {
                throw new ModelException(Exceptions.format("The decoder has no present output for {0}", pastName));
            }

            long[] shape = cacheInfo.getShape();

            this.emptyCacheShapes[index] = new long[]{BATCH_SIZE, shape[HEAD_DIMENSION], 0, shape[HEAD_SIZE_DIMENSION]};
        }

        this.requestedOutputs = Set.of(this.presentNames);

        this.vocabularySize = (int) logitsTensorInfo.getShape()[VOCABULARY_DIMENSION];
        this.logits         = ByteBuffer.allocateDirect(this.vocabularySize * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.logitsTensor   = OnnxTensor.createTensor(environment, this.logits, new long[]{BATCH_SIZE, 1, this.vocabularySize});
    }

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
    @Override
    public int @NotNull [] generate(int @NotNull [] input, int forcedFirstToken, int maximumLength) throws ModelException {
        long[] inputIds = new long[input.length];
        long[] mask = new long[input.length];

        for (int index = 0; index < input.length; index++) {
            inputIds[index] = input[index];
            mask[index]     = 1;
        }

        try (OnnxTensor inputTensor = createIdTensor(this.environment, inputIds);
             OnnxTensor maskTensor = createIdTensor(this.environment, mask);
             OrtSession.Result encoderResult = this.encoder.run(Map.of(INPUT_IDS, inputTensor, ATTENTION_MASK, maskTensor))) {
            OnnxValue hiddenStates = encoderResult.get(LAST_HIDDEN_STATE).orElseThrow();

            if (!(hiddenStates instanceof OnnxTensor hiddenTensor)) {
                throw new ModelException("The encoder output is not a tensor");
            }

            return this.runDecoder(maskTensor, hiddenTensor, forcedFirstToken, maximumLength);
        } catch (OrtException exception) {
            throw new ModelException(Exceptions.format("The inference failed: {0}", exception.getMessage()), exception);
        }
    }

    private int @NotNull [] runDecoder(@NotNull OnnxTensor maskTensor, @NotNull OnnxTensor hiddenTensor, int forcedFirstToken,
                                       int maximumLength) throws OrtException, ModelException {
        int[] output = new int[maximumLength];
        int outputLength = 0;
        long[] token = {this.decoderStartToken};
        List<OnnxTensor> emptyCaches = new ArrayList<>(this.pastNames.length);
        OrtSession.Result first = null;
        OrtSession.Result previous = null;

        try (OnnxTensor withoutCache = OnnxTensor.createTensor(this.environment, new boolean[]{false});
             OnnxTensor withCache = OnnxTensor.createTensor(this.environment, new boolean[]{true})) {
            for (long[] shape : this.emptyCacheShapes) {
                emptyCaches.add(OnnxTensor.createTensor(this.environment, FloatBuffer.allocate(0), shape));
            }

            Map<String, OnnxTensorLike> inputs = new HashMap<>();

            inputs.put(ENCODER_ATTENTION_MASK, maskTensor);
            inputs.put(ENCODER_HIDDEN_STATES, hiddenTensor);

            while (outputLength < maximumLength) {
                try (OnnxTensor tokenTensor = createIdTensor(this.environment, token)) {
                    inputs.put(INPUT_IDS, tokenTensor);
                    inputs.put(USE_CACHE_BRANCH, previous == null ? withoutCache : withCache);

                    for (int index = 0; index < this.pastNames.length; index++) {
                        OrtSession.Result source = this.encoderCaches[index] ? first : previous;

                        inputs.put(this.pastNames[index], source == null ? emptyCaches.get(index) : this.requireTensor(source, this.presentNames[index]));
                    }

                    OrtSession.Result result = this.decoder.run(inputs, this.requestedOutputs, Map.of(LOGITS, this.logitsTensor));

                    if (previous != null && previous != first) {
                        previous.close();
                    }

                    if (first == null) {
                        first = result;
                    }

                    previous = result;
                }

                int next = outputLength == 0 ? forcedFirstToken : this.findBestToken();

                if (next == this.endToken) {
                    break;
                }

                output[outputLength++] = next;
                token[0]               = next;
            }
        } finally {
            OrtSession.Result last = previous != first ? previous : null;
            OrtSession.Result initial = first;

            IgnoreException.execute(
                    () -> {
                        if (last != null) {
                            last.close();
                        }
                    },
                    () -> {
                        if (initial != null) {
                            initial.close();
                        }
                    },
                    () -> emptyCaches.forEach(OnnxTensor::close)
            );
        }

        return Arrays.copyOf(output, outputLength);
    }

    private @NotNull OnnxTensor requireTensor(@NotNull OrtSession.Result result, @NotNull String name) throws ModelException {
        if (!(result.get(name).orElse(null) instanceof OnnxTensor tensor)) {
            throw new ModelException(Exceptions.format("The decoder gave no tensor {0}", name));
        }

        return tensor;
    }

    private int findBestToken() {
        int best = 0;
        float bestScore = this.logits.get(0);

        for (int index = 1; index < this.vocabularySize; index++) {
            float score = this.logits.get(index);

            if (score > bestScore) {
                best      = index;
                bestScore = score;
            }
        }

        return best;
    }

    /**
     * Releases the native resources.
     */
    @Override
    public void close() {
        IgnoreException.execute(
                this.logitsTensor::close,
                this.decoder::close,
                this.encoder::close
        );
    }
}
