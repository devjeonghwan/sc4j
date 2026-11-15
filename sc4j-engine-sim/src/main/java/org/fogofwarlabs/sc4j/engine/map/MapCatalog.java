package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the loaded maps a mod offers, each under the {@link MapReference} of its file (name and digest) with the file contents, so that a game
 * setup finds its map: the system factories of a game definition that serves many games read the map's terrain through
 * {@link #require(GameSetup)}, and a replay or a network game finds the map its setup names, or learns that it is missing or different. A
 * network lobby sends the contents of a map ({@link #findContents}) to a member that misses it. Two maps may share a name if their digests
 * differ, for example a received version of a map next to the own one.
 * <p>
 * A loaded map is shared by every game of it, so nothing may change it after loading; the engine map model and the engine default systems
 * only read it.
 */
public final class MapCatalog {
    private final @NotNull List<@NotNull MapReference> references;
    private final @NotNull List<byte @NotNull []> contents;
    private final @NotNull List<@NotNull MapModel> maps;

    /**
     * Creates an empty {@link MapCatalog} instance.
     */
    public MapCatalog() {
        this.references = new ArrayList<>();
        this.contents   = new ArrayList<>();
        this.maps       = new ArrayList<>();
    }

    /**
     * @return the references of the maps in the order they were added
     */
    public @NotNull List<@NotNull MapReference> getReferences() {
        return List.copyOf(this.references);
    }


    /**
     * Adds a loaded map under the reference of its file.
     *
     * @param name     the map name
     * @param contents the map file contents the map was loaded from; copied
     * @param map      the loaded map
     * @return the created reference of the map
     * @throws IllegalArgumentException if the catalog already has a map of the name and the contents' digest
     */
    public @NotNull MapReference add(@NotNull String name, byte @NotNull [] contents, @NotNull MapModel map) {
        MapReference reference = MapReference.create(name, contents);

        if (this.find(reference) != null) {
            throw Exceptions.newIllegalArgumentException("The map catalog already has the map {0} of the same digest.", name);
        }

        this.references.add(reference);
        this.contents.add(contents.clone());
        this.maps.add(map);

        return reference;
    }

    /**
     * Finds the map of a reference.
     *
     * @param reference the reference, for example of a game setup
     * @return the map of the same name and digest, or {@code null} if the catalog has none
     */
    public @Nullable MapModel find(@NotNull MapReference reference) {
        for (int index = 0; index < this.references.size(); index++) {
            if (this.references.get(index).isSameMap(reference)) {
                return this.maps.get(index);
            }
        }

        return null;
    }

    /**
     * Finds the file contents of the map of a reference.
     *
     * @param reference the reference
     * @return a copy of the contents of the map of the same name and digest, or {@code null} if the catalog has none
     */
    public byte @Nullable [] findContents(@NotNull MapReference reference) {
        for (int index = 0; index < this.references.size(); index++) {
            if (this.references.get(index).isSameMap(reference)) {
                return this.contents.get(index).clone();
            }
        }

        return null;
    }

    /**
     * Returns the map of a game setup.
     *
     * @param setup the game setup
     * @return the map of the setup's map reference
     * @throws IllegalArgumentException if the catalog has no map of the same name and digest
     */
    public @NotNull MapModel require(@NotNull GameSetup setup) {
        return this.require(setup.getMap());
    }

    /**
     * Returns the map of a reference.
     *
     * @param reference the reference
     * @return the map of the same name and digest
     * @throws IllegalArgumentException if the catalog has no map of the same name and digest
     */
    public @NotNull MapModel require(@NotNull MapReference reference) {
        MapModel map = this.find(reference);

        if (map == null) {
            throw Exceptions.newIllegalArgumentException("The map catalog has no map {0} of the given digest.", reference.getName());
        }

        return map;
    }
}
