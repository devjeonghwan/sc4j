package org.fogofwarlabs.sc4j.translate.service.link;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.translate.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Encodes the requests and the answers of the {@value TranslationLinkService#SERVICE_NAME} service of the launcher link. Launcher and runtime
 * come from one install, so the form carries no version:
 * {@snippet lang = "text":
 * request: text, target language tag, priority name, int span count, (int start, int end) per span, byte context form, then by form:
 *          nothing, the context id, or the full context: text kind, description, int entry count, (source term, target language tag,
 *          target term) per entry
 * answer:  byte answer kind, then for a translation: original text, translated text, boolean has source, [source language tag],
 *          status name; nothing for an unknown context
 *}
 * A text is an {@code int} byte length followed by its UTF-8 bytes; numbers are big-endian. A full context is sent the first time a
 * connection uses it, later requests carry its id; the launcher answers a context id it does not know with the unknown context answer.
 */
final class TranslationCodecUtils {
    private static final byte NO_CONTEXT = 0;
    private static final byte CONTEXT_ID = 1;
    private static final byte FULL_CONTEXT = 2;
    private static final byte TRANSLATION_ANSWER = 0;
    private static final byte UNKNOWN_CONTEXT_ANSWER = 1;

    /**
     * Encodes a request.
     *
     * @param request     the request
     * @param fullContext whether the request's context is sent whole, else only its id; ignored for a request without context
     * @return the payload
     */
    static byte @NotNull [] encodeRequest(@NotNull TranslationRequest request, boolean fullContext) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TranslationContext context = request.getContext();

        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writeText(output, request.getText());
            writeText(output, request.getTarget().toLanguageTag());
            writeText(output, request.getPriority().name());
            output.writeInt(request.getProtectedSpans().size());

            for (ProtectedSpan span : request.getProtectedSpans()) {
                output.writeInt(span.getStart());
                output.writeInt(span.getEnd());
            }

            if (context == null) {
                output.writeByte(NO_CONTEXT);
            } else if (fullContext) {
                output.writeByte(FULL_CONTEXT);
                writeText(output, context.getTextKind());
                writeText(output, context.getDescription());
                output.writeInt(context.getGlossary().size());

                for (GlossaryEntry entry : context.getGlossary()) {
                    writeText(output, entry.getSourceTerm());
                    writeText(output, entry.getTarget().toLanguageTag());
                    writeText(output, entry.getTargetTerm());
                }
            } else {
                output.writeByte(CONTEXT_ID);
                writeText(output, context.getId());
            }
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }

        return bytes.toByteArray();
    }

    /**
     * Decodes a request. A full context is added to the known contexts under its id; a context id is looked up there.
     *
     * @param payload  the payload
     * @param contexts the contexts known on the connection, by id
     * @return the request, or {@code null} when it names a context id that is not known
     * @throws IOException if the payload is not an encoded request
     */
    static @Nullable TranslationRequest decodeRequest(byte @NotNull [] payload, @NotNull Map<@NotNull String, @NotNull TranslationContext> contexts) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            String text = readText(input);
            Locale target = Locale.forLanguageTag(readText(input));
            TranslationPriority priority = TranslationPriority.valueOf(readText(input));
            int spanCount = input.readInt();
            TranslationRequest.Builder builder = new TranslationRequest.Builder(text, target).setPriority(priority);

            if (spanCount < 0 || spanCount > text.length()) {
                throw Exceptions.newIOException("Invalid protected span count: {0}", spanCount);
            }

            for (int index = 0; index < spanCount; index++) {
                builder.addProtectedSpan(input.readInt(), input.readInt());
            }

            byte contextForm = input.readByte();
            TranslationContext received = null;
            TranslationContext context;

            switch (contextForm) {
                case NO_CONTEXT -> context = null;
                case CONTEXT_ID -> context = contexts.get(readText(input));
                case FULL_CONTEXT -> {
                    received = readContext(input);
                    context  = received;
                }
                default -> throw Exceptions.newIOException("Invalid context form: {0}", contextForm);
            }

            checkEnd(input);

            if (contextForm == CONTEXT_ID && context == null) {
                return null;
            }

            TranslationRequest request = builder.setContext(context).build();

            if (received != null) {
                contexts.put(received.getId(), received);
            }

            return request;
        } catch (IllegalArgumentException exception) {
            throw Exceptions.newIOException(exception, "Invalid translation request: {0}", exception.getMessage());
        }
    }

    /**
     * Encodes the answer of a translation.
     *
     * @param translation the translation
     * @return the payload
     */
    static byte @NotNull [] encodeTranslation(@NotNull Translation translation) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Locale source = translation.getSource();

        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeByte(TRANSLATION_ANSWER);
            writeText(output, translation.getOriginalText());
            writeText(output, translation.getTranslatedText());
            output.writeBoolean(source != null);

            if (source != null) {
                writeText(output, source.toLanguageTag());
            }

            writeText(output, translation.getStatus().name());
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }

        return bytes.toByteArray();
    }

    /**
     * Encodes the answer to a request whose context id is not known.
     *
     * @return the payload
     */
    static byte @NotNull [] encodeUnknownContext() {
        return new byte[]{UNKNOWN_CONTEXT_ANSWER};
    }

    /**
     * Decodes an answer.
     *
     * @param payload the payload
     * @return the translation, or {@code null} when the launcher did not know the request's context
     * @throws IOException if the payload is not an encoded answer
     */
    static @Nullable Translation decodeAnswer(byte @NotNull [] payload) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            byte kind = input.readByte();

            if (kind == UNKNOWN_CONTEXT_ANSWER) {
                checkEnd(input);

                return null;
            }

            if (kind != TRANSLATION_ANSWER) {
                throw Exceptions.newIOException("Invalid answer kind: {0}", kind);
            }

            String originalText = readText(input);
            String translatedText = readText(input);
            Locale source = input.readBoolean() ? Locale.forLanguageTag(readText(input)) : null;
            TranslationStatus status = TranslationStatus.valueOf(readText(input));

            checkEnd(input);

            return new Translation(originalText, translatedText, source, status);
        } catch (IllegalArgumentException exception) {
            throw Exceptions.newIOException(exception, "Invalid translation: {0}", exception.getMessage());
        }
    }

    private static @NotNull TranslationContext readContext(@NotNull DataInputStream input) throws IOException {
        TranslationContext.Builder builder = new TranslationContext.Builder().setTextKind(readText(input)).setDescription(readText(input));
        int entryCount = input.readInt();

        if (entryCount < 0 || entryCount > input.available()) {
            throw Exceptions.newIOException("Invalid glossary entry count: {0}", entryCount);
        }

        for (int index = 0; index < entryCount; index++) {
            String sourceTerm = readText(input);
            Locale target = Locale.forLanguageTag(readText(input));

            builder.addGlossaryEntry(sourceTerm, target, readText(input));
        }

        return builder.build();
    }

    private static void writeText(@NotNull DataOutputStream output, @NotNull String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static @NotNull String readText(@NotNull DataInputStream input) throws IOException {
        int length = input.readInt();

        if (length < 0 || length > input.available()) {
            throw Exceptions.newIOException("Invalid text length: {0}", length);
        }

        return new String(input.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static void checkEnd(@NotNull DataInputStream input) throws IOException {
        if (input.available() > 0) {
            throw Exceptions.newIOException("{0} bytes after the end", input.available());
        }
    }


    private TranslationCodecUtils() {
    }
}
