package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.definition.UpgradeDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponDefinition;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnReset;
import org.fogofwarlabs.sc4j.engine.entity.column.IntColumn;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests that a {@link GameContent} copies the definitions into writable per-game tables that the state hash covers.
 */
final class GameContentTest {
    private static final int PLAYER_COUNT = 2;
    private static final int SOLDIER_HIT_POINTS = 40;
    private static final int SOLDIER_GOLD = 50;
    private static final int FORT_GOLD = 400;
    private static final int FORT_WOOD = 150;
    private static final int GROUP_FLAGS = 0x15;
    private static final int RAISED_HIT_POINTS = 45;
    private static final int SWORD_DAMAGE = 6;
    private static final int RAISED_SWORD_DAMAGE = 7;
    private static final int MAXIMUM_LEVEL = 3;

    private static final @NotNull ColumnKey<IntColumn> GROUP = ColumnKey.ofInt("group", 0, ColumnReset.KEEP);
    private static final @NotNull ColumnKey<IntColumn> DEATHS = ColumnKey.ofInt("deaths", 0, ColumnReset.KEEP);
    private static final @NotNull ColumnKey<IntColumn> RESEARCHING = ColumnKey.ofInt("researching", 0, ColumnReset.KEEP);

    private static long hash(@NotNull GameContent content) {
        StateHash hash = new StateHash();

        content.contributeStateHash(hash);

        return hash.getValue();
    }

    private final @NotNull ResourceKind gold;
    private final @NotNull ResourceKind wood;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType fort;
    private final @NotNull WeaponType sword;
    private final @NotNull UpgradeType blades;
    private final @NotNull ContentRegistry registry;

    GameContentTest() {
        this.gold     = new ResourceKind("gold");
        this.wood     = new ResourceKind("wood");
        this.soldier  = new UnitType("soldier");
        this.fort     = new UnitType("fort");
        this.sword    = new WeaponType("sword");
        this.blades   = new UpgradeType("blades");
        this.registry = new ContentRegistry();

        this.registry.getResourceKinds().register(this.gold);
        this.registry.getResourceKinds().register(this.wood);
        this.registry.getUpgradeTypes().register(this.blades, new UpgradeDefinition.Builder().setMaximumLevel(MAXIMUM_LEVEL).build());
        this.registry.getWeaponTypes().register(this.sword, new WeaponDefinition.Builder().setDamage(SWORD_DAMAGE).setUpgrade(this.blades).build());
        this.registry.getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setHitPoints(SOLDIER_HIT_POINTS).setGroundWeapon(this.sword)
                                                                                        .setCost(ResourceCost.of(this.gold, SOLDIER_GOLD)).build());
        this.registry.getUnitTypes().register(this.fort, new UnitDefinition.Builder().addTrait(UnitTrait.BUILDING)
                                                                                     .setCost(new ResourceCost.Builder().add(this.gold, FORT_GOLD)
                                                                                                                        .add(this.wood, FORT_WOOD)
                                                                                                                        .build())
                                                                                     .build());
    }

    @Test
    void create_should_copy_definitions_and_resolve_references() {
        GameContent content = GameContent.create(this.registry.build(), PLAYER_COUNT);
        UnitTypeTable units = content.getUnitTypes();

        Assertions.assertEquals(2, units.getCount());
        Assertions.assertEquals(SOLDIER_HIT_POINTS, units.getHitPoints(this.soldier));
        Assertions.assertSame(this.sword, units.getGroundWeapon(this.soldier));
        Assertions.assertNull(units.getAirWeapon(this.soldier));
        Assertions.assertEquals(SOLDIER_GOLD, units.getCost(this.soldier, this.gold));
        Assertions.assertEquals(0, units.getCost(this.soldier, this.wood));
        Assertions.assertEquals(FORT_WOOD, units.getCost(this.fort.getIndex(), this.wood.getIndex()));
        Assertions.assertTrue(units.hasTrait(this.fort, UnitTrait.BUILDING));
        Assertions.assertFalse(units.hasTrait(this.soldier, UnitTrait.BUILDING));
        Assertions.assertSame(this.blades, content.getWeaponTypes().getUpgrade(this.sword));
        Assertions.assertEquals(MAXIMUM_LEVEL, content.getProgress().getMaximumUpgradeLevel(1, this.blades.getIndex()));
        Assertions.assertTrue(content.getProgress().isUnitAvailable(1, this.fort.getIndex()));
    }

    @Test
    void create_should_reject_a_reference_to_an_unregistered_type() {
        this.registry.getUnitTypes().register(new UnitType("archer"), new UnitDefinition.Builder().setAirWeapon(new WeaponType("bow")).build());

        Assertions.assertThrows(IllegalArgumentException.class, () -> GameContent.create(this.registry.build(), PLAYER_COUNT));
    }

    @Test
    void extension_columns_are_initialized_by_the_mod_and_writable() {
        this.registry.addColumn(TableKind.UNIT, GROUP, column -> column.set(this.fort.getIndex(), GROUP_FLAGS));

        GameContent content = GameContent.create(this.registry.build(), PLAYER_COUNT);
        IntColumn group = content.getUnitTypes().getExtensionColumns().get(GROUP);

        Assertions.assertEquals(2, group.getLength());
        Assertions.assertEquals(GROUP_FLAGS, group.get(this.fort.getIndex()));
        Assertions.assertEquals(0, group.get(this.soldier.getIndex()));
    }

    @Test
    void every_game_starts_from_the_definitions() {
        GameContent first = GameContent.create(this.registry.build(), PLAYER_COUNT);
        GameContent second = GameContent.create(this.registry.build(), PLAYER_COUNT);

        first.getUnitTypes().setHitPoints(this.soldier, RAISED_HIT_POINTS);

        Assertions.assertEquals(RAISED_HIT_POINTS, first.getUnitTypes().getHitPoints(this.soldier.getIndex()));
        Assertions.assertEquals(SOLDIER_HIT_POINTS, second.getUnitTypes().getHitPoints(this.soldier.getIndex()));
    }

    @Test
    void state_hash_covers_table_progress_and_extension_values() {
        this.registry.addColumn(TableKind.UNIT, GROUP, column -> column.set(this.fort.getIndex(), GROUP_FLAGS));

        GameContent content = GameContent.create(this.registry.build(), PLAYER_COUNT);
        long initial = hash(content);

        content.getUnitTypes().setHitPoints(this.soldier, RAISED_HIT_POINTS);
        Assertions.assertNotEquals(initial, hash(content));
        content.getUnitTypes().setHitPoints(this.soldier, SOLDIER_HIT_POINTS);
        Assertions.assertEquals(initial, hash(content));

        content.getProgress().setUpgradeLevel(1, this.blades.getIndex(), 1);
        Assertions.assertNotEquals(initial, hash(content));
        content.getProgress().setUpgradeLevel(1, this.blades.getIndex(), 0);

        content.getUnitTypes().getExtensionColumns().get(GROUP).set(this.soldier.getIndex(), 1);
        Assertions.assertNotEquals(initial, hash(content));
    }

    @Test
    void state_hash_recomputes_a_table_digest_after_every_kind_of_write() {
        this.registry.addColumn(TableKind.UNIT, GROUP, column -> column.set(this.fort.getIndex(), GROUP_FLAGS));

        GameContent content = GameContent.create(this.registry.build(), PLAYER_COUNT);
        IntColumn group = content.getUnitTypes().getExtensionColumns().get(GROUP);
        long initial = hash(content);

        Assertions.assertEquals(initial, hash(content));

        group.set(this.fort.getIndex(), GROUP_FLAGS + 1);
        Assertions.assertNotEquals(initial, hash(content));
        group.set(this.fort.getIndex(), GROUP_FLAGS);
        Assertions.assertEquals(initial, hash(content));

        content.getProgress().setCompletedUnitCount(0, this.fort.getIndex(), 1);
        Assertions.assertNotEquals(initial, hash(content));
        content.getProgress().setCompletedUnitCount(0, this.fort.getIndex(), 0);
        Assertions.assertEquals(initial, hash(content));

        content.getWeaponTypes().setDamage(this.sword.getIndex(), RAISED_SWORD_DAMAGE);
        Assertions.assertNotEquals(initial, hash(content));
        content.getWeaponTypes().setDamage(this.sword, SWORD_DAMAGE);
        Assertions.assertEquals(initial, hash(content));
    }

    @Test
    void progress_table_holds_unit_counts_and_per_player_extension_columns() {
        this.registry.addPlayerColumn(TableKind.UNIT, DEATHS, column -> column.set(PLAYER_COUNT * this.soldier.getIndex() + 1, 1));
        this.registry.addPlayerColumn(TableKind.UPGRADE, RESEARCHING, column -> {
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> this.registry.addPlayerColumn(TableKind.UNIT, DEATHS, column -> {
        }));

        GameContent content = GameContent.create(this.registry.build(), PLAYER_COUNT);
        PlayerProgressTable progress = content.getProgress();
        IntColumn deaths = progress.getExtensionColumns(TableKind.UNIT).get(DEATHS);
        long initial = hash(content);

        Assertions.assertEquals(PLAYER_COUNT * 2, deaths.getLength());
        Assertions.assertEquals(PLAYER_COUNT, progress.getExtensionColumns(TableKind.UPGRADE).get(RESEARCHING).getLength());
        Assertions.assertEquals(0, progress.getExtensionColumns(TableKind.WEAPON).getCount());
        Assertions.assertEquals(1, deaths.get(PLAYER_COUNT * this.soldier.getIndex() + 1));

        progress.setUnitCount(1, this.fort.getIndex(), 2);
        Assertions.assertEquals(2, progress.getUnitCount(1, this.fort.getIndex()));
        Assertions.assertEquals(0, progress.getCompletedUnitCount(1, this.fort.getIndex()));
        Assertions.assertNotEquals(initial, hash(content));
        progress.setUnitCount(1, this.fort.getIndex(), 0);
        Assertions.assertEquals(initial, hash(content));

        deaths.set(0, 3);
        Assertions.assertNotEquals(initial, hash(content));
        deaths.set(0, 0);
        Assertions.assertEquals(initial, hash(content));
    }

    @Test
    void empty_content_mixes_nothing() {
        GameContent content = GameContent.create(new ContentRegistry().build(), PLAYER_COUNT);

        Assertions.assertEquals(new StateHash().getValue(), hash(content));
    }
}
