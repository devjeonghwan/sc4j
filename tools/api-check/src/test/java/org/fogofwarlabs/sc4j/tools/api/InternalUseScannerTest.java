package org.fogofwarlabs.sc4j.tools.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Checks that the scan of mod sources reports the internal elements their code uses and ignores comments and literals.
 */
final class InternalUseScannerTest {
    private static final @NotNull InternalElements INTERNAL = new InternalElements(List.of("org.example.loading", "org.example.host.GlfwInput"));

    private static void write(@NotNull Path file, @NotNull String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    @Test
    void scan_should_report_code_uses_per_file(@TempDir @NotNull Path directory) throws IOException {
        Path sources = directory.resolve("src/main/java");

        write(sources.resolve("org/mod/Uses.java"), """
                                                    package org.mod;
                                                    
                                                    import org.example.loading.ModLoader;
                                                    
                                                    final class Uses {
                                                        private final org.example.host.GlfwInput input = null;
                                                    }
                                                    """);
        write(sources.resolve("org/mod/Mentions.java"), """
                                                        package org.mod;
                                                        
                                                        /**
                                                         * Not loaded by {@link org.example.loading.ModLoader}.
                                                         */
                                                        final class Mentions {
                                                            // Not org.example.host.GlfwInput either.
                                                            private static final String TEXT = "org.example.loading.ModLoader";
                                                            private static final char SEPARATOR = '.';
                                                        }
                                                        """);

        Assertions.assertEquals(List.of("src/main/java/org/mod/Uses.java: org.example.host.GlfwInput",
                                        "src/main/java/org/mod/Uses.java: org.example.loading.ModLoader"),
                                new InternalUseScanner(INTERNAL).scan(directory, List.of(sources, directory.resolve("src/missing/java"))));
    }
}
