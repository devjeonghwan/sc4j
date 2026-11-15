package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.engine.trigger.TriggerArea;
import org.fogofwarlabs.sc4j.engine.trigger.TriggerDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds the map data of the engine map format beyond the engine map model ({@link MapModel#getModData}): the map name, the tile codes of the
 * terrain, the slot of the neutral player that owns the resources, the resource containers, the named locations and the triggers of the
 * standard trigger system.
 */
public final class StandardMapData {
    private final @NotNull String name;
    private final @NotNull TileCodeGrid tileCodes;
    private final int neutralSlotIndex;
    private final @NotNull List<@NotNull MapResource> resources;
    private final @NotNull List<@NotNull TriggerArea> locations;
    private final @NotNull List<@NotNull TriggerDefinition> triggers;

    /**
     * Creates a {@link StandardMapData} instance.
     *
     * @param name             the map name
     * @param tileCodes        the tile code of every tile
     * @param neutralSlotIndex the player slot of the neutral player, at least {@code 0}
     * @param resources        the resource containers in map order, copied
     * @param locations        the named locations in map order, copied
     * @param triggers         the triggers in map order, copied
     */
    public StandardMapData(@NotNull String name, @NotNull TileCodeGrid tileCodes, int neutralSlotIndex,
                           @NotNull List<@NotNull MapResource> resources, @NotNull List<@NotNull TriggerArea> locations,
                           @NotNull List<@NotNull TriggerDefinition> triggers) {
        this.name             = name;
        this.tileCodes        = tileCodes;
        this.neutralSlotIndex = neutralSlotIndex;
        this.resources        = List.copyOf(resources);
        this.locations        = List.copyOf(locations);
        this.triggers         = List.copyOf(triggers);
    }

    /**
     * @return the map name
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the tile code of every tile, the codes of the map's terrain rows
     */
    public @NotNull TileCodeGrid getTileCodes() {
        return this.tileCodes;
    }

    /**
     * @return the player slot of the neutral player that owns the resource containers
     */
    public int getNeutralSlotIndex() {
        return this.neutralSlotIndex;
    }

    /**
     * @return the resource containers in map order, unmodifiable
     */
    public @NotNull List<@NotNull MapResource> getResources() {
        return this.resources;
    }

    /**
     * @return the named locations in map order, unmodifiable
     */
    public @NotNull List<@NotNull TriggerArea> getLocations() {
        return this.locations;
    }

    /**
     * @return the triggers in map order, unmodifiable
     */
    public @NotNull List<@NotNull TriggerDefinition> getTriggers() {
        return this.triggers;
    }


    /**
     * Finds a named location.
     *
     * @param name the location name
     * @return the location, or {@code null} if the map has none of the name
     */
    public @Nullable TriggerArea findLocation(@NotNull String name) {
        for (TriggerArea location : this.locations) {
            if (location.getName().equals(name)) {
                return location;
            }
        }

        return null;
    }
}
