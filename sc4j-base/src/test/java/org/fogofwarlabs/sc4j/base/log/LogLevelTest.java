package org.fogofwarlabs.sc4j.base.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the level names that {@link LogLevel#parse} accepts.
 */
final class LogLevelTest {
    @Test
    void parse_should_read_level_names_and_return_null_for_unknown_names() {
        Assertions.assertEquals(LogLevel.WARNING, LogLevel.parse("warn"));
        Assertions.assertEquals(LogLevel.WARNING, LogLevel.parse("WARNING"));
        Assertions.assertEquals(LogLevel.TRACE, LogLevel.parse(" trace "));
        Assertions.assertNull(LogLevel.parse("verbose"));
    }
}
