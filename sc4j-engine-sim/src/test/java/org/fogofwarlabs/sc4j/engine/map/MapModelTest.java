package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.terrain.ArrayTerrainGrid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine map model: copied start locations and preplaced units, and the typed mod data.
 */
final class MapModelTest {
    @Test
    void map_model_keeps_copies_and_the_mod_data() {
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(32, 8, 4, 4);
        UnitType peasant = new UnitType("peasant");
        List<StartLocation> startLocations = new ArrayList<>(List.of(new StartLocation(0, 48, 48), new StartLocation(1, 80, 80)));
        List<PreplacedUnit> preplacedUnits = new ArrayList<>(List.of(new PreplacedUnit(peasant, 0, 56, 48)));
        MapModel model = new MapModel(terrain, "grassland", startLocations, preplacedUnits, "triggers");

        startLocations.clear();
        preplacedUnits.clear();

        Assertions.assertSame(terrain, model.getTerrain());
        Assertions.assertEquals("grassland", model.getTilesetName());
        Assertions.assertEquals(2, model.getStartLocations().size());
        Assertions.assertEquals(1, model.getStartLocations().get(1).getSlotIndex());
        Assertions.assertSame(peasant, model.getPreplacedUnits().get(0).getType());
        Assertions.assertEquals("triggers", model.requireModData(String.class));
        Assertions.assertThrows(IllegalStateException.class, () -> model.requireModData(Integer.class));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> model.getStartLocations().clear());
    }
}
