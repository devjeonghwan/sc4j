package org.fogofwarlabs.sc4j.tools.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writes Java sources into a folder and compiles them with the JDK's compiler, for the tests of the API tools.
 */
final class CompilationScenario {
    private static final @NotNull String JAVA_FILE_SUFFIX = ".java";
    private static final @NotNull String DESTINATION_OPTION = "-d";
    private static final int SUCCESS_EXIT = 0;

    /**
     * Writes and compiles sources.
     *
     * @param sourceFolder the folder the sources are written to
     * @param classFolder  the folder the classes are compiled to
     * @param sources      the source text per qualified top-level class name
     * @throws IOException if a source cannot be written
     */
    static void compile(@NotNull Path sourceFolder, @NotNull Path classFolder, @NotNull Map<@NotNull String, @NotNull String> sources) throws IOException {
        List<String> arguments = new ArrayList<>(List.of(DESTINATION_OPTION, classFolder.toString()));

        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = sourceFolder.resolve(source.getKey().replace('.', '/') + JAVA_FILE_SUFFIX);

            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            arguments.add(file.toString());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exitCode = compiler.run(null, null, errors, arguments.toArray(String[]::new));

        Assertions.assertEquals(SUCCESS_EXIT, exitCode, errors.toString(StandardCharsets.UTF_8));
    }

    private CompilationScenario() {
    }
}
