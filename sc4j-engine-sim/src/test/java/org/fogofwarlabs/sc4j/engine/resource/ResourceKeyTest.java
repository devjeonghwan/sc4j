package org.fogofwarlabs.sc4j.engine.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the syntax and equality of resource keys.
 */
final class ResourceKeyTest {
    @Test
    void of_should_accept_segmented_names_and_compare_by_name() {
        ResourceKey key = ResourceKey.of("units/foot-man_2.png");

        Assertions.assertEquals("units/foot-man_2.png", key.getName());
        Assertions.assertEquals(ResourceKey.of("units/foot-man_2.png"), key);
        Assertions.assertNotEquals(ResourceKey.of("Units/foot-man_2.png"), key);
    }

    @Test
    void of_should_reject_empty_dot_and_foreign_segments() {
        for (String name : new String[]{"", "/units", "units/", "units//a", "./a", "a/../b", "a\\b", "a b", "c:/a"}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> ResourceKey.of(name), name);
        }
    }
}
