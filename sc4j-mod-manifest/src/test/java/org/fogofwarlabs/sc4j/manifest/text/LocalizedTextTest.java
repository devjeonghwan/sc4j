package org.fogofwarlabs.sc4j.manifest.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tests the language fallback of localized manifest texts: exact language, base language, English, first listed.
 */
final class LocalizedTextTest {
    @Test
    void get_should_fall_back_to_base_language_then_english_then_first_text() {
        Map<String, String> texts = new LinkedHashMap<>();

        texts.put("ko", "폴더");
        texts.put("en", "Folder");
        texts.put("pt-BR", "Pasta");

        LocalizedText text = LocalizedText.of(texts);

        Assertions.assertEquals("폴더", text.get("ko"));
        Assertions.assertEquals("폴더", text.get("ko-KR"));
        Assertions.assertEquals("Pasta", text.get("pt-br"));
        Assertions.assertEquals("Folder", text.get("de"));
        Assertions.assertEquals("Carpeta", LocalizedText.of(Map.of("es", "Carpeta")).get("de"));
    }

    @Test
    void of_should_reject_no_text_and_bad_language_names() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> LocalizedText.of(Map.of()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> LocalizedText.of(Map.of("Korean", "폴더")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> LocalizedText.of(Map.of("EN", "Folder")));
    }
}
