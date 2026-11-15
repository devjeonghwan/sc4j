package org.fogofwarlabs.sc4j.base.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Verifies that {@link JsonReader} reads JSON values and rejects malformed input.
 */
final class JsonReaderTest {
    @Test
    void parse_should_read_json_values_and_reject_malformed_input() throws IOException {
        Object value = JsonReader.parse("{\"a\": [1, -2.5, true, null, \"x\\u0041\\n\"], \"b\": {}}".getBytes(StandardCharsets.UTF_8));
        Map<?, ?> object = Assertions.assertInstanceOf(Map.class, value);
        List<?> array = Assertions.assertInstanceOf(List.class, object.get("a"));

        Assertions.assertEquals(1L, array.get(0));
        Assertions.assertEquals(-2.5, array.get(1));
        Assertions.assertEquals(Boolean.TRUE, array.get(2));
        Assertions.assertNull(array.get(3));
        Assertions.assertEquals("xA\n", array.get(4));
        Assertions.assertThrows(IOException.class, () -> JsonReader.parse("[1,".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertThrows(IOException.class, () -> JsonReader.parse("{} x".getBytes(StandardCharsets.UTF_8)));
    }
}
