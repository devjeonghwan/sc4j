package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.json.JsonObjectReader;
import org.fogofwarlabs.sc4j.engine.terrain.ArrayTerrainGrid;
import org.fogofwarlabs.sc4j.engine.trigger.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reads maps of the engine map format, a JSON file, into the engine map model with {@link StandardMapData} as its map data: the engine default
 * {@link MapLoader}. A mod creates it over its content, or over the types it is about to register, whose unit types and resource kinds the map
 * names by their debug names.
 * {@snippet :
 * MapModel map = new JsonMapLoader(definition.getContent()).load(file);
 *}
 * <p>
 * The file is one object (format {@value #FORMAT}, version {@value #VERSION}); every field name outside this list is an error:
 * <ul>
 *     <li>{@code format}, {@code version}: the format name and version.</li>
 *     <li>{@code name}, {@code tileset}: the map name and the name of its tileset graphics.</li>
 *     <li>{@code tileSize}, {@code walkCellSize}: pixels; the walk cell size divides the tile size.</li>
 *     <li>{@code width}, {@code height}: the map size in tiles.</li>
 *     <li>{@code terrain}: {@code legend}, an object from one-character tile codes to {@code walkable} (default {@code true}),
 *     {@code buildable} (default the walkable value) and {@code elevation} ({@code 0} up to {@value ArrayTerrainGrid#MAXIMUM_ELEVATION}, default
 *     {@code 0}); and {@code rows}, {@code height} strings of {@code width} codes, top row first. Every walk cell of a tile gets the tile's
 *     walkable flag and elevation; the codes are kept as the map data's {@link StandardMapData#getTileCodes()}, which a {@link TilesetLegend}
 *     gives pictures.</li>
 *     <li>{@code neutralSlot}: the player slot that owns the resources.</li>
 *     <li>{@code startLocations}: objects with {@code slot}, {@code x}, {@code y}.</li>
 *     <li>{@code units}: preplaced units, objects with {@code type}, {@code slot}, {@code x}, {@code y}.</li>
 *     <li>{@code resources}: resource containers, objects with {@code type}, {@code kind}, {@code amount}, {@code x}, {@code y}.</li>
 *     <li>{@code locations}: objects with {@code name}, {@code left}, {@code top}, {@code right}, {@code bottom} (pixels, inclusive).</li>
 *     <li>{@code victory}: {@code "annihilation"}, {@code "elimination"} or {@code "none"} (default): the triggers of
 *     {@link StandardTriggerUtils} that come before the map's own.</li>
 *     <li>{@code triggers}: objects with {@code players} ({@code "all"} or an array of slots), {@code repeating} (default {@code false}),
 *     {@code conditions} and {@code actions}. A condition or action names its {@code type} (the standard type names below) and its operands:
 *     {@code player} and {@code targetPlayer} ({@code "current"}, the default, or a slot), {@code unitType}, {@code unitTrait} (a
 *     {@link UnitTrait} name in lower case with hyphens), {@code location} (a location name), {@code resourceKind}, {@code comparison}
 *     ({@code "at-least"}, the default, {@code "at-most"} or {@code "exactly"}), {@code amount}, {@code timer} and {@code text}.</li>
 * </ul>
 * Condition type names: {@code always}, {@code never}, {@code elapsed-frames}, {@code unit-count}, {@code opponents-defeated}, {@code timer},
 * {@code resource-amount}. Action type names: {@code victory}, {@code defeat}, {@code display-message}, {@code set-timer}, {@code wait},
 * {@code repeat-trigger}, {@code create-units}, {@code remove-units}, {@code give-units}. Positions are map pixels and must lie on the map.
 * <p>
 * Every violation of the format is an {@link IOException} whose message names the file and the field path.
 */
public final class JsonMapLoader implements MapLoader {
    /**
     * The format name of the engine map format.
     */
    public static final @NotNull String FORMAT = "sc4j-map";
    /**
     * The version of the engine map format this loader reads.
     */
    public static final int VERSION = 1;

    /**
     * The field names a map object may have.
     */
    private static final @NotNull Set<@NotNull String> MAP_FIELDS = Set.of("format", "version", "name", "tileset", "tileSize", "walkCellSize",
                                                                           "width", "height", "terrain", "neutralSlot", "startLocations", "units",
                                                                           "resources", "locations", "victory", "triggers");
    /**
     * The value of {@code players} that names every player that is not neutral.
     */
    private static final @NotNull String ALL_PLAYERS = "all";
    /**
     * The value of a player operand that names the player whose triggers run.
     */
    private static final @NotNull String CURRENT_PLAYER = "current";
    /**
     * The condition type names, at the index of their type id in {@link StandardTriggerConditionUtils}.
     */
    private static final @NotNull List<@NotNull String> CONDITION_NAMES = List.of("always", "never", "elapsed-frames", "unit-count",
                                                                                  "opponents-defeated", "timer", "resource-amount");
    /**
     * The action type names, at the index of their type id in {@link StandardTriggerActionUtils}.
     */
    private static final @NotNull List<@NotNull String> ACTION_NAMES = List.of("victory", "defeat", "display-message", "set-timer", "wait",
                                                                               "repeat-trigger", "create-units", "remove-units", "give-units");
    /**
     * The comparison names, in the order of {@link Comparison}.
     */
    private static final @NotNull List<@NotNull String> COMPARISON_NAMES = List.of("at-least", "at-most", "exactly");

    private final @NotNull List<@NotNull UnitType> unitTypes;
    private final @NotNull List<@NotNull ResourceKind> resourceKinds;

    /**
     * Creates a {@link JsonMapLoader} instance over the types of a content.
     *
     * @param content the content the map names unit types and resource kinds of
     */
    public JsonMapLoader(@NotNull ContentDefinition content) {
        this(content.getUnitTypes().getTypes().getTypes(), content.getResourceKinds().getTypes());
    }

    /**
     * Creates a {@link JsonMapLoader} instance over the types a mod is about to register, for a map that is read before the game definition
     * is built (for example because its terrain configures the systems).
     *
     * @param unitTypes     the unit types the map may name, copied
     * @param resourceKinds the resource kinds the map may name, copied
     */
    public JsonMapLoader(@NotNull List<@NotNull UnitType> unitTypes, @NotNull List<@NotNull ResourceKind> resourceKinds) {
        this.unitTypes     = List.copyOf(unitTypes);
        this.resourceKinds = List.copyOf(resourceKinds);
    }

    /**
     * Loads a map file.
     *
     * @param file the map file
     * @return the loaded map
     * @throws IOException if the file cannot be read or is not a map of the format
     */
    @Override
    public @NotNull MapModel load(@NotNull Path file) throws IOException {
        return this.read(Files.readAllBytes(file), file.toString());
    }

    /**
     * Reads a map from the contents of a map file.
     *
     * @param contents the file contents
     * @param location the file location, for diagnostics
     * @return the map
     * @throws IOException if the contents are not a map of the format
     */
    public @NotNull MapModel read(byte @NotNull [] contents, @NotNull String location) throws IOException {
        JsonObjectReader map = JsonObjectReader.parse(contents, location);

        for (Object name : map.getNames()) {
            if (!MAP_FIELDS.contains(String.valueOf(name))) {
                throw Exceptions.newIOException("{0}: unknown field.", map.getLocation(String.valueOf(name)));
            }
        }

        if (!FORMAT.equals(map.getText("format")) || map.getInt("version") != VERSION) {
            throw Exceptions.newIOException("{0}: not a map of format {1} version {2}.", location, FORMAT, VERSION);
        }

        ArrayTerrainGrid terrain = this.readTerrain(map);
        int neutralSlot = map.getInt("neutralSlot");
        List<TriggerArea> locations = this.readLocations(map, terrain);
        List<TriggerDefinition> triggers = new ArrayList<>(this.readVictory(map));

        requireSlot(neutralSlot, map.getLocation("neutralSlot"));

        List<?> elements = map.getArray("triggers");

        for (int index = 0; index < elements.size(); index++) {
            triggers.add(this.readTrigger(JsonObjectReader.of(elements.get(index), map.getLocation("triggers") + "[" + index + "]"), locations));
        }

        TileCodeGrid tileCodes = new TileCodeGrid(map.getObject("terrain").getTextList("rows"));
        StandardMapData data = new StandardMapData(map.getText("name"), tileCodes, neutralSlot, this.readResources(map, terrain), locations,
                                                   triggers);

        return new MapModel(terrain, map.getText("tileset"), this.readStartLocations(map, terrain), this.readUnits(map, terrain), data);
    }

    private @NotNull ArrayTerrainGrid readTerrain(@NotNull JsonObjectReader map) throws IOException {
        int tileSize = map.getInt("tileSize");
        int walkCellSize = map.getInt("walkCellSize");
        int width = map.getInt("width");
        int height = map.getInt("height");

        if (tileSize <= 0 || walkCellSize <= 0 || tileSize % walkCellSize != 0 || width <= 0 || height <= 0) {
            throw Exceptions.newIOException("{0}: invalid tile size {1}, walk cell size {2} or size {3}x{4}.", map.getLocation(), tileSize,
                                            walkCellSize, width, height);
        }

        JsonObjectReader terrainObject = map.getObject("terrain");
        JsonObjectReader legend = terrainObject.getObject("legend");
        List<String> rows = terrainObject.getTextList("rows");
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(tileSize, walkCellSize, width, height);
        int cellsPerTile = tileSize / walkCellSize;

        if (rows.size() != height) {
            throw Exceptions.newIOException("{0}: {1} rows for a height of {2}.", terrainObject.getLocation("rows"), rows.size(), height);
        }

        for (int tileY = 0; tileY < height; tileY++) {
            String row = rows.get(tileY);
            String rowLocation = terrainObject.getLocation("rows") + "[" + tileY + "]";

            if (row.length() != width) {
                throw Exceptions.newIOException("{0}: {1} tiles for a width of {2}.", rowLocation, row.length(), width);
            }

            for (int tileX = 0; tileX < width; tileX++) {
                String code = String.valueOf(row.charAt(tileX));

                if (!legend.has(code)) {
                    throw Exceptions.newIOException("{0}: the tile code {1} is not in the legend.", rowLocation, code);
                }

                JsonObjectReader tile = legend.getObject(code);
                boolean walkable = tile.getBoolean("walkable", true);
                int elevation = tile.getInt("elevation", 0);

                if (elevation < 0 || elevation > ArrayTerrainGrid.MAXIMUM_ELEVATION) {
                    throw Exceptions.newIOException("{0}: elevation {1} outside 0 to {2}.", tile.getLocation("elevation"), elevation,
                                                    ArrayTerrainGrid.MAXIMUM_ELEVATION);
                }

                terrain.setBuildable(tileX, tileY, tile.getBoolean("buildable", walkable));

                for (int cellY = 0; cellY < cellsPerTile; cellY++) {
                    for (int cellX = 0; cellX < cellsPerTile; cellX++) {
                        terrain.setWalkable(tileX * cellsPerTile + cellX, tileY * cellsPerTile + cellY, walkable);
                        terrain.setElevation(tileX * cellsPerTile + cellX, tileY * cellsPerTile + cellY, elevation);
                    }
                }
            }
        }

        return terrain;
    }

    private @NotNull List<@NotNull StartLocation> readStartLocations(@NotNull JsonObjectReader map, @NotNull ArrayTerrainGrid terrain) throws IOException {
        List<?> elements = map.getArray("startLocations");
        List<StartLocation> startLocations = new ArrayList<>();

        for (int index = 0; index < elements.size(); index++) {
            JsonObjectReader element = JsonObjectReader.of(elements.get(index), map.getLocation("startLocations") + "[" + index + "]");
            int slot = requireSlot(element.getInt("slot"), element.getLocation("slot"));

            startLocations.add(new StartLocation(slot, readX(element, terrain), readY(element, terrain)));
        }

        return startLocations;
    }

    private @NotNull List<@NotNull PreplacedUnit> readUnits(@NotNull JsonObjectReader map, @NotNull ArrayTerrainGrid terrain) throws IOException {
        List<?> elements = map.getArray("units");
        List<PreplacedUnit> units = new ArrayList<>();

        for (int index = 0; index < elements.size(); index++) {
            JsonObjectReader element = JsonObjectReader.of(elements.get(index), map.getLocation("units") + "[" + index + "]");
            UnitType type = this.findUnitType(element.getText("type"), element.getLocation("type"));
            int slot = requireSlot(element.getInt("slot"), element.getLocation("slot"));

            units.add(new PreplacedUnit(type, slot, readX(element, terrain), readY(element, terrain)));
        }

        return units;
    }

    private @NotNull List<@NotNull MapResource> readResources(@NotNull JsonObjectReader map, @NotNull ArrayTerrainGrid terrain) throws IOException {
        List<?> elements = map.getArray("resources");
        List<MapResource> resources = new ArrayList<>();

        for (int index = 0; index < elements.size(); index++) {
            JsonObjectReader element = JsonObjectReader.of(elements.get(index), map.getLocation("resources") + "[" + index + "]");
            UnitType type = this.findUnitType(element.getText("type"), element.getLocation("type"));
            ResourceKind kind = this.findResourceKind(element.getText("kind"), element.getLocation("kind"));
            int amount = element.getInt("amount");

            if (amount < 1) {
                throw Exceptions.newIOException("{0}: the amount {1} is below 1.", element.getLocation("amount"), amount);
            }

            resources.add(new MapResource(type, readX(element, terrain), readY(element, terrain), kind, amount));
        }

        return resources;
    }

    private @NotNull List<@NotNull TriggerArea> readLocations(@NotNull JsonObjectReader map, @NotNull ArrayTerrainGrid terrain) throws IOException {
        List<?> elements = map.getArray("locations");
        List<TriggerArea> locations = new ArrayList<>();
        int mapWidth = terrain.getTileGrid().getWorldWidth();
        int mapHeight = terrain.getTileGrid().getWorldHeight();

        for (int index = 0; index < elements.size(); index++) {
            JsonObjectReader element = JsonObjectReader.of(elements.get(index), map.getLocation("locations") + "[" + index + "]");
            String name = element.getText("name");
            int left = element.getInt("left");
            int top = element.getInt("top");
            int right = element.getInt("right");
            int bottom = element.getInt("bottom");

            if (left < 0 || top < 0 || right < left || bottom < top || right >= mapWidth || bottom >= mapHeight) {
                throw Exceptions.newIOException("{0}: the rectangle {1},{2} to {3},{4} does not lie on the map of {5}x{6} pixels.",
                                                element.getLocation(), left, top, right, bottom, mapWidth, mapHeight);
            }

            for (TriggerArea location : locations) {
                if (location.getName().equals(name)) {
                    throw Exceptions.newIOException("{0}: the location name {1} is used twice.", element.getLocation("name"), name);
                }
            }

            locations.add(new TriggerArea(name, left, top, right, bottom));
        }

        return locations;
    }

    private @NotNull List<@NotNull TriggerDefinition> readVictory(@NotNull JsonObjectReader map) throws IOException {
        String victory = map.findText("victory");

        if (victory == null || victory.equals("none")) {
            return List.of();
        }

        return switch (victory) {
            case "annihilation" -> StandardTriggerUtils.createAnnihilation();
            case "elimination" -> StandardTriggerUtils.createElimination();
            default -> throw Exceptions.newIOException("{0}: unknown victory rule {1}.", map.getLocation("victory"), victory);
        };
    }

    private @NotNull TriggerDefinition readTrigger(@NotNull JsonObjectReader trigger, @NotNull List<@NotNull TriggerArea> locations) throws IOException {
        TriggerDefinition.Builder builder = new TriggerDefinition.Builder().setRepeating(trigger.getBoolean("repeating", false));
        Object players = trigger.get("players");

        if (ALL_PLAYERS.equals(players)) {
            builder.forAllPlayers();
        } else {
            List<?> slots = trigger.getArray("players");

            if (slots.isEmpty()) {
                throw Exceptions.newIOException("{0}: expected \"all\" or an array of player slots.", trigger.getLocation("players"));
            }

            for (int index = 0; index < slots.size(); index++) {
                String slotLocation = trigger.getLocation("players") + "[" + index + "]";

                builder.addPlayer(requireSlot(JsonObjectReader.toInt(slots.get(index), slotLocation), slotLocation));
            }
        }

        List<?> conditions = trigger.getArray("conditions");
        List<?> actions = trigger.getArray("actions");

        for (int index = 0; index < conditions.size(); index++) {
            builder.addCondition(this.readCondition(JsonObjectReader.of(conditions.get(index), trigger.getLocation("conditions") + "[" + index + "]"),
                                                    locations));
        }

        for (int index = 0; index < actions.size(); index++) {
            builder.addAction(this.readAction(JsonObjectReader.of(actions.get(index), trigger.getLocation("actions") + "[" + index + "]"), locations));
        }

        return builder.build();
    }

    private @NotNull ConditionRecord readCondition(@NotNull JsonObjectReader condition, @NotNull List<@NotNull TriggerArea> locations) throws IOException {
        int type = findName(CONDITION_NAMES, condition.getText("type"), condition.getLocation("type"));
        String unitType = condition.findText("unitType");
        String unitTrait = condition.findText("unitTrait");
        String resourceKind = condition.findText("resourceKind");
        String comparison = condition.findText("comparison");

        return new ConditionRecord.Builder(type).setPlayer(readPlayer(condition, "player"))
                                                .setUnitType(unitType != null ? this.findUnitType(unitType, condition.getLocation("unitType")) : null)
                                                .setUnitTrait(unitTrait != null ? findTrait(unitTrait, condition.getLocation("unitTrait")) : null)
                                                .setArea(findArea(condition, locations))
                                                .setResourceKind(resourceKind != null ? this.findResourceKind(resourceKind,
                                                                                                              condition.getLocation("resourceKind"))
                                                                                      : null)
                                                .setComparison(comparison != null ? Comparison.values()[findName(COMPARISON_NAMES, comparison,
                                                                                                                 condition.getLocation("comparison"))]
                                                                                  : Comparison.AT_LEAST)
                                                .setAmount(condition.getInt("amount", 0))
                                                .setTimer(readTimer(condition))
                                                .build();
    }

    private @NotNull ActionRecord readAction(@NotNull JsonObjectReader action, @NotNull List<@NotNull TriggerArea> locations) throws IOException {
        int type = findName(ACTION_NAMES, action.getText("type"), action.getLocation("type"));
        String unitType = action.findText("unitType");
        String text = action.findText("text");

        return new ActionRecord.Builder(type).setPlayer(readPlayer(action, "player"))
                                             .setTargetPlayer(readPlayer(action, "targetPlayer"))
                                             .setUnitType(unitType != null ? this.findUnitType(unitType, action.getLocation("unitType")) : null)
                                             .setArea(findArea(action, locations))
                                             .setAmount(action.getInt("amount", ActionRecord.ALL_UNITS))
                                             .setTimer(readTimer(action))
                                             .setText(text != null ? text : "")
                                             .build();
    }

    private @NotNull UnitType findUnitType(@NotNull String name, @NotNull String location) throws IOException {
        return findType(this.unitTypes, name, location, "unit type");
    }

    private @NotNull ResourceKind findResourceKind(@NotNull String name, @NotNull String location) throws IOException {
        return findType(this.resourceKinds, name, location, "resource kind");
    }

    private static <T extends ContentType> @NotNull T findType(@NotNull List<@NotNull T> types, @NotNull String name, @NotNull String location,
                                                               @NotNull String kind) throws IOException {
        for (T type : types) {
            if (type.getDebugName().equals(name)) {
                return type;
            }
        }

        throw Exceptions.newIOException("{0}: unknown {1} {2}.", location, kind, name);
    }

    private static @NotNull UnitTrait findTrait(@NotNull String name, @NotNull String location) throws IOException {
        for (UnitTrait trait : UnitTrait.values()) {
            if (trait.name().toLowerCase(Locale.ROOT).replace('_', '-').equals(name)) {
                return trait;
            }
        }

        throw Exceptions.newIOException("{0}: unknown unit trait {1}.", location, name);
    }

    private static int findName(@NotNull List<@NotNull String> names, @NotNull String name, @NotNull String location) throws IOException {
        int index = names.indexOf(name);

        if (index < 0) {
            throw Exceptions.newIOException("{0}: unknown name {1}; expected one of {2}.", location, name, names);
        }

        return index;
    }

    private static @Nullable TriggerArea findArea(@NotNull JsonObjectReader record, @NotNull List<@NotNull TriggerArea> locations) throws IOException {
        String name = record.findText("location");

        if (name == null) {
            return null;
        }

        for (TriggerArea location : locations) {
            if (location.getName().equals(name)) {
                return location;
            }
        }

        throw Exceptions.newIOException("{0}: unknown location {1}.", record.getLocation("location"), name);
    }

    private static @NotNull PlayerSelector readPlayer(@NotNull JsonObjectReader record, @NotNull String field) throws IOException {
        Object value = record.get(field);

        if (value == null || CURRENT_PLAYER.equals(value)) {
            return PlayerSelector.CURRENT;
        }

        return PlayerSelector.ofSlot(requireSlot(JsonObjectReader.toInt(value, record.getLocation(field)), record.getLocation(field)));
    }

    private static int readTimer(@NotNull JsonObjectReader record) throws IOException {
        int timer = record.getInt("timer", 0);

        if (timer < 0 || timer >= StandardTriggerSystem.TIMER_COUNT) {
            throw Exceptions.newIOException("{0}: no timer {1}.", record.getLocation("timer"), timer);
        }

        return timer;
    }

    private static int readX(@NotNull JsonObjectReader element, @NotNull ArrayTerrainGrid terrain) throws IOException {
        return requireCoordinate(element.getInt("x"), terrain.getTileGrid().getWorldWidth(), element.getLocation("x"));
    }

    private static int readY(@NotNull JsonObjectReader element, @NotNull ArrayTerrainGrid terrain) throws IOException {
        return requireCoordinate(element.getInt("y"), terrain.getTileGrid().getWorldHeight(), element.getLocation("y"));
    }

    private static int requireCoordinate(int value, int size, @NotNull String location) throws IOException {
        if (value < 0 || value >= size) {
            throw Exceptions.newIOException("{0}: {1} does not lie on the map of {2} pixels.", location, value, size);
        }

        return value;
    }

    private static int requireSlot(int slot, @NotNull String location) throws IOException {
        if (slot < 0) {
            throw Exceptions.newIOException("{0}: invalid player slot {1}.", location, slot);
        }

        return slot;
    }
}
