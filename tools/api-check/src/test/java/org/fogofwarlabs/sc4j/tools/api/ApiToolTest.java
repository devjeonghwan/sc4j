package org.fogofwarlabs.sc4j.tools.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Checks the commands the build runs: a dumped API listing passes the check until the API changes, and the internal use check fails on every
 * use of an internal element.
 */
final class ApiToolTest {
    private static final @NotNull String INTERNAL_PACKAGE = "org.example.loading";
    private static final @NotNull String UPDATE_TASK = ":example:apiDump";
    private static final @NotNull String UNIT_CLASS = "org.example.Unit";
    private static final @NotNull String UNIT_SOURCE = """
                                                       package org.example;
                                                       
                                                       public final class Unit {
                                                           public int getHealth() {
                                                               return 1;
                                                           }
                                                       }
                                                       """;
    private static final @NotNull String USE_SOURCE = """
                                                      package org.mod;
                                                      
                                                      import org.example.loading.ModLoader;
                                                      
                                                      final class Use {
                                                      }
                                                      """;
    private static final @NotNull String USE_LINE = "src/main/java/org/mod/Use.java: org.example.loading.ModLoader";
    private static final int SUCCESS_EXIT = 0;
    private static final int FAILURE_EXIT = 1;
    private static final int USAGE_EXIT = 2;

    private static int run(@NotNull ByteArrayOutputStream errors, @NotNull String @NotNull ... arguments) {
        try (PrintStream output = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
             PrintStream errorStream = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            return ApiTool.run(arguments, output, errorStream);
        }
    }

    private static int check(@NotNull Path directory, @NotNull Path classes, @NotNull ByteArrayOutputStream errors) {
        return run(errors, "check", "--api-file", directory.resolve("api/example.api").toString(), "--classes", classes.toString(),
                   "--internal", INTERNAL_PACKAGE, "--update-task", UPDATE_TASK, "--report", directory.resolve("report.txt").toString());
    }

    private static int checkUse(@NotNull Path directory, @NotNull ByteArrayOutputStream errors) {
        return run(errors, "use", "--base", directory.toString(), "--sources", directory.resolve("src/main/java").toString(),
                   "--internal", INTERNAL_PACKAGE, "--report", directory.resolve("use-report.txt").toString());
    }

    @Test
    void check_should_pass_on_the_dumped_listing_and_fail_on_a_changed_api(@TempDir @NotNull Path directory) throws IOException {
        Path classes = directory.resolve("classes");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        CompilationScenario.compile(directory.resolve("sources"), classes, Map.of(UNIT_CLASS, UNIT_SOURCE));

        Assertions.assertEquals(FAILURE_EXIT, check(directory, classes, errors));
        Assertions.assertEquals(SUCCESS_EXIT, run(errors, "dump", "--api-file", directory.resolve("api/example.api").toString(), "--classes",
                                                  classes.toString(), "--internal", INTERNAL_PACKAGE));
        Assertions.assertEquals(SUCCESS_EXIT, check(directory, classes, errors));
        Assertions.assertTrue(Files.isRegularFile(directory.resolve("report.txt")));

        CompilationScenario.compile(directory.resolve("changed-sources"), classes,
                                    Map.of(UNIT_CLASS, UNIT_SOURCE.replace("public int getHealth()", "public int getHealth(int scale)")));
        errors.reset();

        Assertions.assertEquals(FAILURE_EXIT, check(directory, classes, errors));

        String message = errors.toString(StandardCharsets.UTF_8);

        Assertions.assertTrue(message.contains("- public int getHealth();"), message);
        Assertions.assertTrue(message.contains("+ public int getHealth(int);"), message);
        Assertions.assertTrue(message.contains(UPDATE_TASK), message);
    }

    @Test
    void use_should_fail_on_every_internal_use(@TempDir @NotNull Path directory) throws IOException {
        Path source = directory.resolve("src/main/java/org/mod/Use.java");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        Files.createDirectories(source.getParent());
        Files.writeString(source, USE_SOURCE, StandardCharsets.UTF_8);

        Assertions.assertEquals(FAILURE_EXIT, checkUse(directory, errors));
        Assertions.assertTrue(errors.toString(StandardCharsets.UTF_8).contains(USE_LINE));

        Files.writeString(source, USE_SOURCE.replace("import org.example.loading.ModLoader;", ""), StandardCharsets.UTF_8);
        errors.reset();

        Assertions.assertEquals(SUCCESS_EXIT, checkUse(directory, errors));
    }

    @Test
    void run_should_reject_an_invalid_command_line() {
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        Assertions.assertEquals(USAGE_EXIT, run(errors, "--api-file", "example.api"));
        Assertions.assertEquals(USAGE_EXIT, run(errors, "list"));
        Assertions.assertEquals(USAGE_EXIT, run(errors, "check", "--classes", "classes"));
    }
}
