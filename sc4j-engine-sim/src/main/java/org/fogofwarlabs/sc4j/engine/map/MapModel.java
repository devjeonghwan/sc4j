package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds what a game needs to know about its map: the terrain, the name of the tileset graphics the mod's data pack resolves, the start location
 * of each map player slot, the preplaced units and the mod's own map data (triggers, locations, strings and whatever else its format holds),
 * which the mod interprets.
 */
public final class MapModel {
    private final @NotNull TerrainGrid terrain;
    private final @NotNull String tilesetName;
    private final @NotNull List<@NotNull StartLocation> startLocations;
    private final @NotNull List<@NotNull PreplacedUnit> preplacedUnits;
    private final @Nullable Object modData;

    /**
     * Creates a {@link MapModel} instance.
     *
     * @param terrain        the terrain
     * @param tilesetName    the name of the tileset graphics
     * @param startLocations the start locations in map order, copied
     * @param preplacedUnits the preplaced units in map order, copied
     * @param modData        the mod's own map data, or {@code null} for none
     */
    public MapModel(@NotNull TerrainGrid terrain,
                    @NotNull String tilesetName,
                    @NotNull List<@NotNull StartLocation> startLocations,
                    @NotNull List<@NotNull PreplacedUnit> preplacedUnits,
                    @Nullable Object modData) {
        this.terrain        = terrain;
        this.tilesetName    = tilesetName;
        this.startLocations = List.copyOf(startLocations);
        this.preplacedUnits = List.copyOf(preplacedUnits);
        this.modData        = modData;
    }

    /**
     * @return the terrain
     */
    public @NotNull TerrainGrid getTerrain() {
        return this.terrain;
    }

    /**
     * @return the name of the tileset graphics
     */
    public @NotNull String getTilesetName() {
        return this.tilesetName;
    }

    /**
     * @return the start locations in map order, unmodifiable
     */
    public @NotNull List<@NotNull StartLocation> getStartLocations() {
        return this.startLocations;
    }

    /**
     * @return the preplaced units in map order, unmodifiable
     */
    public @NotNull List<@NotNull PreplacedUnit> getPreplacedUnits() {
        return this.preplacedUnits;
    }

    /**
     * @return the mod's own map data, or {@code null} for none
     */
    public @Nullable Object getModData() {
        return this.modData;
    }

    /**
     * Reads the mod's own map data as the type the mod stored.
     *
     * @param type the type of the map data
     * @param <T>  the type of the map data
     * @return the map data
     * @throws IllegalStateException if the map has no data of the type
     */
    public <T> @NotNull T requireModData(@NotNull Class<T> type) {
        if (!type.isInstance(this.modData)) {
            throw Exceptions.newIllegalStateException("The map has no mod data of type {0}.", type.getName());
        }

        return type.cast(this.modData);
    }
}
