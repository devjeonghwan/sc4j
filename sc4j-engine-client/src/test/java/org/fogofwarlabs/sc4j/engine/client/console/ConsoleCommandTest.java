package org.fogofwarlabs.sc4j.engine.client.console;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the declaration of a console command and its switch argument: {@code on} and {@code off} in any case, nothing else.
 */
final class ConsoleCommandTest {
    @Test
    void of_should_keep_the_lower_case_name_the_syntax_and_the_summary() {
        ConsoleCommand command = ConsoleCommand.of("/Zoom", "[on|off]", "zooms the camera");

        Assertions.assertEquals("/zoom", command.getName());
        Assertions.assertEquals("[on|off]", command.getSyntax());
        Assertions.assertEquals("zooms the camera", command.getSummary());
    }

    @Test
    void parse_switch_should_read_on_and_off_in_any_case() {
        Assertions.assertEquals(Boolean.TRUE, ConsoleCommand.parseSwitch(ConsoleCommand.ON));
        Assertions.assertEquals(Boolean.TRUE, ConsoleCommand.parseSwitch("ON"));
        Assertions.assertEquals(Boolean.FALSE, ConsoleCommand.parseSwitch(ConsoleCommand.OFF));
        Assertions.assertEquals(Boolean.FALSE, ConsoleCommand.parseSwitch("Off"));
        Assertions.assertNull(ConsoleCommand.parseSwitch("toggle"));
        Assertions.assertNull(ConsoleCommand.parseSwitch(""));
    }
}
