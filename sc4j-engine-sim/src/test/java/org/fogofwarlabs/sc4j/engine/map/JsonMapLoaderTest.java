package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.fogofwarlabs.sc4j.engine.trigger.StandardTriggerActionUtils;
import org.fogofwarlabs.sc4j.engine.trigger.StandardTriggerConditionUtils;
import org.fogofwarlabs.sc4j.engine.trigger.TriggerDefinition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tests the engine map format: the skirmish map reads into its terrain, start locations, preplaced units, resources, locations and triggers,
 * broken maps fail with the location of the broken field, and maps of another format version are refused.
 */
final class JsonMapLoaderTest {
    private static final @NotNull String MINIMAL_MAP = """
                                                       {"format": "sc4j-map", "version": 1, "name": "tiny", "tileset": "meadow", "tileSize": 32, "walkCellSize": 8, "width": 2,
                                                        "height": 2, "neutralSlot": 2, "terrain": {"legend": {".": {}, "#": {"walkable": false}}, "rows": [".#", ".."]}%s}
                                                       """;

    private final @NotNull UnitType scout;
    private final @NotNull ResourceKind gold;
    private final @NotNull JsonMapLoader loader;

    JsonMapLoaderTest() {
        UnitType mine = new UnitType("gold mine");

        this.scout  = new UnitType("scout");
        this.gold   = new ResourceKind("gold");
        this.loader = new JsonMapLoader(List.of(mine, this.scout), List.of(this.gold));
    }

    @Test
    void read_should_return_the_model_of_the_skirmish_map() throws IOException {
        MapModel map = this.loader.read(MapTestResources.read(MapTestResources.TWIN_ROCKS), MapTestResources.TWIN_ROCKS);
        TerrainGrid terrain = map.getTerrain();
        StandardMapData data = map.requireModData(StandardMapData.class);

        Assertions.assertEquals("meadow", map.getTilesetName());
        Assertions.assertEquals(48, terrain.getWidth());
        Assertions.assertTrue(terrain.isWalkable(10 * 4, 10 * 4));
        Assertions.assertFalse(terrain.isWalkable(20 * 4, 20 * 4));
        Assertions.assertEquals(1, terrain.getElevation(20 * 4, 20 * 4));
        Assertions.assertFalse(terrain.isBuildable(0, 5));
        Assertions.assertTrue(terrain.isBuildable(5, 5));
        Assertions.assertEquals(2, map.getStartLocations().size());
        Assertions.assertEquals(1, map.getStartLocations().get(1).getSlotIndex());
        Assertions.assertSame(this.scout, map.getPreplacedUnits().getFirst().getType());
        Assertions.assertEquals("Twin Rocks", data.getName());
        Assertions.assertEquals(2, data.getNeutralSlotIndex());
        Assertions.assertEquals(4, data.getResources().size());
        Assertions.assertSame(this.gold, data.getResources().getFirst().getKind());
        Assertions.assertNotNull(data.findLocation("center"));

        List<TriggerDefinition> triggers = data.getTriggers();

        Assertions.assertEquals(4, triggers.size());
        Assertions.assertEquals(StandardTriggerConditionUtils.UNIT_COUNT, triggers.getFirst().getConditions().getFirst().getType());
        Assertions.assertEquals(StandardTriggerActionUtils.DISPLAY_MESSAGE, triggers.get(2).getActions().getFirst().getType());
        Assertions.assertTrue(triggers.get(3).isRepeating());
        Assertions.assertEquals(List.of(0, 1), triggers.get(3).getPlayerSlots());
        Assertions.assertSame(data.findLocation("center"), triggers.get(3).getConditions().getFirst().getArea());
    }

    @Test
    void read_should_read_a_repeating_trigger_and_its_repeat_action() throws IOException {
        TriggerDefinition trigger = this.read(", \"triggers\": [{\"players\": \"all\", \"repeating\": true, \"actions\": [{\"type\": \"repeat-trigger\"}]}]")
                                        .requireModData(StandardMapData.class)
                                        .getTriggers()
                                        .getFirst();

        Assertions.assertTrue(trigger.isRepeating());
        Assertions.assertEquals(StandardTriggerActionUtils.REPEAT_TRIGGER, trigger.getActions().getFirst().getType());
    }

    @Test
    void read_should_refuse_a_map_of_another_format_version() {
        byte[] other = MINIMAL_MAP.formatted("").replace("\"version\": 1", "\"version\": 2").getBytes(StandardCharsets.UTF_8);
        IOException version = Assertions.assertThrows(IOException.class, () -> this.loader.read(other, "other.json"));

        Assertions.assertTrue(version.getMessage().contains("version " + JsonMapLoader.VERSION), version.getMessage());
    }

    @Test
    void read_should_keep_the_tile_codes_of_the_terrain_rows() throws IOException {
        TileCodeGrid codes = this.read("").requireModData(StandardMapData.class).getTileCodes();

        Assertions.assertEquals(2, codes.getWidth());
        Assertions.assertEquals(2, codes.getHeight());
        Assertions.assertEquals('.', codes.getCode(0, 0));
        Assertions.assertEquals('#', codes.getCode(1, 0));
        Assertions.assertEquals('.', codes.getCode(1, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> codes.getCode(2, 0));
    }

    @Test
    void read_should_name_the_broken_field_of_a_map() throws IOException {
        Assertions.assertEquals(2, this.read("").getTerrain().getHeight());

        this.assertBroken(", \"units\": [{\"type\": \"castle\", \"slot\": 0, \"x\": 1, \"y\": 1}]", "units[0].type");
        this.assertBroken(", \"units\": [{\"type\": \"scout\", \"slot\": 0, \"x\": 64, \"y\": 1}]", "units[0].x");
        this.assertBroken(", \"extra\": 1", "extra");
        this.assertBroken(", \"triggers\": [{\"players\": \"all\", \"actions\": [{\"type\": \"explode\"}]}]", "triggers[0].actions[0].type");
        this.assertBroken(", \"triggers\": [{\"players\": [], \"actions\": []}]", "triggers[0].players");
        this.assertBroken(", \"victory\": \"conquest\"", "victory");
        this.assertBroken(", \"resources\": [{\"type\": \"gold mine\", \"kind\": \"gold\", \"amount\": 0, \"x\": 1, \"y\": 1}]", "resources[0].amount");

        byte[] otherFormat = "{\"format\": \"other\", \"version\": 1}".getBytes(StandardCharsets.UTF_8);
        IOException format = Assertions.assertThrows(IOException.class, () -> this.loader.read(otherFormat, "other.json"));

        Assertions.assertTrue(format.getMessage().contains("sc4j-map"));
    }

    private @NotNull MapModel read(@NotNull String extra) throws IOException {
        return this.loader.read(MINIMAL_MAP.formatted(extra).getBytes(StandardCharsets.UTF_8), "tiny.json");
    }

    private void assertBroken(@NotNull String extra, @NotNull String field) {
        IOException exception = Assertions.assertThrows(IOException.class, () -> this.read(extra));

        Assertions.assertTrue(exception.getMessage().contains(field), exception.getMessage());
    }
}
