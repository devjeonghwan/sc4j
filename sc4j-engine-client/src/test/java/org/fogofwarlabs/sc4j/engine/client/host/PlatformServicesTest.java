package org.fogofwarlabs.sc4j.engine.client.host;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Tests the platform services of the host: the system pointer speed, the host tick clock and the input method, their portable defaults and
 * the Windows defaults where the tests run on Windows. The pointer speed is only read, never set.
 */
final class PlatformServicesTest {
    /**
     * The longest time between two clock readings of the test, in milliseconds.
     */
    private static final int CLOCK_TOLERANCE_MILLISECONDS = 10_000;
    private static final int KOREAN_LANGUAGE_IDENTIFIER = 0x0412;
    private static final int TRADITIONAL_CHINESE_LANGUAGE_IDENTIFIER = 0x0404;
    private static final @NotNull String OPERATING_SYSTEM_PROPERTY = "os.name";
    private static final @NotNull String WINDOWS_PREFIX = "windows";

    private static boolean isWindows() {
        return System.getProperty(OPERATING_SYSTEM_PROPERTY, "").toLowerCase(Locale.ROOT).startsWith(WINDOWS_PREFIX);
    }

    @Test
    void fixed_pointer_speed_reads_no_speed_and_sets_nothing() {
        FixedSystemPointerSpeed speed = new FixedSystemPointerSpeed();

        speed.writeSpeed(SystemPointerSpeed.DEFAULT_SPEED);

        Assertions.assertEquals(-1, speed.readSpeed());
    }

    @Test
    void default_pointer_speed_reads_the_platform_speed_in_range() {
        SystemPointerSpeed speed = SystemPointerSpeed.getDefault();
        int current = speed.readSpeed();

        Assertions.assertEquals(isWindows(), speed instanceof WindowsSystemPointerSpeed);
        Assertions.assertTrue(current == -1 || current >= SystemPointerSpeed.MINIMUM_SPEED && current <= SystemPointerSpeed.MAXIMUM_SPEED,
                              "speed " + current);
    }

    @Test
    void tick_clocks_count_forward_in_milliseconds() {
        for (HostTickClock clock : new HostTickClock[]{HostTickClock.getDefault(), new MonotonicHostTickClock()}) {
            int first = clock.readTicks();
            int second = clock.readTicks();

            Assertions.assertTrue(Integer.toUnsignedLong(second - first) <= CLOCK_TOLERANCE_MILLISECONDS, clock.getClass().getSimpleName());
        }

        Assertions.assertEquals(isWindows(), HostTickClock.getDefault() instanceof WindowsHostTickClock);
    }

    @Test
    void no_input_method_never_enables_text_input() {
        NoTextInputMethod method = new NoTextInputMethod();

        method.setListener(null);
        method.setEnabled(true);
        method.cancelComposition();

        Assertions.assertFalse(method.isEnabled());
        Assertions.assertNull(method.getCompositionLocale());
    }

    @Test
    void find_locale_should_name_windows_language_identifiers() {
        if (!isWindows()) {
            Assertions.assertNull(WindowsTextInputMethod.findLocale(KOREAN_LANGUAGE_IDENTIFIER));

            return;
        }

        Assertions.assertEquals(Locale.forLanguageTag("ko-KR"), WindowsTextInputMethod.findLocale(KOREAN_LANGUAGE_IDENTIFIER));
        Assertions.assertEquals(Locale.forLanguageTag("zh-TW"), WindowsTextInputMethod.findLocale(TRADITIONAL_CHINESE_LANGUAGE_IDENTIFIER));
    }

    @Test
    void text_composition_holds_its_clause_and_cursor_inside_the_text() {
        TextComposition composition = new TextComposition("abc", 1, 2, 3);

        Assertions.assertEquals("abc", composition.getText());
        Assertions.assertEquals(1, composition.getClauseStart());
        Assertions.assertEquals(2, composition.getClauseEnd());
        Assertions.assertEquals(3, composition.getCursor());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TextComposition("abc", 2, 1, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TextComposition("abc", 0, 0, 4));
    }
}
