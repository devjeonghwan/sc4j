package org.fogofwarlabs.sc4j.engine.content.modifier;

import org.fogofwarlabs.sc4j.engine.content.definition.TechDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UpgradeDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.TechType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine stat modifiers and their application in registration order.
 */
final class StatModifierTest {
    private static final @NotNull Player OWNER = new Player(1);
    private static final @NotNull Player OTHER = new Player(0);
    private static final int BASE_ARMOR = 1;
    private static final int BASE_DAMAGE = 6;
    private static final int DAMAGE_BONUS = 2;
    private static final int SIGHT_BONUS = 64;
    private static final int DOUBLING = 2;
    private static final int MAXIMUM_LEVEL = 3;
    private static final int BLADES_LEVEL = 3;
    private static final int SPEED_BONUS = 5;
    private static final int HIT_POINT_BONUS = 10;

    private final @NotNull UpgradeType plating;
    private final @NotNull UpgradeType blades;
    private final @NotNull TechType optics;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType scout;
    private final @NotNull WeaponType sword;
    private final @NotNull ContentRegistry registry;

    StatModifierTest() {
        this.plating  = new UpgradeType("plating");
        this.blades   = new UpgradeType("blades");
        this.optics   = new TechType("optics");
        this.soldier  = new UnitType("soldier");
        this.scout    = new UnitType("scout");
        this.sword    = new WeaponType("sword");
        this.registry = new ContentRegistry();

        this.registry.getUpgradeTypes().register(this.plating, new UpgradeDefinition.Builder().setMaximumLevel(MAXIMUM_LEVEL).build());
        this.registry.getUpgradeTypes().register(this.blades, new UpgradeDefinition.Builder().setMaximumLevel(MAXIMUM_LEVEL).build());
        this.registry.getTechTypes().register(this.optics, new TechDefinition.Builder().build());
        this.registry.getWeaponTypes().register(this.sword, new WeaponDefinition.Builder().setDamage(BASE_DAMAGE).setDamageBonus(DAMAGE_BONUS)
                                                                                          .setUpgrade(this.blades).build());
        this.registry.getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setArmor(BASE_ARMOR).setArmorUpgrade(this.plating)
                                                                                        .setGroundWeapon(this.sword).build());
        this.registry.getUnitTypes().register(this.scout, new UnitDefinition.Builder().build());
    }

    @Test
    void upgrade_modifiers_read_the_levels_of_the_owner() {
        this.registry.addModifier(Stat.ARMOR, new ArmorUpgradeModifier(1));
        this.registry.addModifier(Stat.WEAPON_DAMAGE, new WeaponDamageUpgradeModifier());

        GameContent content = GameContent.create(this.registry.build(), 2);

        content.getProgress().setUpgradeLevel(OWNER, this.plating, 2);
        content.getProgress().setUpgradeLevel(OWNER, this.blades, BLADES_LEVEL);

        Assertions.assertEquals(BASE_ARMOR + 2, content.applyModifiers(Stat.ARMOR, OWNER, this.soldier, null, BASE_ARMOR));
        Assertions.assertEquals(BASE_ARMOR, content.applyModifiers(Stat.ARMOR, OTHER, this.soldier, null, BASE_ARMOR));
        Assertions.assertEquals(BASE_DAMAGE + BLADES_LEVEL * DAMAGE_BONUS,
                                content.applyModifiers(Stat.WEAPON_DAMAGE, OWNER, this.soldier, this.sword, BASE_DAMAGE));
        Assertions.assertEquals(0, content.applyModifiers(Stat.ARMOR, OWNER, this.scout, null, 0));
    }

    @Test
    void modifiers_of_a_stat_apply_in_registration_order() {
        this.registry.addModifier(Stat.SIGHT_RANGE, new TechResearchedModifier(this.optics, SIGHT_BONUS, this.scout));
        this.registry.addModifier(Stat.SIGHT_RANGE, (content, player, unitType, weaponType, value) -> value * DOUBLING);

        GameContent content = GameContent.create(this.registry.build(), 2);

        Assertions.assertEquals(2 * DOUBLING, content.applyModifiers(Stat.SIGHT_RANGE, OWNER, this.scout, null, 2));

        content.getProgress().setTechResearched(OWNER, this.optics, true);

        Assertions.assertEquals((2 + SIGHT_BONUS) * DOUBLING, content.applyModifiers(Stat.SIGHT_RANGE, OWNER, this.scout, null, 2));
        Assertions.assertEquals(2 * DOUBLING, content.applyModifiers(Stat.SIGHT_RANGE, OWNER, this.soldier, null, 2));
    }

    @Test
    void game_modifiers_apply_after_the_definition_modifiers_until_sealed() {
        this.registry.addModifier(Stat.SIGHT_RANGE, (content, player, unitType, weaponType, value) -> value + SIGHT_BONUS);

        ContentDefinition definition = this.registry.build();
        GameContent content = GameContent.create(definition, 2);

        content.addModifier(Stat.SIGHT_RANGE, (game, player, unitType, weaponType, value) -> value * DOUBLING);

        Assertions.assertEquals((2 + SIGHT_BONUS) * DOUBLING, content.applyModifiers(Stat.SIGHT_RANGE, OWNER, this.scout, null, 2));

        content.seal();

        Assertions.assertThrows(IllegalStateException.class,
                                () -> content.addModifier(Stat.SIGHT_RANGE, (game, player, unitType, weaponType, value) -> value));
        Assertions.assertEquals(2 + SIGHT_BONUS,
                                GameContent.create(definition, 2).applyModifiers(Stat.SIGHT_RANGE, OWNER, this.scout, null, 2));
    }

    @Test
    void upgrade_level_modifier_applies_to_selected_unit_types() {
        this.registry.addModifier(Stat.TOP_SPEED, new UpgradeLevelModifier(this.plating, SPEED_BONUS, this.scout));
        this.registry.addModifier(Stat.HIT_POINTS, new UpgradeLevelModifier(this.plating, HIT_POINT_BONUS));

        GameContent content = GameContent.create(this.registry.build(), 2);

        content.getProgress().setUpgradeLevel(OWNER.getIndex(), this.plating.getIndex(), 1);

        Assertions.assertEquals(SPEED_BONUS, content.applyModifiers(Stat.TOP_SPEED, OWNER, this.scout, null, 0));
        Assertions.assertEquals(0, content.applyModifiers(Stat.TOP_SPEED, OWNER, this.soldier, null, 0));
        Assertions.assertEquals(HIT_POINT_BONUS, content.applyModifiers(Stat.HIT_POINTS, OWNER, this.soldier, null, 0));
    }
}
