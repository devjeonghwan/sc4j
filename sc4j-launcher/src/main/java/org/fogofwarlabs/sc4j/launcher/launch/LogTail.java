package org.fogofwarlabs.sc4j.launcher.launch;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Keeps the last lines of a runtime's output, up to a capacity; older lines are dropped. The launcher keeps the output
 * of a run in one tail and reports its last lines when the run fails.
 */
public final class LogTail {
    private final int capacity;
    private final @NotNull Deque<@NotNull String> lines;

    private long droppedCount;

    /**
     * Creates a {@link LogTail} instance without lines.
     *
     * @param capacity the largest number of lines kept
     * @throws IllegalArgumentException if the capacity is not positive
     */
    public LogTail(int capacity) {
        if (capacity <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid log tail capacity: {0}", capacity);
        }

        this.capacity = capacity;
        this.lines    = new ArrayDeque<>(capacity);

        this.droppedCount = 0L;
    }

    /**
     * @return the number of lines dropped because the capacity was reached
     */
    public long getDroppedCount() {
        return this.droppedCount;
    }

    /**
     * @return the kept lines, oldest first
     */
    public @NotNull List<@NotNull String> getLines() {
        return List.copyOf(this.lines);
    }

    /**
     * Returns the last kept lines.
     *
     * @param count the largest number of lines
     * @return up to {@code count} lines, oldest first
     */
    public @NotNull List<@NotNull String> getLastLines(int count) {
        List<String> all = new ArrayList<>(this.lines);

        return List.copyOf(all.subList(Math.max(0, all.size() - count), all.size()));
    }

    /**
     * Adds a line, dropping the oldest line when the capacity is reached.
     *
     * @param line the output line without its line terminator
     */
    public void add(@NotNull String line) {
        if (this.lines.size() == this.capacity) {
            this.lines.removeFirst();
            this.droppedCount++;
        }

        this.lines.addLast(line);
    }
}
