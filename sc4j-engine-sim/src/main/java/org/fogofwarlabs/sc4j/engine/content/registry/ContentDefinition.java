package org.fogofwarlabs.sc4j.engine.content.registry;

import org.fogofwarlabs.sc4j.engine.content.combat.DamageKind;
import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.modifier.Stat;
import org.fogofwarlabs.sc4j.engine.content.modifier.StatModifier;
import org.fogofwarlabs.sc4j.engine.content.type.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Holds the frozen content of a mod: every registry in registration order, the extension columns of each type table
 * and the modifiers of each stat. Every game of a session creates its per-game tables from the same content definition;
 * the definition never changes.
 */
public final class ContentDefinition {
    private final @NotNull DefinitionRegistry<UnitType, UnitDefinition> unitTypes;
    private final @NotNull DefinitionRegistry<WeaponType, WeaponDefinition> weaponTypes;
    private final @NotNull DefinitionRegistry<MotionProfile, MotionProfileDefinition> motionProfiles;
    private final @NotNull DefinitionRegistry<SpriteType, SpriteDefinition> spriteTypes;
    private final @NotNull DefinitionRegistry<ImageType, ImageDefinition> imageTypes;
    private final @NotNull DefinitionRegistry<UpgradeType, UpgradeDefinition> upgradeTypes;
    private final @NotNull DefinitionRegistry<TechType, TechDefinition> techTypes;
    private final @NotNull DefinitionRegistry<OrderType, OrderDefinition> orderTypes;
    private final @NotNull DefinitionRegistry<SoundType, SoundDefinition> soundTypes;
    private final @NotNull DefinitionRegistry<Faction, FactionDefinition> factions;
    private final @NotNull TypeRegistry<ResourceKind> resourceKinds;
    private final @NotNull TypeRegistry<DamageKind> damageKinds;
    private final @NotNull TypeRegistry<Stat> stats;

    private final @NotNull List<@NotNull List<@NotNull TypeColumn<?>>> columns;
    private final @NotNull List<@NotNull List<@NotNull TypeColumn<?>>> playerColumns;
    private final @NotNull StatModifier @NotNull [] @NotNull [] modifiers;
    private final int playerCount;

    /**
     * Creates a {@link ContentDefinition} instance over frozen registries.
     *
     * @param unitTypes      the unit types
     * @param weaponTypes    the weapon types
     * @param motionProfiles the motion profiles
     * @param spriteTypes    the sprite types
     * @param imageTypes     the image types
     * @param upgradeTypes   the upgrade types
     * @param techTypes      the tech types
     * @param orderTypes     the order types
     * @param soundTypes     the sound types
     * @param factions       the factions
     * @param resourceKinds  the resource kinds
     * @param damageKinds    the damage kinds
     * @param stats          the stats
     * @param columns        the extension columns of each {@link TableKind}, by its ordinal
     * @param playerColumns  the per-player extension columns of each {@link TableKind}, by its ordinal
     * @param modifiers      the modifiers of each stat, by the stat index
     * @param playerCount    the player count, or {@link ContentRegistry#PLAYER_COUNT_FROM_SETUP}
     */
    ContentDefinition(@NotNull DefinitionRegistry<UnitType, UnitDefinition> unitTypes,
                      @NotNull DefinitionRegistry<WeaponType, WeaponDefinition> weaponTypes,
                      @NotNull DefinitionRegistry<MotionProfile, MotionProfileDefinition> motionProfiles,
                      @NotNull DefinitionRegistry<SpriteType, SpriteDefinition> spriteTypes,
                      @NotNull DefinitionRegistry<ImageType, ImageDefinition> imageTypes,
                      @NotNull DefinitionRegistry<UpgradeType, UpgradeDefinition> upgradeTypes,
                      @NotNull DefinitionRegistry<TechType, TechDefinition> techTypes,
                      @NotNull DefinitionRegistry<OrderType, OrderDefinition> orderTypes,
                      @NotNull DefinitionRegistry<SoundType, SoundDefinition> soundTypes,
                      @NotNull DefinitionRegistry<Faction, FactionDefinition> factions,
                      @NotNull TypeRegistry<ResourceKind> resourceKinds,
                      @NotNull TypeRegistry<DamageKind> damageKinds,
                      @NotNull TypeRegistry<Stat> stats,
                      @NotNull List<@NotNull List<@NotNull TypeColumn<?>>> columns,
                      @NotNull List<@NotNull List<@NotNull TypeColumn<?>>> playerColumns,
                      @NotNull StatModifier @NotNull [] @NotNull [] modifiers,
                      int playerCount) {
        this.unitTypes      = unitTypes;
        this.weaponTypes    = weaponTypes;
        this.motionProfiles = motionProfiles;
        this.spriteTypes    = spriteTypes;
        this.imageTypes     = imageTypes;
        this.upgradeTypes   = upgradeTypes;
        this.techTypes      = techTypes;
        this.orderTypes     = orderTypes;
        this.soundTypes     = soundTypes;
        this.factions       = factions;
        this.resourceKinds  = resourceKinds;
        this.damageKinds    = damageKinds;
        this.stats          = stats;

        this.columns       = columns;
        this.playerColumns = playerColumns;
        this.modifiers     = modifiers;
        this.playerCount   = playerCount;
    }

    /**
     * @return the unit types and their definitions
     */
    public @NotNull DefinitionRegistry<UnitType, UnitDefinition> getUnitTypes() {
        return this.unitTypes;
    }

    /**
     * @return the weapon types and their definitions
     */
    public @NotNull DefinitionRegistry<WeaponType, WeaponDefinition> getWeaponTypes() {
        return this.weaponTypes;
    }

    /**
     * @return the motion profiles and their definitions
     */
    public @NotNull DefinitionRegistry<MotionProfile, MotionProfileDefinition> getMotionProfiles() {
        return this.motionProfiles;
    }

    /**
     * @return the sprite types and their definitions
     */
    public @NotNull DefinitionRegistry<SpriteType, SpriteDefinition> getSpriteTypes() {
        return this.spriteTypes;
    }

    /**
     * @return the image types and their definitions
     */
    public @NotNull DefinitionRegistry<ImageType, ImageDefinition> getImageTypes() {
        return this.imageTypes;
    }

    /**
     * @return the upgrade types and their definitions
     */
    public @NotNull DefinitionRegistry<UpgradeType, UpgradeDefinition> getUpgradeTypes() {
        return this.upgradeTypes;
    }

    /**
     * @return the tech types and their definitions
     */
    public @NotNull DefinitionRegistry<TechType, TechDefinition> getTechTypes() {
        return this.techTypes;
    }

    /**
     * @return the order types and their definitions
     */
    public @NotNull DefinitionRegistry<OrderType, OrderDefinition> getOrderTypes() {
        return this.orderTypes;
    }

    /**
     * @return the sound types and their definitions
     */
    public @NotNull DefinitionRegistry<SoundType, SoundDefinition> getSoundTypes() {
        return this.soundTypes;
    }

    /**
     * @return the factions and their definitions, the same in every game
     */
    public @NotNull DefinitionRegistry<Faction, FactionDefinition> getFactions() {
        return this.factions;
    }

    /**
     * @return the resource kinds
     */
    public @NotNull TypeRegistry<ResourceKind> getResourceKinds() {
        return this.resourceKinds;
    }

    /**
     * @return the damage kinds of the weapon types
     */
    public @NotNull TypeRegistry<DamageKind> getDamageKinds() {
        return this.damageKinds;
    }

    /**
     * @return the stats, the engine stats first
     */
    public @NotNull TypeRegistry<Stat> getStats() {
        return this.stats;
    }

    /**
     * @return the number of players of the per-player tables, or {@link ContentRegistry#PLAYER_COUNT_FROM_SETUP}
     */
    public int getPlayerCount() {
        return this.playerCount;
    }

    /**
     * Returns the extension columns of a type table.
     *
     * @param table the type table
     * @return the columns in the order they were added
     */
    public @NotNull List<@NotNull TypeColumn<?>> getColumns(@NotNull TableKind table) {
        return this.columns.get(table.ordinal());
    }

    /**
     * Returns the per-player extension columns of the player progress table for the types of a table.
     *
     * @param table the type table whose types index the columns
     * @return the columns in the order they were added
     */
    public @NotNull List<@NotNull TypeColumn<?>> getPlayerColumns(@NotNull TableKind table) {
        return this.playerColumns.get(table.ordinal());
    }

    /**
     * Returns the modifiers of a stat.
     *
     * @param statIndex the index of the stat
     * @return the modifiers in the order they were added, a copy
     */
    public @NotNull StatModifier @NotNull [] getModifiers(int statIndex) {
        return this.modifiers[statIndex].clone();
    }
}
