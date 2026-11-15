package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponDefinition;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.DeclarativeRequirements;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Holds the content definitions of a standard pack ({@link StandardPack#readContent}): the resource kinds, motion profiles, weapon types, factions
 * and unit types with their definitions, each named by its pack id (also its debug name, which maps name unit types and resource kinds by),
 * and the declarative requirements of the unit types. The types are new identities, not yet registered; {@link #register} registers them
 * with their definitions in a content registry.
 */
public final class PackContent {
    private static <T extends ContentType> @NotNull T requireType(@NotNull Map<@NotNull String, @NotNull T> types, @NotNull String kind,
                                                                  @NotNull String id) {
        T type = types.get(id);

        if (type == null) {
            throw Exceptions.newIllegalArgumentException("The pack content has no {0} {1}.", kind, id);
        }

        return type;
    }


    private final @NotNull Map<@NotNull String, @NotNull ResourceKind> resourceKinds;
    private final @NotNull Map<@NotNull String, @NotNull MotionProfile> motionProfiles;
    private final @NotNull Map<@NotNull String, @NotNull WeaponType> weaponTypes;
    private final @NotNull Map<@NotNull String, @NotNull Faction> factions;
    private final @NotNull Map<@NotNull String, @NotNull UnitType> unitTypes;

    private final @NotNull List<@NotNull MotionProfileDefinition> motionProfileDefinitions;
    private final @NotNull List<@NotNull WeaponDefinition> weaponDefinitions;
    private final @NotNull List<@NotNull FactionDefinition> factionDefinitions;
    private final @NotNull List<@NotNull UnitDefinition> unitDefinitions;

    private final @NotNull DeclarativeRequirements requirements;

    /**
     * Creates a {@link PackContent} instance from read types and definitions; the definitions are in the order of their types.
     *
     * @param resourceKinds            the resource kinds by id, in file order
     * @param motionProfiles           the motion profiles by id, in file order
     * @param weaponTypes              the weapon types by id, in file order
     * @param factions                 the factions by id, in file order
     * @param unitTypes                the unit types by id, in file order
     * @param motionProfileDefinitions the definitions of the motion profiles
     * @param weaponDefinitions        the definitions of the weapon types
     * @param factionDefinitions       the definitions of the factions
     * @param unitDefinitions          the definitions of the unit types
     * @param requirements             the declarative requirements of the unit types
     */
    PackContent(@NotNull Map<@NotNull String, @NotNull ResourceKind> resourceKinds,
                @NotNull Map<@NotNull String, @NotNull MotionProfile> motionProfiles, @NotNull Map<@NotNull String, @NotNull WeaponType> weaponTypes,
                @NotNull Map<@NotNull String, @NotNull Faction> factions, @NotNull Map<@NotNull String, @NotNull UnitType> unitTypes,
                @NotNull List<@NotNull MotionProfileDefinition> motionProfileDefinitions, @NotNull List<@NotNull WeaponDefinition> weaponDefinitions,
                @NotNull List<@NotNull FactionDefinition> factionDefinitions, @NotNull List<@NotNull UnitDefinition> unitDefinitions,
                @NotNull DeclarativeRequirements requirements) {
        this.resourceKinds  = resourceKinds;
        this.motionProfiles = motionProfiles;
        this.weaponTypes    = weaponTypes;
        this.factions       = factions;
        this.unitTypes      = unitTypes;

        this.motionProfileDefinitions = List.copyOf(motionProfileDefinitions);
        this.weaponDefinitions        = List.copyOf(weaponDefinitions);
        this.factionDefinitions       = List.copyOf(factionDefinitions);
        this.unitDefinitions          = List.copyOf(unitDefinitions);

        this.requirements = requirements;
    }

    /**
     * @return the resource kinds in file order
     */
    public @NotNull List<@NotNull ResourceKind> getResourceKinds() {
        return List.copyOf(this.resourceKinds.values());
    }

    /**
     * @return the factions in file order
     */
    public @NotNull List<@NotNull Faction> getFactions() {
        return List.copyOf(this.factions.values());
    }

    /**
     * @return the unit types in file order
     */
    public @NotNull List<@NotNull UnitType> getUnitTypes() {
        return List.copyOf(this.unitTypes.values());
    }

    /**
     * @return the declarative requirements of the unit types, for the engine default requirement system
     */
    public @NotNull DeclarativeRequirements getRequirements() {
        return this.requirements;
    }


    /**
     * Returns a resource kind of the content.
     *
     * @param id the pack id
     * @return the resource kind
     * @throws IllegalArgumentException if the content has no resource kind of the id
     */
    public @NotNull ResourceKind requireResourceKind(@NotNull String id) {
        return requireType(this.resourceKinds, "resource kind", id);
    }

    /**
     * Returns a faction of the content.
     *
     * @param id the pack id
     * @return the faction
     * @throws IllegalArgumentException if the content has no faction of the id
     */
    public @NotNull Faction requireFaction(@NotNull String id) {
        return requireType(this.factions, "faction", id);
    }

    /**
     * Returns a unit type of the content.
     *
     * @param id the pack id
     * @return the unit type
     * @throws IllegalArgumentException if the content has no unit type of the id
     */
    public @NotNull UnitType requireUnitType(@NotNull String id) {
        return requireType(this.unitTypes, "unit type", id);
    }

    /**
     * Registers every type of the content with its definition, in this order: the resource kinds, motion profiles, weapon types, factions and unit
     * types, each in file order. The order types the content names must be registered in the same registry, before or after.
     *
     * @param content the content registry of the mod's games
     * @throws IllegalStateException if the registry is frozen or a type is already registered
     */
    public void register(@NotNull ContentRegistry content) {
        for (ResourceKind kind : this.resourceKinds.values()) {
            content.getResourceKinds().register(kind);
        }

        int index = 0;

        for (MotionProfile type : this.motionProfiles.values()) {
            content.getMotionProfiles().register(type, this.motionProfileDefinitions.get(index++));
        }

        index = 0;

        for (WeaponType type : this.weaponTypes.values()) {
            content.getWeaponTypes().register(type, this.weaponDefinitions.get(index++));
        }

        index = 0;

        for (Faction faction : this.factions.values()) {
            content.getFactions().register(faction, this.factionDefinitions.get(index++));
        }

        index = 0;

        for (UnitType type : this.unitTypes.values()) {
            content.getUnitTypes().register(type, this.unitDefinitions.get(index++));
        }
    }
}
