package org.fogofwarlabs.sc4j.tools.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

/**
 * Checks which compiled classes and which references in code the internal packages and classes cover.
 */
final class InternalElementsTest {
    private static final @NotNull InternalElements INTERNAL = new InternalElements(List.of("org.example.loading", "org.example.host.GlfwInput"));

    @Test
    void is_internal_should_cover_packages_subpackages_classes_and_nested_classes() {
        Assertions.assertTrue(INTERNAL.isInternal("org.example.loading.ModLoader"));
        Assertions.assertTrue(INTERNAL.isInternal("org.example.loading.detail.Entry$Part"));
        Assertions.assertTrue(INTERNAL.isInternal("org.example.host.GlfwInput"));
        Assertions.assertTrue(INTERNAL.isInternal("org.example.host.GlfwInput$Listener"));
        Assertions.assertFalse(INTERNAL.isInternal("org.example.host.GlfwInputs"));
        Assertions.assertFalse(INTERNAL.isInternal("org.example.host.WindowSettings"));
        Assertions.assertFalse(INTERNAL.isInternal("org.example.loadingtools.Tool"));
    }

    @Test
    void find_references_should_report_qualified_names_and_on_demand_imports() {
        String code = """
                      import org.example.loading.ModLoader;
                      import org.example.loading.detail.*;
                      import org.example.host.*;
                      import org.example.host.WindowSettings;
                      import org.example.loadingtools.Tool;
                      class Use { GlfwInput input; org.example.host.GlfwInputs other; }
                      """;

        Assertions.assertEquals(Set.of("org.example.loading.ModLoader", "org.example.loading.detail.*", "org.example.host.GlfwInput"),
                                INTERNAL.findReferences(code));
    }

    @Test
    void find_references_should_ignore_simple_names_without_an_on_demand_import() {
        String code = """
                      import org.example.host.WindowSettings;
                      class Use { GlfwInput input; }
                      """;

        Assertions.assertEquals(Set.of(), INTERNAL.findReferences(code));
    }

    @Test
    void constructor_should_reject_names_that_are_not_qualified() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InternalElements(List.of("loading")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InternalElements(List.of("org.example.")));
    }
}
