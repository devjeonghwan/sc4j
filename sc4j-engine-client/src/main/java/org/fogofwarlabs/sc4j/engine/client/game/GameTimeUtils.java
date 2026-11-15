package org.fogofwarlabs.sc4j.engine.client.game;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Writes and reads game time: the time a number of game loops takes at normal speed, given the client time of one loop, as minutes and seconds
 * ({@code 3:58}), with hours in front from one hour on ({@code 1:02:05}). The game clocks, replay lists and replay viewers of the engine and the
 * mods show game time instead of game loops.
 */
public final class GameTimeUtils {
    private static final long MILLISECONDS_PER_SECOND = 1000;
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_HOUR = 3600;
    private static final char SEPARATOR = ':';
    private static final int MAXIMUM_PARTS = 3;

    /**
     * Writes the game time of a number of game loops.
     *
     * @param loops            the game loops
     * @param loopMilliseconds the client time of one game loop at normal speed in milliseconds
     * @return the game time, for example {@code "3:58"} or {@code "1:02:05"}
     */
    public static @NotNull String format(long loops, long loopMilliseconds) {
        long seconds = Math.max(0, loops) * loopMilliseconds / MILLISECONDS_PER_SECOND;

        if (seconds >= SECONDS_PER_HOUR) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / SECONDS_PER_HOUR, seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
                                 seconds % SECONDS_PER_MINUTE);
        }

        return String.format(Locale.ROOT, "%d:%02d", seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE);
    }

    /**
     * Reads a game time as the first game loop at or after it: seconds ({@code 90}), minutes and seconds ({@code 1:30}) or hours, minutes and
     * seconds ({@code 1:02:05}).
     *
     * @param text             the game time
     * @param loopMilliseconds the client time of one game loop at normal speed in milliseconds
     * @return the game loop, or {@code -1} if the text is no game time
     */
    public static long parse(@NotNull String text, long loopMilliseconds) {
        String[] parts = text.strip().split(String.valueOf(SEPARATOR), -1);
        long seconds = 0;

        if (parts.length > MAXIMUM_PARTS || loopMilliseconds <= 0) {
            return -1;
        }

        for (int index = 0; index < parts.length; index++) {
            long part;

            try {
                part = Long.parseLong(parts[index]);
            } catch (NumberFormatException exception) {
                return -1;
            }

            // Every part after the first counts below sixty.
            if (part < 0 || index > 0 && part >= SECONDS_PER_MINUTE) {
                return -1;
            }

            seconds = seconds * SECONDS_PER_MINUTE + part;
        }

        return (seconds * MILLISECONDS_PER_SECOND + loopMilliseconds - 1) / loopMilliseconds;
    }

    private GameTimeUtils() {
    }
}
