package org.fogofwarlabs.sc4j.engine.map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests the tile pictures of tile codes: variants per code, a selection that depends on the tile position only and the failures of
 * unknown codes.
 */
final class TilesetLegendTest {
    private static final int SAMPLE_SIZE = 16;

    @Test
    void select_tile_should_pick_variants_of_the_code_from_the_position_only() {
        TilesetLegend legend = new TilesetLegend(Map.of('.', new int[]{0, 1, 2}, '#', new int[]{5}));
        Set<Integer> chosen = new HashSet<>();

        for (int tileY = 0; tileY < SAMPLE_SIZE; tileY++) {
            for (int tileX = 0; tileX < SAMPLE_SIZE; tileX++) {
                int tile = legend.selectTile('.', tileX, tileY);

                Assertions.assertEquals(tile, legend.selectTile('.', tileX, tileY));
                Assertions.assertEquals(5, legend.selectTile('#', tileX, tileY));

                chosen.add(tile);
            }
        }

        Assertions.assertEquals(Set.of(0, 1, 2), chosen);
        Assertions.assertEquals("#.", legend.getCodes());
        Assertions.assertEquals(3, legend.getVariantCount('.'));
        Assertions.assertEquals(2, legend.getTile('.', 2));
        Assertions.assertTrue(legend.hasCode('#'));
        Assertions.assertFalse(legend.hasCode(','));
    }

    @Test
    void select_tile_should_reject_codes_without_pictures() {
        TilesetLegend legend = new TilesetLegend(Map.of('.', new int[]{0}));

        Assertions.assertThrows(IllegalArgumentException.class, () -> legend.selectTile(',', 0, 0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> legend.getTile('.', 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TilesetLegend(Map.of('.', new int[0])));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TilesetLegend(Map.of('.', new int[]{-1})));
    }
}
