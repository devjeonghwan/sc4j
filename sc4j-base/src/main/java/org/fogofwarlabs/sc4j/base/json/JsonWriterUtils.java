package org.fogofwarlabs.sc4j.base.json;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Writes the values {@link JsonReader} produces back as JSON text in one stable layout: objects keep their key order,
 * two spaces indent every level, an object or array that holds only scalars is written on one line, strings escape
 * the quote, the backslash and the control characters, and the text ends with a line feed.
 */
public final class JsonWriterUtils {
    private static final @NotNull String INDENT = "  ";
    private static final char LINE_FEED = '\n';
    private static final char LAST_CONTROL_CHARACTER = 0x1f;
    private static final int HEX_DIGITS = 4;
    private static final int HEX_RADIX = 16;

    /**
     * Writes a value.
     *
     * @param value a {@link Map} with string keys, a {@link List}, a {@link String}, a {@link Number}, a {@link Boolean} or {@code null}
     * @return the JSON text, ending with a line feed
     * @throws IllegalArgumentException if a value has another type
     */
    public static @NotNull String write(@Nullable Object value) {
        StringBuilder text = new StringBuilder();

        append(text, value, 0);

        return text.append(LINE_FEED).toString();
    }

    private static void append(@NotNull StringBuilder text, @Nullable Object value, int depth) {
        if (value instanceof Map<?, ?> object) {
            appendObject(text, object, depth);
        } else if (value instanceof List<?> array) {
            appendArray(text, array, depth);
        } else {
            appendScalar(text, value);
        }
    }

    private static void appendObject(@NotNull StringBuilder text, @NotNull Map<?, ?> object, int depth) {
        if (object.isEmpty()) {
            text.append("{}");

            return;
        }

        boolean flat = object.values().stream().noneMatch(JsonWriterUtils::isContainer);
        boolean first = true;

        text.append('{');

        for (Map.Entry<?, ?> entry : object.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw Exceptions.newIllegalArgumentException("JSON object keys must be strings: {0}", entry.getKey());
            }

            text.append(first ? "" : ",");
            first = false;

            if (flat) {
                text.append(text.charAt(text.length() - 1) == '{' ? "" : " ");
            } else {
                text.append(LINE_FEED);
                text.append(INDENT.repeat(depth + 1));
            }

            appendString(text, key);
            text.append(": ");
            append(text, entry.getValue(), depth + 1);
        }

        if (!flat) {
            text.append(LINE_FEED);
            text.append(INDENT.repeat(depth));
        }

        text.append('}');
    }

    private static void appendArray(@NotNull StringBuilder text, @NotNull List<?> array, int depth) {
        if (array.isEmpty()) {
            text.append("[]");

            return;
        }

        boolean flat = array.stream().noneMatch(JsonWriterUtils::isContainer);

        text.append('[');

        for (int index = 0; index < array.size(); index++) {
            text.append(index == 0 ? "" : flat ? ", " : ",");

            if (!flat) {
                text.append(LINE_FEED);
                text.append(INDENT.repeat(depth + 1));
            }

            append(text, array.get(index), depth + 1);
        }

        if (!flat) {
            text.append(LINE_FEED);
            text.append(INDENT.repeat(depth));
        }

        text.append(']');
    }

    private static void appendScalar(@NotNull StringBuilder text, @Nullable Object value) {
        switch (value) {
            case null -> text.append("null");
            case String string -> appendString(text, string);
            case Boolean flag -> text.append(flag);
            case Long number -> text.append(number);
            case Integer number -> text.append(number);
            case Double number -> {
                if (number.isNaN() || number.isInfinite()) {
                    throw Exceptions.newIllegalArgumentException("JSON numbers must be finite: {0}", number);
                }

                text.append(number);
            }
            default -> throw Exceptions.newIllegalArgumentException("Not a JSON value: {0}", value.getClass().getName());
        }
    }

    private static void appendString(@NotNull StringBuilder text, @NotNull String value) {
        text.append('"');

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            switch (character) {
                case '"' -> text.append("\\\"");
                case '\\' -> text.append("\\\\");
                case '\n' -> text.append("\\n");
                case '\r' -> text.append("\\r");
                case '\t' -> text.append("\\t");
                default -> {
                    if (character <= LAST_CONTROL_CHARACTER) {
                        String hex = Integer.toString(character, HEX_RADIX);

                        text.append("\\u").append("0".repeat(HEX_DIGITS - hex.length())).append(hex);
                    } else {
                        text.append(character);
                    }
                }
            }
        }

        text.append('"');
    }

    private static boolean isContainer(@Nullable Object value) {
        return value instanceof Map<?, ?> map && !map.isEmpty() || value instanceof List<?> list && !list.isEmpty();
    }


    private JsonWriterUtils() {
    }
}
