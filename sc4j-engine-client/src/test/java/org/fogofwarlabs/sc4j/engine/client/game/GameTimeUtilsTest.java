package org.fogofwarlabs.sc4j.engine.client.game;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the game time of game loops: minutes and seconds, hours from one hour on, and the game loop read back from a game time.
 */
final class GameTimeUtilsTest {
    private static final long LOOP_MILLISECONDS = 42;

    @ParameterizedTest
    @CsvSource({"0, 0:00", "23, 0:00", "24, 0:01", "5667, 3:58", "85715, 1:00:00", "88572, 1:02:00"})
    void format_should_write_minutes_and_seconds_and_hours_from_one_hour_on(long loops, @NotNull String time) {
        Assertions.assertEquals(time, GameTimeUtils.format(loops, LOOP_MILLISECONDS));
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "90, 2143", "1:30, 2143", "3:58, 5667", "1:02:05, 88691", "1:60, -1", "x, -1", "1:2:3:4, -1", "-5, -1"})
    void parse_should_read_seconds_minutes_and_hours_as_the_first_loop_at_or_after_them(@NotNull String text, long loop) {
        Assertions.assertEquals(loop, GameTimeUtils.parse(text, LOOP_MILLISECONDS));
    }
}
