package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tests the map catalog: maps under the name and digest of their files with their contents, the lookup of a setup's map, the refusal of a
 * missing, renamed or changed map, and two versions of one map name.
 */
final class MapCatalogTest {
    private static final @NotNull String MAP = """
                                               {"format": "sc4j-map", "version": 1, "name": "%s", "tileset": "meadow", "tileSize": 32, "walkCellSize": 8, "width": 2,
                                                "height": 2, "neutralSlot": 2, "terrain": {"legend": {".": {}}, "rows": ["..", ".."]}}
                                               """;
    private static final int DIGEST_LENGTH = 32;

    @Test
    void require_should_return_the_map_of_the_same_name_and_digest() throws IOException {
        JsonMapLoader loader = new JsonMapLoader(List.of(), List.of());
        byte[] firstContents = MAP.formatted("first").getBytes(StandardCharsets.UTF_8);
        byte[] secondContents = MAP.formatted("second").getBytes(StandardCharsets.UTF_8);
        MapModel first = loader.read(firstContents, "first.json");
        MapModel second = loader.read(secondContents, "second.json");
        MapCatalog catalog = new MapCatalog();
        MapReference firstReference = catalog.add("first", firstContents, first);
        MapReference secondReference = catalog.add("second", secondContents, second);
        GameSetup setup = new GameSetup.Builder(MapReference.create("second", secondContents)).build();

        Assertions.assertEquals(DIGEST_LENGTH, firstReference.getDigest().length);
        Assertions.assertEquals(List.of("first", "second"), catalog.getReferences().stream().map(MapReference::getName).toList());
        Assertions.assertSame(first, catalog.require(MapReference.create("first", firstContents)));
        Assertions.assertSame(second, catalog.require(setup));
        Assertions.assertTrue(secondReference.isSameMap(setup.getMap()));
        Assertions.assertFalse(firstReference.isSameMap(secondReference));
        Assertions.assertNull(catalog.find(MapReference.create("first", secondContents)));
        Assertions.assertNull(catalog.find(MapReference.create("renamed", firstContents)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> catalog.require(new MapReference("first", new byte[0])));
        Assertions.assertThrows(IllegalArgumentException.class, () -> catalog.add("first", firstContents, first));
        Assertions.assertArrayEquals(secondContents, catalog.findContents(setup.getMap()));
        Assertions.assertNull(catalog.findContents(MapReference.create("first", secondContents)));

        MapReference otherFirst = catalog.add("first", secondContents, second);

        Assertions.assertSame(second, catalog.require(otherFirst));
        Assertions.assertSame(first, catalog.require(firstReference));
    }
}
