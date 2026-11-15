package org.fogofwarlabs.sc4j.base.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies that {@link JsonWriterUtils} writes what {@link JsonReader} reads back unchanged, in one stable layout.
 */
final class JsonWriterUtilsTest {
    @Test
    void write_should_produce_text_the_reader_reads_back_unchanged() throws IOException {
        Map<String, Object> inner = new LinkedHashMap<>();

        inner.put("type", 10L);
        inner.put("name", "quote \" slash \\ tab \t line \n \u0001 한글");
        inner.put("ratio", 1.5);
        inner.put("on", true);
        inner.put("none", null);

        Map<String, Object> root = new LinkedHashMap<>();

        root.put("createGame", inner);
        root.put("list", List.of(1L, 2L, List.of("a")));
        root.put("empty", Map.of());

        String text = JsonWriterUtils.write(root);

        Assertions.assertEquals(root, JsonReader.parse(text.getBytes(StandardCharsets.UTF_8)));
        Assertions.assertEquals(text, JsonWriterUtils.write(JsonReader.parse(text.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void write_should_produce_stable_layout() {
        Map<String, Object> group = new LinkedHashMap<>();

        group.put("type", 2L);
        group.put("name", "x");

        Map<String, Object> root = new LinkedHashMap<>();

        root.put("createGame", group);
        root.put("flags", List.of(true, false));

        Assertions.assertEquals("""
                                {
                                  "createGame": {"type": 2, "name": "x"},
                                  "flags": [true, false]
                                }
                                """, JsonWriterUtils.write(root));
    }

    @Test
    void write_should_refuse_unsupported_values() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonWriterUtils.write(Map.of("x", new Object())));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonWriterUtils.write(Double.NaN));
    }
}
