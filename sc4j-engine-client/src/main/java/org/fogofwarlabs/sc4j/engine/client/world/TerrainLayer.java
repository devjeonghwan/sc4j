package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the terrain of the map in plain colours: the engine default terrain {@link WorldLayer} for mods without tile pictures. Every tile the
 * viewport may show is filled with its {@link TerrainColors} colour as the screen outline of the tile through the projection, a rectangle
 * from above and a diamond in the isometric projection; the area outside the map stays as it is.
 */
public final class TerrainLayer implements WorldLayer {
    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;
    private final @NotNull TerrainColors colors;

    private final @NotNull VisibleCellRange cells;
    private final float @NotNull [] corners;

    /**
     * Creates a {@link TerrainLayer} instance.
     *
     * @param canvas the canvas the layer draws on
     * @param view   the game view
     * @param colors the terrain colours
     */
    public TerrainLayer(@NotNull Canvas canvas, @NotNull GameView view, @NotNull TerrainColors colors) {
        this.canvas = canvas;
        this.view   = view;
        this.colors = colors;

        this.cells   = new VisibleCellRange();
        this.corners = new float[WorldProjection.OUTLINE_LENGTH];
    }

    /**
     * Draws the layer for a frame: the colour of every tile the viewport may show.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        GridTopology tiles = this.view.getTerrain().getTileGrid();

        this.cells.update(viewport, tiles);

        for (int row = this.cells.getFirstRow(); row <= this.cells.getLastRow(); row++) {
            for (int column = this.cells.getFirstColumn(); column <= this.cells.getLastColumn(); column++) {
                this.cells.outlineCell(viewport, tiles, column, row, this.corners);
                this.canvas.fillQuadrilateral(this.corners, this.colors.getDrawColor(this.view.getTerrain(), column, row));
            }
        }
    }
}
