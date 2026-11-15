package org.fogofwarlabs.sc4j.engine.trigger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the trigger registry: one entry per type id, replacement before the freeze, lookups of any id and the freeze.
 */
final class TriggerRegistryTest {
    private static final int TYPE_COUNT = 4;

    @Test
    void find_should_return_the_registered_entry_and_null_outside_the_table() {
        TriggerRegistry<String> registry = new TriggerRegistry<>(TYPE_COUNT);

        registry.register(1, "first");
        registry.register(1, "replaced");
        registry.register(TYPE_COUNT - 1, "last");

        Assertions.assertEquals(TYPE_COUNT, registry.getCount());
        Assertions.assertEquals("replaced", registry.find(1));
        Assertions.assertEquals("last", registry.find(TYPE_COUNT - 1));
        Assertions.assertNull(registry.find(0));
        Assertions.assertNull(registry.find(-1));
        Assertions.assertNull(registry.find(TYPE_COUNT));
    }

    @Test
    void register_should_reject_types_outside_the_table_and_a_frozen_registry() {
        TriggerRegistry<String> registry = new TriggerRegistry<>(TYPE_COUNT);

        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.register(TYPE_COUNT, "outside"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.register(-1, "outside"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TriggerRegistry<String>(-1));

        registry.register(0, "kept");
        registry.freeze();
        registry.freeze();

        Assertions.assertTrue(registry.isFrozen());
        Assertions.assertThrows(IllegalStateException.class, () -> registry.register(0, "late"));
        Assertions.assertEquals("kept", registry.find(0));
    }
}
