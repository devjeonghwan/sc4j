package org.fogofwarlabs.sc4j.translate.service.link;

import org.fogofwarlabs.sc4j.translate.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tests the payloads of the translation service on the launcher link: requests and translations come back as they were encoded, a context
 * is sent whole once and then by its id, an unknown context id is reported, and a payload of another form is refused.
 */
final class TranslationCodecUtilsTest {
    private static final @NotNull TranslationContext CONTEXT = new TranslationContext.Builder().setTextKind("chat message")
                                                                                              .setDescription("Short messages.\nCasual tone.")
                                                                                              .addGlossaryEntry("tower", Locale.KOREAN, "타워")
                                                                                              .addGlossaryEntry("wall", Locale.GERMAN, "Mauer")
                                                                                              .build();

    @Test
    void decode_request_should_return_the_encoded_request() throws IOException {
        TranslationRequest request = new TranslationRequest.Builder("^1안녕 ^2하세요", Locale.forLanguageTag("zh-Hans-CN")).addProtectedSpan(0, 2)
                                                                                                                      .addProtectedSpan(5, 7)
                                                                                                                      .setPriority(TranslationPriority.BATCH)
                                                                                                                      .build();

        TranslationRequest decoded = TranslationCodecUtils.decodeRequest(TranslationCodecUtils.encodeRequest(request, true), new HashMap<>());

        Assertions.assertNotNull(decoded);
        Assertions.assertEquals(request.getText(), decoded.getText());
        Assertions.assertEquals(request.getTarget(), decoded.getTarget());
        Assertions.assertEquals(TranslationPriority.BATCH, decoded.getPriority());
        Assertions.assertNull(decoded.getContext());
        Assertions.assertEquals(2, decoded.getProtectedSpans().size());

        for (int index = 0; index < request.getProtectedSpans().size(); index++) {
            ProtectedSpan expected = request.getProtectedSpans().get(index);
            ProtectedSpan actual = decoded.getProtectedSpans().get(index);

            Assertions.assertEquals(expected.getStart(), actual.getStart());
            Assertions.assertEquals(expected.getEnd(), actual.getEnd());
        }
    }

    @Test
    void decode_request_should_keep_a_full_context_and_resolve_its_id_later() throws IOException {
        TranslationRequest request = new TranslationRequest.Builder("tower", Locale.KOREAN).setContext(CONTEXT).build();
        Map<String, TranslationContext> contexts = new HashMap<>();
        byte[] full = TranslationCodecUtils.encodeRequest(request, true);
        byte[] byId = TranslationCodecUtils.encodeRequest(request, false);

        Assertions.assertTrue(byId.length < full.length);
        Assertions.assertNull(TranslationCodecUtils.decodeRequest(byId, contexts), "an id before its context is unknown");

        TranslationRequest first = TranslationCodecUtils.decodeRequest(full, contexts);
        TranslationRequest second = TranslationCodecUtils.decodeRequest(byId, contexts);

        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        Assertions.assertNotNull(first.getContext());
        Assertions.assertEquals(CONTEXT, first.getContext());
        Assertions.assertEquals(CONTEXT.getGlossary(), first.getContext().getGlossary());
        Assertions.assertEquals(CONTEXT.getDescription(), first.getContext().getDescription());
        Assertions.assertSame(first.getContext(), second.getContext());
        Assertions.assertEquals(Map.of(CONTEXT.getId(), first.getContext()), contexts);
    }

    @Test
    void decode_answer_should_return_the_encoded_translation_or_an_unknown_context() throws IOException {
        Translation translated = new Translation("안녕하세요", "Hello", Locale.KOREAN, TranslationStatus.TRANSLATED);
        Translation skipped = Translation.untranslated(":)", null, TranslationStatus.SKIPPED);

        for (Translation translation : new Translation[]{translated, skipped}) {
            Translation decoded = TranslationCodecUtils.decodeAnswer(TranslationCodecUtils.encodeTranslation(translation));

            Assertions.assertNotNull(decoded);
            Assertions.assertEquals(translation.getOriginalText(), decoded.getOriginalText());
            Assertions.assertEquals(translation.getTranslatedText(), decoded.getTranslatedText());
            Assertions.assertEquals(translation.getSource(), decoded.getSource());
            Assertions.assertEquals(translation.getStatus(), decoded.getStatus());
        }

        Assertions.assertNull(TranslationCodecUtils.decodeAnswer(TranslationCodecUtils.encodeUnknownContext()));
    }

    @Test
    void decode_should_refuse_payloads_of_another_form() {
        byte[] request = TranslationCodecUtils.encodeRequest(new TranslationRequest.Builder("text", Locale.ENGLISH).setContext(CONTEXT).build(), true);
        byte[] translation = TranslationCodecUtils.encodeTranslation(Translation.untranslated("text", null, TranslationStatus.UNAVAILABLE));
        Map<String, TranslationContext> contexts = new HashMap<>();

        Assertions.assertThrows(IOException.class, () -> TranslationCodecUtils.decodeRequest("garbage".getBytes(StandardCharsets.UTF_8), contexts));
        Assertions.assertThrows(IOException.class, () -> TranslationCodecUtils.decodeRequest(Arrays.copyOf(request, request.length - 1), contexts));
        Assertions.assertThrows(IOException.class, () -> TranslationCodecUtils.decodeRequest(Arrays.copyOf(request, request.length + 1), contexts));
        Assertions.assertThrows(IOException.class, () -> TranslationCodecUtils.decodeAnswer(request));
        Assertions.assertThrows(IOException.class, () -> TranslationCodecUtils.decodeAnswer(Arrays.copyOf(translation, translation.length + 1)));
        Assertions.assertThrows(IOException.class, () -> TranslationCodecUtils.decodeRequest(translation, contexts));
        Assertions.assertEquals(Map.of(), contexts, "a refused payload adds no context");
    }
}
