package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.*;
import org.fogofwarlabs.sc4j.engine.content.type.*;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.json.JsonObjectReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/**
 * Reads the content definitions file of a standard pack into a {@link PackContent}. The package description gives the file format.
 * <p>
 * The reader creates the identities of every section first, so a definition may name a type that comes later in the file (a faction names its
 * worker, which is a unit type after it).
 */
final class PackContentReader {
    /**
     * The format number of the content file this reader understands.
     */
    static final int FORMAT = 1;

    /**
     * The field names of the content object.
     */
    private static final @NotNull Set<@NotNull String> CONTENT_FIELDS = Set.of("format", "resourceKinds", "motionProfiles", "weapons", "factions",
                                                                               "units", "requirements");
    /**
     * The field names of a motion profile definition.
     */
    private static final @NotNull Set<@NotNull String> MOTION_PROFILE_FIELDS = Set.of("topSpeed", "acceleration", "stoppingDistance", "turnRate",
                                                                                      "acceleratingTurn");
    /**
     * The field names of a weapon definition.
     */
    private static final @NotNull Set<@NotNull String> WEAPON_FIELDS = Set.of("damage", "damageBonus", "damageFactor", "cooldown", "minimumRange",
                                                                              "maximumRange", "targets", "projectile", "homing",
                                                                              "splashInnerRadius", "splashMiddleRadius", "splashOuterRadius");
    /**
     * The field names of a faction definition.
     */
    private static final @NotNull Set<@NotNull String> FACTION_FIELDS = Set.of("worker", "townHall", "supplyProvider", "resourceKinds", "startUnits");
    /**
     * The field names of a unit definition.
     */
    private static final @NotNull Set<@NotNull String> UNIT_FIELDS = Set.of("faction", "motionProfile", "hitPoints", "armor", "sightRange",
                                                                            "targetAcquisitionRange", "groundWeapon", "airWeapon", "cost",
                                                                            "buildTime", "supplyProvided", "supplyRequired", "spaceRequired",
                                                                            "spaceProvided", "placementWidth", "placementHeight", "collision",
                                                                            "traits", "idleOrders");
    /**
     * The field names of a collision box object.
     */
    private static final @NotNull Set<@NotNull String> COLLISION_FIELDS = Set.of("left", "up", "right", "down");
    /**
     * The field names of a requirement condition: exactly one keyword, and {@code count} with {@code completed}.
     */
    private static final @NotNull Set<@NotNull String> CONDITION_FIELDS = Set.of("actor", "completed", "count", "anyOf");
    /**
     * The largest number of fields of a {@code completed} condition: {@code completed} and {@code count}.
     */
    private static final int COMPLETED_CONDITION_FIELD_LIMIT = 2;
    /**
     * The damage factor of a weapon definition without {@code damageFactor}: one hit applies the damage once.
     */
    private static final int DEFAULT_DAMAGE_FACTOR = 1;

    /**
     * Reads a content file.
     *
     * @param contents   the file contents
     * @param location   the file location, for diagnostics
     * @param orderTypes the order types a unit definition may name as its idle order, by their debug names
     * @return the content
     * @throws IOException if the contents are not a content file of the format or name a missing type
     */
    static @NotNull PackContent read(byte @NotNull [] contents, @NotNull String location, @NotNull List<@NotNull OrderType> orderTypes) throws IOException {
        return new PackContentReader(orderTypes).read(JsonObjectReader.parse(contents, location));
    }

    private static void checkFields(@NotNull JsonObjectReader object, @NotNull Set<@NotNull String> fields) throws IOException {
        for (Object name : object.getNames()) {
            if (!fields.contains(String.valueOf(name))) {
                throw Exceptions.newIOException("{0}: unknown field.", object.getLocation(String.valueOf(name)));
            }
        }
    }

    private static <T extends ContentType> @NotNull Map<@NotNull String, @NotNull T> createTypes(@NotNull JsonObjectReader section,
                                                                                                 @NotNull Function<@NotNull String, @NotNull T> factory) throws IOException {
        Map<String, T> types = new LinkedHashMap<>();

        for (Object name : section.getNames()) {
            String id = String.valueOf(name);

            if (id.isEmpty()) {
                throw Exceptions.newIOException("{0}: empty id.", section.getLocation());
            }

            types.put(id, factory.apply(id));
        }

        return Collections.unmodifiableMap(types);
    }

    private static <E extends Enum<E>> @NotNull E parseConstant(@NotNull Class<E> type, @NotNull String name, @NotNull String location) throws IOException {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw Exceptions.newIOException(exception, "{0}: unknown {1} {2}.", location, type.getSimpleName(), name);
        }
    }

    private static <T extends ContentType> @Nullable T findType(@NotNull Map<@NotNull String, @NotNull T> types, @NotNull JsonObjectReader object,
                                                                @NotNull String name) throws IOException {
        String id = object.findText(name);

        return id == null ? null : requireType(types, id, object.getLocation(name));
    }

    private static <T> @NotNull T requireType(@NotNull Map<@NotNull String, @NotNull T> types, @NotNull String id, @NotNull String location) throws IOException {
        T type = types.get(id);

        if (type == null) {
            throw Exceptions.newIOException("{0}: unknown id {1}.", location, id);
        }

        return type;
    }


    private final @NotNull Map<@NotNull String, @NotNull OrderType> orderTypes;

    private @NotNull Map<@NotNull String, @NotNull ResourceKind> resourceKinds;
    private @NotNull Map<@NotNull String, @NotNull MotionProfile> motionProfiles;
    private @NotNull Map<@NotNull String, @NotNull WeaponType> weaponTypes;
    private @NotNull Map<@NotNull String, @NotNull Faction> factions;
    private @NotNull Map<@NotNull String, @NotNull UnitType> unitTypes;

    private PackContentReader(@NotNull List<@NotNull OrderType> orderTypes) {
        Map<String, OrderType> orders = new LinkedHashMap<>();

        for (OrderType order : orderTypes) {
            orders.put(order.getDebugName(), order);
        }

        this.orderTypes = orders;

        this.resourceKinds  = Map.of();
        this.motionProfiles = Map.of();
        this.weaponTypes    = Map.of();
        this.factions       = Map.of();
        this.unitTypes      = Map.of();
    }

    private @NotNull PackContent read(@NotNull JsonObjectReader content) throws IOException {
        int format = content.getInt("format");

        if (format != FORMAT) {
            throw Exceptions.newIOException("{0}: unsupported content format {1}; this engine reads format {2}.", content.getLocation(), format,
                                            FORMAT);
        }

        checkFields(content, CONTENT_FIELDS);

        Map<String, ResourceKind> kinds = new LinkedHashMap<>();

        for (String id : content.getTextList("resourceKinds")) {
            if (id.isEmpty() || kinds.put(id, new ResourceKind(id)) != null) {
                throw Exceptions.newIOException("{0}: the resource kind {1} is empty or listed twice.", content.getLocation("resourceKinds"), id);
            }
        }

        JsonObjectReader motionProfileSection = readSection(content, "motionProfiles");
        JsonObjectReader weapons = readSection(content, "weapons");
        JsonObjectReader factionSection = readSection(content, "factions");
        JsonObjectReader units = readSection(content, "units");

        this.resourceKinds  = Collections.unmodifiableMap(kinds);
        this.motionProfiles = createTypes(motionProfileSection, MotionProfile::new);
        this.weaponTypes    = createTypes(weapons, WeaponType::new);
        this.factions       = createTypes(factionSection, Faction::new);
        this.unitTypes      = createTypes(units, UnitType::new);

        List<MotionProfileDefinition> motionProfileDefinitions = new ArrayList<>();
        List<WeaponDefinition> weaponDefinitions = new ArrayList<>();
        List<FactionDefinition> factionDefinitions = new ArrayList<>();
        List<UnitDefinition> unitDefinitions = new ArrayList<>();

        for (String id : this.motionProfiles.keySet()) {
            motionProfileDefinitions.add(this.readMotionProfile(motionProfileSection.getObject(id)));
        }

        for (String id : this.weaponTypes.keySet()) {
            weaponDefinitions.add(this.readWeapon(weapons.getObject(id)));
        }

        for (String id : this.factions.keySet()) {
            factionDefinitions.add(this.readFaction(factionSection.getObject(id)));
        }

        for (String id : this.unitTypes.keySet()) {
            unitDefinitions.add(this.readUnit(units.getObject(id)));
        }

        return new PackContent(this.resourceKinds, this.motionProfiles, this.weaponTypes, this.factions, this.unitTypes,
                               motionProfileDefinitions, weaponDefinitions, factionDefinitions, unitDefinitions,
                               this.readRequirements(readSection(content, "requirements")));
    }

    private static @NotNull JsonObjectReader readSection(@NotNull JsonObjectReader content, @NotNull String name) throws IOException {
        return content.has(name) ? content.getObject(name) : JsonObjectReader.of(Map.of(), content.getLocation(name));
    }

    private @NotNull MotionProfileDefinition readMotionProfile(@NotNull JsonObjectReader motionProfile) throws IOException {
        checkFields(motionProfile, MOTION_PROFILE_FIELDS);

        return new MotionProfileDefinition.Builder().setTopSpeed(motionProfile.getInt("topSpeed", 0))
                                                    .setAcceleration(motionProfile.getInt("acceleration", 0))
                                                    .setStoppingDistance(motionProfile.getInt("stoppingDistance", 0))
                                                    .setTurnRate(motionProfile.getInt("turnRate", 0))
                                                    .setAcceleratingTurn(motionProfile.getBoolean("acceleratingTurn", false))
                                                    .build();
    }

    private @NotNull WeaponDefinition readWeapon(@NotNull JsonObjectReader weapon) throws IOException {
        checkFields(weapon, WEAPON_FIELDS);

        WeaponDefinition.Builder builder = new WeaponDefinition.Builder().setDamage(weapon.getInt("damage", 0))
                                                                         .setDamageBonus(weapon.getInt("damageBonus", 0))
                                                                         .setDamageFactor(weapon.getInt("damageFactor", DEFAULT_DAMAGE_FACTOR))
                                                                         .setCooldown(weapon.getInt("cooldown", 0))
                                                                         .setMinimumRange(weapon.getInt("minimumRange", 0))
                                                                         .setMaximumRange(weapon.getInt("maximumRange", 0))
                                                                         .setProjectile(findType(this.motionProfiles, weapon, "projectile"))
                                                                         .setHoming(weapon.getBoolean("homing", true))
                                                                         .setSplashInnerRadius(weapon.getInt("splashInnerRadius", 0))
                                                                         .setSplashMiddleRadius(weapon.getInt("splashMiddleRadius", 0))
                                                                         .setSplashOuterRadius(weapon.getInt("splashOuterRadius", 0));
        List<String> targets = weapon.getTextList("targets");

        for (int index = 0; index < targets.size(); index++) {
            builder.addTarget(parseConstant(WeaponTarget.class, targets.get(index), weapon.getLocation("targets") + "[" + index + "]"));
        }

        return builder.build();
    }

    private @NotNull FactionDefinition readFaction(@NotNull JsonObjectReader faction) throws IOException {
        checkFields(faction, FACTION_FIELDS);

        FactionDefinition.Builder builder = new FactionDefinition.Builder().setWorker(findType(this.unitTypes, faction, "worker"))
                                                                           .setTownHall(findType(this.unitTypes, faction, "townHall"))
                                                                           .setSupplyProvider(findType(this.unitTypes, faction, "supplyProvider"));
        List<String> kinds = faction.getTextList("resourceKinds");
        List<String> startUnits = faction.getTextList("startUnits");

        for (int index = 0; index < kinds.size(); index++) {
            builder.addResourceKind(requireType(this.resourceKinds, kinds.get(index), faction.getLocation("resourceKinds") + "[" + index + "]"));
        }

        for (int index = 0; index < startUnits.size(); index++) {
            builder.addStartUnit(requireType(this.unitTypes, startUnits.get(index), faction.getLocation("startUnits") + "[" + index + "]"));
        }

        return builder.build();
    }

    private @NotNull UnitDefinition readUnit(@NotNull JsonObjectReader unit) throws IOException {
        checkFields(unit, UNIT_FIELDS);

        UnitDefinition.Builder builder = new UnitDefinition.Builder().setFaction(findType(this.factions, unit, "faction"))
                                                                     .setMotionProfile(findType(this.motionProfiles, unit, "motionProfile"))
                                                                     .setHitPoints(unit.getInt("hitPoints", 0))
                                                                     .setArmor(unit.getInt("armor", 0))
                                                                     .setSightRange(unit.getInt("sightRange", 0))
                                                                     .setTargetAcquisitionRange(unit.getInt("targetAcquisitionRange", 0))
                                                                     .setGroundWeapon(findType(this.weaponTypes, unit, "groundWeapon"))
                                                                     .setAirWeapon(findType(this.weaponTypes, unit, "airWeapon"))
                                                                     .setCost(this.readCost(unit))
                                                                     .setBuildTime(unit.getInt("buildTime", 0))
                                                                     .setSupplyProvided(unit.getInt("supplyProvided", 0))
                                                                     .setSupplyRequired(unit.getInt("supplyRequired", 0))
                                                                     .setSpaceRequired(unit.getInt("spaceRequired", 0))
                                                                     .setSpaceProvided(unit.getInt("spaceProvided", 0))
                                                                     .setPlacementWidth(unit.getInt("placementWidth", 0))
                                                                     .setPlacementHeight(unit.getInt("placementHeight", 0));
        List<String> traits = unit.getTextList("traits");

        readCollision(unit, builder);

        for (int index = 0; index < traits.size(); index++) {
            builder.addTrait(parseConstant(UnitTrait.class, traits.get(index), unit.getLocation("traits") + "[" + index + "]"));
        }

        if (unit.has("idleOrders")) {
            JsonObjectReader idleOrders = unit.getObject("idleOrders");

            for (Object name : idleOrders.getNames()) {
                String kind = String.valueOf(name);

                builder.setIdleOrder(parseConstant(ControllerKind.class, kind, idleOrders.getLocation(kind)),
                                     requireType(this.orderTypes, idleOrders.getText(kind), idleOrders.getLocation(kind)));
            }
        }

        return builder.build();
    }

    private @NotNull ResourceCost readCost(@NotNull JsonObjectReader unit) throws IOException {
        if (!unit.has("cost")) {
            return ResourceCost.NONE;
        }

        JsonObjectReader cost = unit.getObject("cost");
        ResourceCost.Builder builder = new ResourceCost.Builder();

        for (Object name : cost.getNames()) {
            String kind = String.valueOf(name);

            builder.add(requireType(this.resourceKinds, kind, cost.getLocation(kind)), cost.getInt(kind));
        }

        return builder.build();
    }

    private static void readCollision(@NotNull JsonObjectReader unit, UnitDefinition.@NotNull Builder builder) throws IOException {
        Object value = unit.get("collision");

        if (value == null) {
            return;
        }

        if (value instanceof Long) {
            int extent = unit.getInt("collision");

            builder.setCollisionLeft(extent).setCollisionUp(extent).setCollisionRight(extent).setCollisionDown(extent);

            return;
        }

        JsonObjectReader collision = unit.getObject("collision");

        checkFields(collision, COLLISION_FIELDS);
        builder.setCollisionLeft(collision.getInt("left"))
               .setCollisionUp(collision.getInt("up"))
               .setCollisionRight(collision.getInt("right"))
               .setCollisionDown(collision.getInt("down"));
    }

    private @NotNull DeclarativeRequirements readRequirements(@NotNull JsonObjectReader requirements) throws IOException {
        DeclarativeRequirements.Builder builder = new DeclarativeRequirements.Builder();

        for (Object name : requirements.getNames()) {
            String id = String.valueOf(name);
            UnitType type = requireType(this.unitTypes, id, requirements.getLocation(id));

            builder.requireBuild(type, Requirement.allOf(this.readConditions(requirements.getArray(id), requirements.getLocation(id))));
        }

        return builder.build();
    }

    private @NotNull RequirementCondition @NotNull [] readConditions(@NotNull List<?> elements, @NotNull String location) throws IOException {
        RequirementCondition[] conditions = new RequirementCondition[elements.size()];

        for (int index = 0; index < conditions.length; index++) {
            conditions[index] = this.readCondition(JsonObjectReader.of(elements.get(index), location + "[" + index + "]"));
        }

        return conditions;
    }

    private @NotNull RequirementCondition readCondition(@NotNull JsonObjectReader condition) throws IOException {
        checkFields(condition, CONDITION_FIELDS);

        if (condition.has("actor") && condition.getNames().size() == 1) {
            List<String> ids = condition.get("actor") instanceof String id ? List.of(id) : condition.getTextList("actor");
            UnitType[] actors = new UnitType[ids.size()];

            for (int index = 0; index < actors.length; index++) {
                actors[index] = requireType(this.unitTypes, ids.get(index), condition.getLocation("actor"));
            }

            return new ActorTypeCondition(actors);
        }

        if (condition.has("completed") && condition.getNames().size() <= COMPLETED_CONDITION_FIELD_LIMIT &&
            (condition.getNames().size() == 1 || condition.has("count"))) {
            UnitType type = requireType(this.unitTypes, condition.getText("completed"), condition.getLocation("completed"));

            return new CompletedUnitCondition(type, condition.getInt("count", 1));
        }

        if (condition.has("anyOf") && condition.getNames().size() == 1) {
            return new AnyOfCondition(this.readConditions(condition.getArray("anyOf"), condition.getLocation("anyOf")));
        }

        throw Exceptions.newIOException("{0}: expected one of actor, completed (with an optional count) or anyOf.", condition.getLocation());
    }
}
