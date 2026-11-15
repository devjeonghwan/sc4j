package org.fogofwarlabs.sc4j.manifest.settings;

import org.fogofwarlabs.sc4j.manifest.text.LocalizedText;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tests the locale options of a choice setting: the option of a locale and the default from the operating system's locale.
 */
final class ChoiceSettingTest {
    private static final @NotNull List<@NotNull String> LOCALES = List.of("en-US", "de_DE", "es-ES", "esMX", "ko-KR", "zh-TW");
    private static final @NotNull LocalizedText LABEL = LocalizedText.of(Map.of("en", "Text language"));

    @Test
    void find_option_should_match_language_and_country_before_the_language() {
        ChoiceSetting locale = new ChoiceSetting("locale", LABEL, null, false, "en-US", null, LOCALES, true);

        Assertions.assertEquals("ko-KR", locale.findOption(Locale.KOREA));
        Assertions.assertEquals("ko-KR", locale.findOption(Locale.KOREAN));
        Assertions.assertEquals("esMX", locale.findOption(Locale.of("es", "MX")));
        Assertions.assertEquals("es-ES", locale.findOption(Locale.of("es", "AR")));
        Assertions.assertEquals("de_DE", locale.findOption(Locale.of("de", "AT")));
        Assertions.assertEquals("zh-TW", locale.findOption(Locale.TAIWAN));
        Assertions.assertEquals("en-US", locale.findOption(Locale.UK));
        Assertions.assertNull(locale.findOption(Locale.JAPAN));
        Assertions.assertNull(locale.findOption(Locale.ROOT));
    }

    @Test
    void get_default_value_should_follow_the_system_locale_and_fall_back_to_the_declared_default() {
        ChoiceSetting system = new ChoiceSetting("locale", LABEL, null, false, "en-US", null, LOCALES, true);
        ChoiceSetting fixed = new ChoiceSetting("locale", LABEL, null, false, "en-US", null, LOCALES, false);
        Locale previous = Locale.getDefault();

        try {
            Locale.setDefault(Locale.KOREA);

            Assertions.assertEquals("ko-KR", system.getDefaultValue());
            Assertions.assertEquals("ko-KR", system.resolve(null));
            Assertions.assertEquals("en-US", fixed.getDefaultValue());

            Locale.setDefault(Locale.FRANCE);

            Assertions.assertEquals("en-US", system.getDefaultValue());
            Assertions.assertEquals("de_DE", system.resolve("de_DE"));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
