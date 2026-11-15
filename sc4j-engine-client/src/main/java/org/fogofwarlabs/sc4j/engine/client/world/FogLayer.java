package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.QuadrilateralUtils;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the fog of war of the local player: the engine default fog {@link WorldLayer}. Each shown tile the player has never seen is covered
 * with the unexplored colour, each explored tile the player does not see now with the fog colour; a tile takes the visibility of its centre.
 * An opaque cover grows by {@value #COVER_PIXELS} framebuffer pixels on every side ({@link QuadrilateralUtils#expand}), so the edge pixels of a
 * terrain picture drawn in the box of its tile never show beside it, also at the map's edge. An observer without local player sees the whole
 * map.
 */
public final class FogLayer implements WorldLayer {
    /**
     * The default colour of unexplored tiles, packed {@code 0xRRGGBBAA}.
     */
    public static final int DEFAULT_UNEXPLORED_COLOR = 0x000000FF;
    /**
     * The default colour of explored tiles out of sight, packed {@code 0xRRGGBBAA}.
     */
    public static final int DEFAULT_FOG_COLOR = 0x00000080;
    /**
     * The framebuffer pixels an opaque cover grows by on every side.
     */
    public static final float COVER_PIXELS = 2;

    private static final int ALPHA_MASK = 0xFF;

    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;
    private final int unexploredColor;
    private final int fogColor;

    private final @NotNull VisibleCellRange cells;
    private final float @NotNull [] corners;

    /**
     * Creates a {@link FogLayer} instance with the default colours.
     *
     * @param canvas the canvas the layer draws on
     * @param view   the game view
     */
    public FogLayer(@NotNull Canvas canvas, @NotNull GameView view) {
        this(canvas, view, DEFAULT_UNEXPLORED_COLOR, DEFAULT_FOG_COLOR);
    }

    /**
     * Creates a {@link FogLayer} instance.
     *
     * @param canvas          the canvas the layer draws on
     * @param view            the game view
     * @param unexploredColor the colour of unexplored tiles, packed {@code 0xRRGGBBAA}
     * @param fogColor        the colour of explored tiles out of sight, packed {@code 0xRRGGBBAA}
     */
    public FogLayer(@NotNull Canvas canvas, @NotNull GameView view, int unexploredColor, int fogColor) {
        this.canvas          = canvas;
        this.view            = view;
        this.unexploredColor = unexploredColor;
        this.fogColor        = fogColor;

        this.cells   = new VisibleCellRange();
        this.corners = new float[WorldProjection.OUTLINE_LENGTH];
    }

    /**
     * Draws the layer for a frame: the fog over the shown tiles.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        Player player = this.view.getLocalPlayer();

        if (player == null) {
            return;
        }

        GridTopology tiles = this.view.getTerrain().getTileGrid();
        float cover = COVER_PIXELS / this.canvas.getPixelScale();

        this.cells.update(viewport, tiles);

        for (int tileY = this.cells.getFirstRow(); tileY <= this.cells.getLastRow(); tileY++) {
            for (int tileX = this.cells.getFirstColumn(); tileX <= this.cells.getLastColumn(); tileX++) {
                int x = tiles.getCenterX(tileX, tileY);
                int y = tiles.getCenterY(tileX, tileY);

                if (this.view.isVisible(player, x, y)) {
                    continue;
                }

                int color = this.view.isExplored(player, x, y) ? this.fogColor : this.unexploredColor;

                this.cells.outlineCell(viewport, tiles, tileX, tileY, this.corners);

                if ((color & ALPHA_MASK) == ALPHA_MASK) {
                    QuadrilateralUtils.expand(this.corners, cover);
                }

                this.canvas.fillQuadrilateral(this.corners, color);
            }
        }
    }
}
