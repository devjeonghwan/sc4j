package org.fogofwarlabs.sc4j.launcher.launch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests keeping the last lines of a runtime's output.
 */
final class LogTailTest {
    @Test
    void add_should_drop_the_oldest_lines_beyond_the_capacity() {
        LogTail tail = new LogTail(2);

        tail.add("first");
        tail.add("second");
        tail.add("third");

        Assertions.assertEquals(List.of("second", "third"), tail.getLines());
        Assertions.assertEquals(1L, tail.getDroppedCount());
        Assertions.assertEquals(List.of("third"), tail.getLastLines(1));
    }
}
