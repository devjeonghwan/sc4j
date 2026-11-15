package org.fogofwarlabs.sc4j.engine.content.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the registration-order indices of {@link TypeRegistry} and its freeze.
 */
final class TypeRegistryTest {
    @Test
    void register_should_assign_indices_in_registration_order() {
        TypeRegistry<UnitType> registry = new TypeRegistry<>("unit type");
        UnitType first = registry.register(new UnitType("first"));
        UnitType second = registry.register(new UnitType("second"));

        Assertions.assertEquals(0, first.getIndex());
        Assertions.assertEquals(1, second.getIndex());
        Assertions.assertSame(second, registry.get(1));
        Assertions.assertEquals(2, registry.getCount());
        Assertions.assertTrue(registry.contains(first));
    }

    @Test
    void register_should_reject_a_duplicate_and_a_frozen_registry() {
        TypeRegistry<UnitType> registry = new TypeRegistry<>("unit type");
        UnitType type = registry.register(new UnitType("type"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.register(type));

        registry.freeze();

        Assertions.assertThrows(IllegalStateException.class, () -> registry.register(new UnitType("late")));
        Assertions.assertTrue(registry.isFrozen());
    }

    @Test
    void register_should_keep_the_index_in_a_new_session_and_reject_another_position() {
        UnitType first = new UnitType("first");
        UnitType second = new UnitType("second");
        TypeRegistry<UnitType> session = new TypeRegistry<>("unit type");

        session.register(first);
        session.register(second);

        TypeRegistry<UnitType> nextSession = new TypeRegistry<>("unit type");

        nextSession.register(first);
        nextSession.register(second);

        Assertions.assertEquals(1, second.getIndex());

        TypeRegistry<UnitType> reordered = new TypeRegistry<>("unit type");

        Assertions.assertThrows(IllegalStateException.class, () -> reordered.register(second));
    }

    @Test
    void require_index_should_map_null_to_no_index_and_reject_foreign_types() {
        TypeRegistry<WeaponType> registry = new TypeRegistry<>("weapon type");
        WeaponType registered = registry.register(new WeaponType("registered"));

        Assertions.assertEquals(ContentType.NO_INDEX, registry.requireIndex(null));
        Assertions.assertEquals(0, registry.requireIndex(registered));
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.requireIndex(new WeaponType("foreign")));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> registry.get(1));
    }
}
