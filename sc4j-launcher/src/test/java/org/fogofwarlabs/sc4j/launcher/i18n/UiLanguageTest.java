package org.fogofwarlabs.sc4j.launcher.i18n;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Tests the selection of the launcher's UI language from the launcher settings and the operating system's locale.
 */
final class UiLanguageTest {
    @Test
    void select_should_follow_the_system_language_without_an_override() {
        Assertions.assertEquals(UiLanguage.KOREAN, UiLanguage.select(null, Locale.KOREA));
        Assertions.assertEquals(UiLanguage.ENGLISH, UiLanguage.select(null, Locale.US));
    }

    @Test
    void select_should_use_english_for_a_system_language_without_texts() {
        Assertions.assertEquals(UiLanguage.ENGLISH, UiLanguage.select(null, Locale.GERMANY));
    }

    @Test
    void select_should_prefer_the_override_to_the_system_language() {
        Assertions.assertEquals(UiLanguage.ENGLISH, UiLanguage.select(UiLanguage.ENGLISH, Locale.KOREA));
        Assertions.assertEquals(UiLanguage.KOREAN, UiLanguage.select(UiLanguage.KOREAN, Locale.US));
    }

    @Test
    void from_code_should_find_languages_ignoring_case() {
        Assertions.assertEquals(UiLanguage.KOREAN, UiLanguage.fromCode("KO"));
        Assertions.assertNull(UiLanguage.fromCode("de"));
    }
}
