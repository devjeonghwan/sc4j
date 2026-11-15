package org.fogofwarlabs.sc4j.engine.client.console;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the console command registry, the declared commands and their generated help.
 */
final class ConsoleCommandRegistryTest {
    private static final int LONG_TEXT_WORD_COUNT = 40;
    private static final int WRAPPED_HELP_LINE_THRESHOLD = 4;
    /**
     * The widest help line.
     */
    private static final int HELP_WIDTH = 72;
    /**
     * The reply lines of one page before its footer.
     */
    private static final int PAGE_LINES = 9;

    private static final @NotNull String UNKNOWN_COMMAND_REPLY = "Unknown command, type /help";
    private static final @NotNull String NO_MORE_REPLY = "No more lines.";
    private static final @NotNull String REFUSAL = "Not available here.";

    private static @NotNull ConsoleCommand createEchoCommand() {
        return ConsoleCommand.of("/echo", "<words>|twice <words>", "repeat the words")
                             .describe("Repeats what follows.")
                             .subcommand("twice",
                                         "<words>",
                                         "repeat the words twice",
                                         arguments -> String.join(" ", arguments) + " " + String.join(" ", arguments))
                             .otherwise("<words>", "repeat the words", arguments -> String.join(" ", arguments))
                             .section("Notes:")
                             .item("case", "the words keep their case")
                             .availability("everywhere.")
                             .example("/echo hello");
    }


    @Test
    void registry_runs_declared_forms_and_lists_them_in_help() {
        ConsoleCommandRegistry registry = new ConsoleCommandRegistry(PAGE_LINES);

        registry.register(createEchoCommand());

        Assertions.assertEquals("a B", registry.run("/echo a B"));
        Assertions.assertEquals("a B", registry.run("/ECHO a B"), "names are case-insensitive");
        Assertions.assertEquals("a a", registry.run("/echo TWICE a"), "sub-command words are case-insensitive");
        Assertions.assertEquals("Use /echo <words>|twice <words> (/help echo).", registry.run("/echo"), "no argument form: the usage");
        Assertions.assertEquals(UNKNOWN_COMMAND_REPLY, registry.run("/nothing"));

        List<String> help = registry.getHelpLines();

        Assertions.assertEquals("/help [command], /? - list the commands, or explain one command", help.getFirst());
        Assertions.assertEquals("/more - show the next page of a long reply", help.get(1));
        Assertions.assertEquals("/echo <words>|twice <words> - repeat the words", help.get(2));
        Assertions.assertEquals(registry.run("/help"), registry.run("/?"));
    }

    @Test
    void help_command_prints_detailed_help_of_one_command() {
        ConsoleCommandRegistry registry = new ConsoleCommandRegistry(PAGE_LINES);

        registry.register(createEchoCommand());

        List<String> expected = List.of("/echo <words>|twice <words> - repeat the words",
                                        "  Repeats what follows.",
                                        "Forms:",
                                        "  /echo twice <words> - repeat the words twice",
                                        "  /echo <words> - repeat the words",
                                        "Notes:",
                                        "  case - the words keep their case",
                                        "Where: everywhere.",
                                        "Example: /echo hello");

        Assertions.assertEquals(String.join("\n", expected), registry.run("/help echo"));
        Assertions.assertEquals(registry.run("/help echo"), registry.run("/? /echo"), "the name with or without its slash");
        Assertions.assertTrue(registry.run("/help nothing").startsWith("Unknown command: /nothing."));
        Assertions.assertTrue(registry.run("/help more").startsWith("/more - "));
        Assertions.assertTrue(registry.run("/help more").endsWith("shows " + PAGE_LINES + " lines at a time."));
    }

    @Test
    void run_should_refuse_when_requirement_fails() {
        boolean[] available = {false};
        List<String> runs = new ArrayList<>();
        ConsoleCommandRegistry registry = new ConsoleCommandRegistry(PAGE_LINES);

        registry.register(ConsoleCommand.of("/go", "", "go")
                                        .whenEmpty("go", arguments -> {
                                            runs.add("go");

                                            return "Went.";
                                        })
                                        .requires(() -> available[0], REFUSAL));

        Assertions.assertEquals(REFUSAL, registry.run("/go"));
        Assertions.assertTrue(runs.isEmpty());

        available[0] = true;

        Assertions.assertEquals("Went.", registry.run("/go"));
    }

    @Test
    void get_help_lines_should_wrap_long_text_at_help_width() {
        String words = "word ".repeat(LONG_TEXT_WORD_COUNT).trim();
        List<String> help = ConsoleCommand.of("/long", "", "a long command").describe(words).whenEmpty(words, arguments -> "").getHelpLines();

        Assertions.assertTrue(help.size() > WRAPPED_HELP_LINE_THRESHOLD);

        for (String line : help) {
            Assertions.assertTrue(line.length() <= HELP_WIDTH, line);
        }

        Assertions.assertTrue(help.get(1).startsWith("  word"), "description lines are indented");
        Assertions.assertTrue(help.stream().anyMatch(line -> line.startsWith("      word")), "a wrapped form continues further indented");
    }

    @Test
    void run_paged_should_page_long_reply_with_more() {
        ConsoleCommandRegistry registry = new ConsoleCommandRegistry(PAGE_LINES);
        List<String> lines = new ArrayList<>();

        for (int index = 0; index < PAGE_LINES * 2 + 1; index++) {
            lines.add("line " + index);
        }

        registry.register(ConsoleCommand.of("/many", "", "many lines").whenEmpty("print many lines", arguments -> String.join("\n", lines)));

        Assertions.assertEquals(String.join("\n", lines), registry.run("/many"), "run returns the whole reply");

        List<String> first = registry.runPaged("/many");

        Assertions.assertEquals(PAGE_LINES + 1, first.size());
        Assertions.assertEquals("line 0", first.getFirst());
        Assertions.assertEquals("-- page 1 of 3, /more shows the next --", first.getLast());

        List<String> second = registry.runPaged("/more");

        Assertions.assertEquals("line " + PAGE_LINES, second.getFirst());
        Assertions.assertEquals("-- page 2 of 3, /more shows the next --", second.getLast());
        Assertions.assertEquals(List.of("line " + PAGE_LINES * 2), registry.runPaged("/MORE"), "the last page has no footer");
        Assertions.assertEquals(List.of(NO_MORE_REPLY), registry.runPaged("/more"));
        Assertions.assertEquals(List.of(UNKNOWN_COMMAND_REPLY), registry.runPaged("/nothing"));
    }

    @Test
    void slash_lines_are_commands_and_command_names_require_slash() {
        Assertions.assertTrue(ConsoleCommandRegistry.isCommand("/debug"));
        Assertions.assertFalse(ConsoleCommandRegistry.isCommand("show me the money"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ConsoleCommand.of("debug", "", "no slash"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ConsoleCommandRegistry(0));
    }
}
