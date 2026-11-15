package org.fogofwarlabs.sc4j.base.json;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses JSON text such as mod manifests, data pack definitions, maps, settings files and the JSON data files a mod reads.
 * <p>
 * Objects become insertion-ordered {@link Map} instances, arrays {@link List} instances, numbers {@link Long} or
 * {@link Double} values, and {@code null} stays {@code null}. A leading UTF-8 byte order mark is skipped. The parser is a
 * data boundary for these files; it is not a general JSON library.
 */
public final class JsonReader {
    private static final @NotNull String BYTE_ORDER_MARK = "\uFEFF";
    private static final int UNICODE_ESCAPE_DIGITS = 4;
    private static final int HEX_RADIX = 16;
    /**
     * The characters a number token may hold; {@link Long#parseLong} and {@link Double#parseDouble} check the token.
     */
    private static final @NotNull String NUMBER_CHARACTERS = "+-0123456789.eE";

    /**
     * Parses UTF-8 JSON bytes.
     *
     * @param contents the file contents
     * @return the parsed value
     * @throws IOException if the contents are not valid JSON
     */
    public static @Nullable Object parse(byte @NotNull [] contents) throws IOException {
        String text = new String(contents, StandardCharsets.UTF_8);
        JsonReader reader = new JsonReader(text.startsWith(BYTE_ORDER_MARK) ? text.substring(BYTE_ORDER_MARK.length()) : text);
        Object value = reader.readValue();

        reader.skipWhitespace();

        if (reader.position != reader.text.length()) {
            throw reader.createParseException("Unexpected trailing content");
        }

        return value;
    }


    private final @NotNull String text;

    private int position;

    private JsonReader(@NotNull String text) {
        this.text = text;

        this.position = 0;
    }

    private @Nullable Object readValue() throws IOException {
        this.skipWhitespace();

        if (this.position >= this.text.length()) {
            throw this.createParseException("Unexpected end of input");
        }

        char character = this.text.charAt(this.position);

        return switch (character) {
            case '{' -> this.readObject();
            case '[' -> this.readArray();
            case '"' -> this.readString();
            case 't' -> this.readLiteral("true", Boolean.TRUE);
            case 'f' -> this.readLiteral("false", Boolean.FALSE);
            case 'n' -> this.readLiteral("null", null);
            default -> this.readNumber();
        };
    }

    private @NotNull Map<String, Object> readObject() throws IOException {
        Map<String, Object> object = new LinkedHashMap<>();

        this.position++;
        this.skipWhitespace();

        if (this.peekCharacter() == '}') {
            this.position++;

            return object;
        }

        while (true) {
            this.skipWhitespace();

            if (this.peekCharacter() != '"') {
                throw this.createParseException("Expected an object key");
            }

            String key = this.readString();

            this.skipWhitespace();

            if (this.peekCharacter() != ':') {
                throw this.createParseException("Expected ':'");
            }

            this.position++;
            object.put(key, this.readValue());
            this.skipWhitespace();

            char separator = this.peekCharacter();

            this.position++;

            if (separator == '}') {
                return object;
            }

            if (separator != ',') {
                throw this.createParseException("Expected ',' or '}'");
            }
        }
    }

    private @NotNull List<Object> readArray() throws IOException {
        List<Object> array = new ArrayList<>();

        this.position++;
        this.skipWhitespace();

        if (this.peekCharacter() == ']') {
            this.position++;

            return array;
        }

        while (true) {
            array.add(this.readValue());
            this.skipWhitespace();

            char separator = this.peekCharacter();

            this.position++;

            if (separator == ']') {
                return array;
            }

            if (separator != ',') {
                throw this.createParseException("Expected ',' or ']'");
            }
        }
    }

    private @NotNull String readString() throws IOException {
        StringBuilder builder = new StringBuilder();

        this.position++;

        while (true) {
            if (this.position >= this.text.length()) {
                throw this.createParseException("Unterminated string");
            }

            char character = this.text.charAt(this.position++);

            if (character == '"') {
                return builder.toString();
            }

            if (character != '\\') {
                builder.append(character);

                continue;
            }

            if (this.position >= this.text.length()) {
                throw this.createParseException("Unterminated escape");
            }

            char escape = this.text.charAt(this.position++);

            switch (escape) {
                case '"', '\\', '/' -> builder.append(escape);
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (this.position + UNICODE_ESCAPE_DIGITS > this.text.length()) {
                        throw this.createParseException("Truncated unicode escape");
                    }

                    try {
                        builder.append((char) Integer.parseInt(this.text.substring(this.position, this.position + UNICODE_ESCAPE_DIGITS), HEX_RADIX));
                    } catch (NumberFormatException exception) {
                        throw this.createParseException("Invalid unicode escape");
                    }

                    this.position += UNICODE_ESCAPE_DIGITS;
                }
                default -> throw this.createParseException("Invalid escape");
            }
        }
    }

    private @Nullable Object readLiteral(@NotNull String literal, @Nullable Object value) throws IOException {
        if (!this.text.startsWith(literal, this.position)) {
            throw this.createParseException("Invalid literal");
        }

        this.position += literal.length();

        return value;
    }

    private @NotNull Object readNumber() throws IOException {
        int start = this.position;

        while (this.position < this.text.length() && NUMBER_CHARACTERS.indexOf(this.text.charAt(this.position)) >= 0) {
            this.position++;
        }

        String number = this.text.substring(start, this.position);

        if (number.isEmpty()) {
            throw this.createParseException("Unexpected character");
        }

        try {
            if (number.indexOf('.') < 0 && number.indexOf('e') < 0 && number.indexOf('E') < 0) {
                return Long.parseLong(number);
            }

            return Double.parseDouble(number);
        } catch (NumberFormatException exception) {
            throw this.createParseException("Invalid number");
        }
    }

    private char peekCharacter() throws IOException {
        if (this.position >= this.text.length()) {
            throw this.createParseException("Unexpected end of input");
        }

        return this.text.charAt(this.position);
    }

    private void skipWhitespace() {
        while (this.position < this.text.length() && Character.isWhitespace(this.text.charAt(this.position))) {
            this.position++;
        }
    }

    private @NotNull IOException createParseException(@NotNull String message) {
        return Exceptions.newIOException("{0} at offset {1}.", message, this.position);
    }
}
