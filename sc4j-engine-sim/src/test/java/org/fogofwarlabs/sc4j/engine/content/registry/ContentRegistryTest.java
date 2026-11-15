package org.fogofwarlabs.sc4j.engine.content.registry;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.modifier.ArmorUpgradeModifier;
import org.fogofwarlabs.sc4j.engine.content.modifier.Stat;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnReset;
import org.fogofwarlabs.sc4j.engine.entity.column.IntColumn;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the registration of content and its freeze into a {@link ContentDefinition}.
 */
final class ContentRegistryTest {
    private static final @NotNull ColumnKey<IntColumn> GROUP = ColumnKey.ofInt("group", 0, ColumnReset.KEEP);

    @Test
    void engine_stats_come_first_in_every_content() {
        ContentRegistry first = new ContentRegistry();
        ContentRegistry second = new ContentRegistry();
        Stat modStat = first.getStats().register(new Stat("mod stat"));

        Assertions.assertEquals(Stat.getEngineStats(), second.getStats().getTypes());
        Assertions.assertEquals(Stat.getEngineStats().size(), modStat.getIndex());

        first.addModifier(modStat, new ArmorUpgradeModifier(1));

        Assertions.assertEquals(1, first.build().getModifiers(modStat.getIndex()).length);
        Assertions.assertEquals(0, first.build().getModifiers(Stat.ARMOR.getIndex()).length);
    }

    @Test
    void build_should_freeze_every_registry_and_close_the_registration() {
        ContentRegistry registry = new ContentRegistry();

        registry.getUnitTypes().register(new UnitType("soldier"), new UnitDefinition.Builder().build());

        ContentDefinition definition = registry.build();

        Assertions.assertSame(definition, registry.build());
        Assertions.assertEquals(1, definition.getUnitTypes().getCount());
        Assertions.assertTrue(definition.getUnitTypes().getTypes().isFrozen());
        Assertions.assertTrue(definition.getStats().isFrozen());
        Assertions.assertThrows(IllegalStateException.class, registry::getUnitTypes);
        Assertions.assertThrows(IllegalStateException.class, () -> registry.setPlayerCount(2));
    }

    @Test
    void add_column_and_modifier_should_reject_invalid_registrations() {
        ContentRegistry registry = new ContentRegistry();

        registry.addColumn(TableKind.UNIT, GROUP, column -> column.set(0, 1));
        registry.addColumn(TableKind.WEAPON, GROUP, column -> column.set(0, 1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.addColumn(TableKind.UNIT, GROUP, column -> column.set(0, 1)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.addModifier(new Stat("unregistered"), new ArmorUpgradeModifier(1)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.setPlayerCount(-1));
        Assertions.assertEquals(1, registry.build().getColumns(TableKind.WEAPON).size());
    }
}
