package org.fogofwarlabs.sc4j.manifest.text;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Holds one text in several languages, such as a mod name or a setting label of the manifest.
 * <p>
 * Languages are lower case language codes ({@code en}, {@code ko}), optionally followed by a region or script part
 * ({@code pt-BR}). {@link #get(String)} falls back from the requested language to its base language, then to
 * {@value #FALLBACK_LANGUAGE}, then to the first language listed.
 */
public final class LocalizedText {
    /**
     * The language shown when a text has neither the requested language nor its base language.
     */
    public static final @NotNull String FALLBACK_LANGUAGE = "en";

    private static final @NotNull Pattern LANGUAGE_PATTERN = Pattern.compile("[a-z]{2,3}(-[A-Za-z0-9]{2,8})*");
    private static final char LANGUAGE_SEPARATOR = '-';

    /**
     * Checks whether a language name has the language code syntax.
     *
     * @param language the language name
     * @return {@code true} if the name is a lower case language code with optional parts, {@code false} otherwise
     */
    public static boolean isLanguage(@NotNull String language) {
        return LANGUAGE_PATTERN.matcher(language).matches();
    }

    /**
     * Creates a {@link LocalizedText} instance from texts by language.
     *
     * @param texts the texts by language, in the order the manifest lists them
     * @return the created {@link LocalizedText} instance
     * @throws IllegalArgumentException if there is no text or a language does not have the language code syntax
     */
    public static @NotNull LocalizedText of(@NotNull Map<@NotNull String, @NotNull String> texts) {
        if (texts.isEmpty()) {
            throw Exceptions.newIllegalArgumentException("A localized text needs at least one language.");
        }

        for (String language : texts.keySet()) {
            if (!isLanguage(language)) {
                throw Exceptions.newIllegalArgumentException("Invalid language: {0}", language);
            }
        }

        return new LocalizedText(Collections.unmodifiableMap(new LinkedHashMap<>(texts)));
    }


    private final @NotNull Map<@NotNull String, @NotNull String> texts;

    /**
     * Creates a {@link LocalizedText} instance.
     *
     * @param texts the checked, unmodifiable texts by language
     */
    private LocalizedText(@NotNull Map<@NotNull String, @NotNull String> texts) {
        this.texts = texts;
    }

    /**
     * @return the texts by language, in the listed order
     */
    public @NotNull Map<@NotNull String, @NotNull String> getTexts() {
        return this.texts;
    }

    /**
     * Returns the text in a language, falling back to the base language, then to {@value #FALLBACK_LANGUAGE}, then to the
     * first listed language.
     *
     * @param language the requested language, such as {@code ko} or {@code ko-KR}; case does not matter
     * @return the text to show
     */
    public @NotNull String get(@NotNull String language) {
        String exact = this.findIgnoringCase(language);

        if (exact != null) {
            return exact;
        }

        int separator = language.indexOf(LANGUAGE_SEPARATOR);

        if (separator > 0) {
            String base = this.findIgnoringCase(language.substring(0, separator));

            if (base != null) {
                return base;
            }
        }

        String fallback = this.texts.get(FALLBACK_LANGUAGE);

        return fallback != null ? fallback : this.texts.values().iterator().next();
    }

    private @Nullable String findIgnoringCase(@NotNull String language) {
        String lowerCaseLanguage = language.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> entry : this.texts.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).equals(lowerCaseLanguage)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
