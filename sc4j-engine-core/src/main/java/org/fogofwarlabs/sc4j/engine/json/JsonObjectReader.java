package org.fogofwarlabs.sc4j.engine.json;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.json.JsonReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * Reads the typed fields of one JSON object of an engine data file: a data pack file or a map. Every failure is an {@link IOException} that
 * names the file and the field path, so a broken file tells where it is broken.
 */
public final class JsonObjectReader {
    /**
     * Parses a data file that holds one JSON object.
     *
     * @param contents the file contents
     * @param location the file location, for diagnostics
     * @return the object reader
     * @throws IOException if the contents are not JSON or not an object
     */
    public static @NotNull JsonObjectReader parse(byte @NotNull [] contents, @NotNull String location) throws IOException {
        Object value;

        try {
            value = JsonReader.parse(contents);
        } catch (IOException exception) {
            throw Exceptions.newIOException(exception, "{0}: invalid JSON: {1}", location, exception.getMessage());
        }

        return of(value, location);
    }

    /**
     * Parses a data file that holds one JSON array.
     *
     * @param contents the file contents
     * @param location the file location, for diagnostics
     * @return the array elements
     * @throws IOException if the contents are not JSON or not an array
     */
    public static @NotNull List<?> parseArray(byte @NotNull [] contents, @NotNull String location) throws IOException {
        Object value;

        try {
            value = JsonReader.parse(contents);
        } catch (IOException exception) {
            throw Exceptions.newIOException(exception, "{0}: invalid JSON: {1}", location, exception.getMessage());
        }

        if (!(value instanceof List<?> array)) {
            throw Exceptions.newIOException("{0}: expected a JSON array.", location);
        }

        return array;
    }

    /**
     * Creates a {@link JsonObjectReader} instance of a parsed value that must be an object.
     *
     * @param value    the parsed value
     * @param location the location of the value, for diagnostics
     * @return the created {@link JsonObjectReader} instance
     * @throws IOException if the value is not an object
     */
    public static @NotNull JsonObjectReader of(@Nullable Object value, @NotNull String location) throws IOException {
        if (!(value instanceof Map<?, ?> object)) {
            throw Exceptions.newIOException("{0}: expected a JSON object.", location);
        }

        return new JsonObjectReader(object, location);
    }

    /**
     * Converts a parsed number to an {@code int}.
     *
     * @param value    the parsed value
     * @param location the location of the value, for diagnostics
     * @return the value
     * @throws IOException if the value is not an integer of the {@code int} range
     */
    public static int toInt(@Nullable Object value, @NotNull String location) throws IOException {
        if (!(value instanceof Long number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw Exceptions.newIOException("{0}: expected an integer, found {1}.", location, value);
        }

        return number.intValue();
    }

    /**
     * Converts a parsed value to a string.
     *
     * @param value    the parsed value
     * @param location the location of the value, for diagnostics
     * @return the string
     * @throws IOException if the value is not a string
     */
    public static @NotNull String toText(@Nullable Object value, @NotNull String location) throws IOException {
        if (!(value instanceof String text)) {
            throw Exceptions.newIOException("{0}: expected a string, found {1}.", location, value);
        }

        return text;
    }


    private final @NotNull Map<?, ?> object;
    private final @NotNull String location;

    private JsonObjectReader(@NotNull Map<?, ?> object, @NotNull String location) {
        this.object   = object;
        this.location = location;
    }

    /**
     * @return the location of the object, for diagnostics
     */
    public @NotNull String getLocation() {
        return this.location;
    }

    /**
     * @return the field names in file order
     */
    public @NotNull Set<?> getNames() {
        return this.object.keySet();
    }

    /**
     * Returns the location of a field.
     *
     * @param name the field name
     * @return the location, for diagnostics
     */
    public @NotNull String getLocation(@NotNull String name) {
        return this.location + "." + name;
    }

    /**
     * Checks whether the object has a field that is not {@code null}.
     *
     * @param name the field name
     * @return {@code true} if the field is present, {@code false} otherwise
     */
    public boolean has(@NotNull String name) {
        return this.object.get(name) != null;
    }

    /**
     * Returns the raw value of a field.
     *
     * @param name the field name
     * @return the value, or {@code null} if the field is absent
     */
    public @Nullable Object get(@NotNull String name) {
        return this.object.get(name);
    }

    /**
     * Returns an integer field.
     *
     * @param name the field name
     * @return the value
     * @throws IOException if the field is absent or not an integer
     */
    public int getInt(@NotNull String name) throws IOException {
        return toInt(this.object.get(name), this.getLocation(name));
    }

    /**
     * Returns an optional integer field.
     *
     * @param name         the field name
     * @param defaultValue the value of an absent field
     * @return the value
     * @throws IOException if the field is present but not an integer
     */
    public int getInt(@NotNull String name, int defaultValue) throws IOException {
        return this.has(name) ? this.getInt(name) : defaultValue;
    }

    /**
     * Returns an optional boolean field.
     *
     * @param name         the field name
     * @param defaultValue the value of an absent field
     * @return the value
     * @throws IOException if the field is present but not a boolean
     */
    public boolean getBoolean(@NotNull String name, boolean defaultValue) throws IOException {
        Object value = this.object.get(name);

        if (value == null) {
            return defaultValue;
        }

        if (!(value instanceof Boolean flag)) {
            throw Exceptions.newIOException("{0}: expected true or false, found {1}.", this.getLocation(name), value);
        }

        return flag;
    }

    /**
     * Returns a string field.
     *
     * @param name the field name
     * @return the value
     * @throws IOException if the field is absent or not a string
     */
    public @NotNull String getText(@NotNull String name) throws IOException {
        return toText(this.object.get(name), this.getLocation(name));
    }

    /**
     * Returns an optional string field.
     *
     * @param name the field name
     * @return the value, or {@code null} if the field is absent
     * @throws IOException if the field is present but not a string
     */
    public @Nullable String findText(@NotNull String name) throws IOException {
        return this.has(name) ? this.getText(name) : null;
    }

    /**
     * Returns an object field.
     *
     * @param name the field name
     * @return the object reader
     * @throws IOException if the field is absent or not an object
     */
    public @NotNull JsonObjectReader getObject(@NotNull String name) throws IOException {
        return of(this.object.get(name), this.getLocation(name));
    }

    /**
     * Returns an optional array field.
     *
     * @param name the field name
     * @return the elements, empty if the field is absent
     * @throws IOException if the field is present but not an array
     */
    public @NotNull List<?> getArray(@NotNull String name) throws IOException {
        Object value = this.object.get(name);

        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> array)) {
            throw Exceptions.newIOException("{0}: expected a JSON array.", this.getLocation(name));
        }

        return array;
    }

    /**
     * Returns an optional array field of strings.
     *
     * @param name the field name
     * @return the strings in file order, empty if the field is absent
     * @throws IOException if the field is present but not an array of strings
     */
    public @NotNull List<@NotNull String> getTextList(@NotNull String name) throws IOException {
        List<?> array = this.getArray(name);
        List<String> texts = new ArrayList<>(array.size());

        for (int index = 0; index < array.size(); index++) {
            texts.add(toText(array.get(index), this.getLocation(name) + "[" + index + "]"));
        }

        return texts;
    }

    /**
     * Returns an optional object field whose values are strings, such as an id to path map.
     *
     * @param name the field name
     * @return the entries in file order, empty if the field is absent
     * @throws IOException if the field is present but not an object of strings
     */
    public @NotNull Map<@NotNull String, @NotNull String> getTextMap(@NotNull String name) throws IOException {
        if (!this.has(name)) {
            return Map.of();
        }

        JsonObjectReader entries = this.getObject(name);
        Map<String, String> texts = new LinkedHashMap<>();

        for (Object key : entries.getNames()) {
            String entryName = String.valueOf(key);

            texts.put(entryName, entries.getText(entryName));
        }

        return Collections.unmodifiableMap(texts);
    }
}
