package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.display.DetailUtils;
import org.fogofwarlabs.sc4j.engine.client.projection.IsometricProjection;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.world.TilesetLayer;
import org.fogofwarlabs.sc4j.engine.map.MapModel;
import org.fogofwarlabs.sc4j.engine.map.StandardMapData;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Creates the world view parts of a demo game: the demo selects the engine {@link IsometricProjection}, whose diamond is the size of the tile
 * pictures of the map's tileset in the pack, and draws the terrain with the engine {@link TilesetLayer} over the map's tile codes. The pack
 * and the maps stay free of projection data: a tileset only states its tile size and shape.
 */
public final class DemoWorldUtils {
    /**
     * Loads the tile pictures of a map's tileset from the demo pack, with their detail pictures.
     *
     * @param content the demo content with its pack
     * @param map     the map
     * @return the tile pictures with their details
     * @throws IOException if the tileset cannot be read
     */
    public static @NotNull DetailSet<TileGraphics> loadTileGraphics(@NotNull DemoContent content, @NotNull MapModel map) throws IOException {
        return DetailUtils.loadTileGraphics(content.getPack(), ResourceKey.of(map.getTilesetName()));
    }

    /**
     * Creates the projection of the demo's world view: isometric, a map tile showing as a diamond of the tile pictures' size.
     *
     * @param map   the map
     * @param tiles the tile pictures of the map's tileset with their details
     * @return the created {@link WorldProjection} instance
     */
    public static @NotNull WorldProjection createProjection(@NotNull MapModel map, @NotNull DetailSet<TileGraphics> tiles) {
        TileGraphics graphics = tiles.getResource();

        return new IsometricProjection(map.getTerrain().getTileSize(), graphics.getTileWidth(), graphics.getTileHeight());
    }

    /**
     * Creates the terrain layer of the demo's world view: the map's tileset pictures over its tile codes.
     *
     * @param canvas  the canvas the layer draws on
     * @param view    the game view
     * @param content the demo content with its pack
     * @param map     the map
     * @param tiles   the tile pictures of the map's tileset with their details
     * @return the created {@link TilesetLayer} instance, closed by the caller
     * @throws IOException if the tileset legend cannot be read
     */
    public static @NotNull TilesetLayer createTerrainLayer(@NotNull Canvas canvas, @NotNull GameView view, @NotNull DemoContent content,
                                                           @NotNull MapModel map, @NotNull DetailSet<TileGraphics> tiles) throws IOException {
        return new TilesetLayer(canvas, view, tiles, content.getPack().loadTilesetLegend(ResourceKey.of(map.getTilesetName())),
                                map.requireModData(StandardMapData.class).getTileCodes());
    }

    private DemoWorldUtils() {
    }
}
