package org.fogofwarlabs.sc4j.tools.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Checks which compiled classes the annotated internal packages and classes cover.
 */
final class InternalElementsTest {
    private static final @NotNull Map<@NotNull String, @NotNull String> SOURCES = Map.of(
            "org.example.loading.package-info", """
                                                @ApiStatus.Internal
                                                package org.example.loading;

                                                import org.jetbrains.annotations.ApiStatus;
                                                """,
            "org.example.loading.ModLoader", """
                                             package org.example.loading;

                                             public final class ModLoader {
                                                 public static final class Entry {
                                                 }
                                             }
                                             """,
            "org.example.loading.detail.Part", """
                                               package org.example.loading.detail;

                                               public final class Part {
                                               }
                                               """,
            "org.example.host.GlfwInput", """
                                          package org.example.host;

                                          import org.jetbrains.annotations.ApiStatus;

                                          @ApiStatus.Internal
                                          public final class GlfwInput {
                                              public interface Listener {
                                              }
                                          }
                                          """,
            "org.example.host.GlfwInputs", """
                                           package org.example.host;

                                           public final class GlfwInputs {
                                           }
                                           """,
            "org.example.host.WindowSettings", """
                                               package org.example.host;

                                               import org.jetbrains.annotations.ApiStatus;

                                               public final class WindowSettings {
                                                   @ApiStatus.Internal
                                                   public static final class Defaults {
                                                   }
                                               }
                                               """
    );

    @Test
    void read_should_find_annotated_packages_classes_and_their_nested_classes(@TempDir @NotNull Path directory) throws IOException {
        Path classes = directory.resolve("classes");

        CompilationScenario.compile(directory.resolve("sources"), classes, SOURCES);

        InternalElements internal = InternalElements.read(List.of(classes, directory.resolve("missing")));

        Assertions.assertTrue(internal.isInternal("org.example.loading.ModLoader"));
        Assertions.assertTrue(internal.isInternal("org.example.loading.ModLoader$Entry"));
        Assertions.assertTrue(internal.isInternal("org.example.host.GlfwInput"));
        Assertions.assertTrue(internal.isInternal("org.example.host.GlfwInput$Listener"));
        Assertions.assertTrue(internal.isInternal("org.example.host.WindowSettings$Defaults"));
        Assertions.assertFalse(internal.isInternal("org.example.loading.detail.Part"));
        Assertions.assertFalse(internal.isInternal("org.example.host.GlfwInputs"));
        Assertions.assertFalse(internal.isInternal("org.example.host.WindowSettings"));
    }
}
