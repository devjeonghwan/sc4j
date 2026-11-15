package org.fogofwarlabs.sc4j.tools.api;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the API tools for the build. Commands:
 * <ul>
 *     <li>{@code dump --api-file <file> --classes <folder>... --internal <name>...}: writes the API listing ({@link ApiListing}) of the
 *     compiled classes to the file.</li>
 *     <li>{@code check --api-file <file> --classes <folder>... --internal <name>... --update-task <task> --report <file>}: compares the API
 *     listing of the compiled classes with the file (a missing file is an empty listing) and fails with the difference; the update task is
 *     named in the failure.</li>
 *     <li>{@code use --base <folder> --sources <folder>... --internal <name>... --report <file>}: scans the sources for internal elements
 *     ({@link InternalUseScanner}) and fails on every use.</li>
 * </ul>
 * The {@code check} and {@code use} commands write a one-line report when they pass.
 */
public final class ApiTool {
    private static final @NotNull String DUMP_COMMAND = "dump";
    private static final @NotNull String CHECK_COMMAND = "check";
    private static final @NotNull String USE_COMMAND = "use";
    private static final @NotNull String OPTION_PREFIX = "--";
    private static final @NotNull String API_FILE_OPTION = "api-file";
    private static final @NotNull String CLASSES_OPTION = "classes";
    private static final @NotNull String INTERNAL_OPTION = "internal";
    private static final @NotNull String UPDATE_TASK_OPTION = "update-task";
    private static final @NotNull String REPORT_OPTION = "report";
    private static final @NotNull String BASE_OPTION = "base";
    private static final @NotNull String SOURCES_OPTION = "sources";
    private static final @NotNull String LINE_INDENT = "  ";
    private static final @NotNull String POLICY_FILE = "docs/api-policy.md";
    private static final int SUCCESS_EXIT = 0;
    private static final int FAILURE_EXIT = 1;
    private static final int USAGE_EXIT = 2;

    /**
     * Holds the parsed command line: the command and the values of each option in their order.
     */
    private static final class Arguments {
        private final @NotNull String command;
        private final @NotNull Map<@NotNull String, @NotNull List<@NotNull String>> options;

        private Arguments(@NotNull String @NotNull [] arguments) {
            if (arguments.length == 0 || arguments[0].startsWith(OPTION_PREFIX)) {
                throw Exceptions.newIllegalArgumentException("The first argument must be the command ({0}, {1} or {2}).", DUMP_COMMAND,
                                                             CHECK_COMMAND, USE_COMMAND);
            }

            this.command = arguments[0];
            this.options = new LinkedHashMap<>();

            String option = null;

            for (int index = 1; index < arguments.length; index++) {
                String argument = arguments[index];

                if (argument.startsWith(OPTION_PREFIX)) {
                    option = argument.substring(OPTION_PREFIX.length());
                    this.options.computeIfAbsent(option, name -> new ArrayList<>());
                } else if (option == null) {
                    throw Exceptions.newIllegalArgumentException("A value without an option: {0}", argument);
                } else {
                    this.options.get(option).add(argument);
                }
            }
        }

        private @NotNull List<@NotNull String> getValues(@NotNull String option) {
            return this.options.getOrDefault(option, List.of());
        }

        private @NotNull List<@NotNull Path> getPaths(@NotNull String option) {
            return this.getValues(option).stream().map(Path::of).toList();
        }

        private @NotNull String getValue(@NotNull String option) {
            List<String> values = this.getValues(option);

            if (values.size() != 1) {
                throw Exceptions.newIllegalArgumentException("The option --{0} needs exactly one value.", option);
            }

            return values.getFirst();
        }

        private @NotNull Path getPath(@NotNull String option) {
            return Path.of(this.getValue(option));
        }
    }

    /**
     * Runs a command and exits with its exit code.
     *
     * @param arguments the command and its options
     */
    public static void main(@NotNull String @NotNull [] arguments) {
        System.exit(run(arguments, System.out, System.err));
    }

    /**
     * Runs a command.
     *
     * @param arguments the command and its options
     * @param output    the stream of the messages of a passed command
     * @param errors    the stream of the failure messages
     * @return {@code 0} if the command passed, {@code 1} if a check failed, {@code 2} if the command line is invalid
     */
    public static int run(@NotNull String @NotNull [] arguments, @NotNull PrintStream output, @NotNull PrintStream errors) {
        try {
            Arguments parsed = new Arguments(arguments);
            InternalElements internal = new InternalElements(parsed.getValues(INTERNAL_OPTION));

            return switch (parsed.command) {
                case DUMP_COMMAND -> dump(parsed, internal, output);
                case CHECK_COMMAND -> check(parsed, internal, output, errors);
                case USE_COMMAND -> checkUse(parsed, internal, output, errors);
                default -> throw Exceptions.newIllegalArgumentException("Unknown command: {0}", parsed.command);
            };
        } catch (IllegalArgumentException exception) {
            errors.println(exception.getMessage());

            return USAGE_EXIT;
        } catch (IOException | IllegalStateException exception) {
            errors.println("The API tool failed: " + exception.getMessage());

            return FAILURE_EXIT;
        }
    }

    private static int dump(@NotNull Arguments arguments, @NotNull InternalElements internal, @NotNull PrintStream output) throws IOException {
        Path apiFile = arguments.getPath(API_FILE_OPTION);
        ApiListing listing = ApiListing.create(arguments.getPaths(CLASSES_OPTION), internal);
        Path directory = apiFile.toAbsolutePath().getParent();

        if (directory != null) {
            Files.createDirectories(directory);
        }

        Files.writeString(apiFile, listing.format(), StandardCharsets.UTF_8);

        output.println("Wrote the API listing of " + listing.getClassNames().size() + " classes: " + apiFile);

        return SUCCESS_EXIT;
    }

    private static int check(@NotNull Arguments arguments, @NotNull InternalElements internal,
                             @NotNull PrintStream output, @NotNull PrintStream errors) throws IOException {
        Path apiFile = arguments.getPath(API_FILE_OPTION);
        ApiListing expected = Files.isRegularFile(apiFile) ? ApiListing.parse(Files.readString(apiFile, StandardCharsets.UTF_8))
                                                           : ApiListing.parse("");
        ApiListing actual = ApiListing.create(arguments.getPaths(CLASSES_OPTION), internal);
        List<String> difference = expected.describeDifference(actual);

        if (!difference.isEmpty()) {
            errors.println("The public API differs from " + apiFile + " (- removed, + added):");
            difference.forEach(line -> errors.println(LINE_INDENT + line));
            errors.println("If the change is intended, run " + arguments.getValue(UPDATE_TASK_OPTION) + " and commit the changed .api file " +
                           "with the change (" + POLICY_FILE + ").");

            return FAILURE_EXIT;
        }

        writeReport(arguments.getPath(REPORT_OPTION), "The public API equals " + apiFile + ": " + actual.getClassNames().size() + " classes.");
        output.println("The public API equals " + apiFile + ".");

        return SUCCESS_EXIT;
    }

    private static int checkUse(@NotNull Arguments arguments, @NotNull InternalElements internal,
                                @NotNull PrintStream output, @NotNull PrintStream errors) throws IOException {
        List<String> uses = new InternalUseScanner(internal).scan(arguments.getPath(BASE_OPTION), arguments.getPaths(SOURCES_OPTION));

        if (!uses.isEmpty()) {
            errors.println("Mods must not use the engine's internal packages and classes (" + POLICY_FILE + "); use the API instead:");
            uses.forEach(use -> errors.println(LINE_INDENT + use));

            return FAILURE_EXIT;
        }

        writeReport(arguments.getPath(REPORT_OPTION), "Internal uses: 0.");
        output.println("No use of an internal engine element.");

        return SUCCESS_EXIT;
    }

    private static void writeReport(@NotNull Path report, @NotNull String text) throws IOException {
        Path directory = report.toAbsolutePath().getParent();

        if (directory != null) {
            Files.createDirectories(directory);
        }

        Files.writeString(report, text + "\n", StandardCharsets.UTF_8);
    }

    private ApiTool() {
    }
}
