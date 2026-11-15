package org.fogofwarlabs.sc4j.base.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Holds the process-wide logging configuration and writes formatted lines to the console and the optional rolling file.
 * <p>
 * A line is {@code yyyy-MM-dd HH:mm:ss.SSS LEVEL [thread] Logger - message}. Errors and warnings go to the standard
 * error stream, other levels to the standard output. The default threshold is {@link LogLevel#INFO} without a file.
 */
public final class Logging {
    /**
     * The file name of the current log; rolled files insert their index, as {@code sc4j.1.log}.
     */
    public static final @NotNull String FILE_NAME = "sc4j.log";
    /**
     * The number of log files kept, including the current one.
     */
    public static final int FILE_COUNT = 5;
    /**
     * The size in bytes at which the current log rolls over.
     */
    public static final long FILE_SIZE_LIMIT = 10L * 1024 * 1024;

    private static final @NotNull DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final @NotNull Object LOCK = new Object();

    private static volatile @NotNull LogLevel threshold = LogLevel.INFO;
    private static @Nullable RollingLogFile file;

    /**
     * @return the current threshold
     */
    public static @NotNull LogLevel getLevel() {
        return threshold;
    }

    /**
     * Sets the threshold: messages below it are dropped.
     *
     * @param level the new threshold
     */
    public static void setLevel(@NotNull LogLevel level) {
        threshold = level;
    }

    /**
     * @param level the message level
     * @return {@code true} if messages of the level are written, {@code false} otherwise
     */
    public static boolean isEnabled(@NotNull LogLevel level) {
        return level != LogLevel.OFF && level.getPriority() >= threshold.getPriority();
    }

    /**
     * @return {@code true} if lines are also written to a log file, {@code false} otherwise
     */
    public static boolean isFileEnabled() {
        synchronized (LOCK) {
            return file != null;
        }
    }


    /**
     * Starts writing every line also to {@value #FILE_NAME} in a directory, rolling it at {@link #FILE_SIZE_LIMIT} bytes and
     * keeping {@link #FILE_COUNT} files. A previous file is closed first.
     *
     * @param directory the log directory, created if absent
     * @throws IOException if the directory or the file cannot be opened
     */
    public static void enableFile(@NotNull Path directory) throws IOException {
        RollingLogFile opened = new RollingLogFile(directory, FILE_NAME, FILE_COUNT, FILE_SIZE_LIMIT);

        synchronized (LOCK) {
            if (file != null) {
                file.close();
            }

            file = opened;
        }
    }

    /**
     * Stops writing to the log file, if any.
     */
    public static void disableFile() {
        synchronized (LOCK) {
            if (file != null) {
                file.close();
                file = null;
            }
        }
    }

    /**
     * Writes one formatted line to the console and, if enabled, to the log file; a file write failure disables the file.
     *
     * @param level      the message level
     * @param loggerName the name of the writing logger
     * @param message    the formatted message
     */
    static void write(@NotNull LogLevel level, @NotNull String loggerName, @NotNull String message) {
        String line = TIME_FORMAT.format(LocalDateTime.now()) + ' ' + String.format("%-5s", level.getTitle()) + " [" +
                      Thread.currentThread().getName() + "] " + loggerName + " - " + message;
        PrintStream console = level.getPriority() >= LogLevel.WARNING.getPriority() ? System.err : System.out;

        synchronized (LOCK) {
            console.println(line);

            if (file != null && !file.write(line)) {
                file.close();
                file = null;
                System.err.println("Log file disabled after a write failure.");
            }
        }
    }


    private Logging() {
    }
}
