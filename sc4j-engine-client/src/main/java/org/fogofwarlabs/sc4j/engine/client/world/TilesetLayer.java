package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.*;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.map.TileCodeGrid;
import org.fogofwarlabs.sc4j.engine.map.TilesetLegend;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelFormat;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the terrain of a map with the pictures of a tileset: the engine default terrain {@link WorldLayer} of a mod with tile pictures. Every
 * tile the viewport may show draws the picture its tile code selects ({@link TilesetLegend#selectTile}) into the box of the tile's screen
 * outline through the projection, so square pictures fit the top-down projection and diamond pictures ({@link
 * org.fogofwarlabs.sc4j.engine.resource.graphics.TileShape#DIAMOND}) the isometric one; the transparent corners of a diamond picture show the
 * neighbouring tiles drawn before it. Tiles draw row by row, top row first. A code the legend does not know leaves its tile undrawn.
 * <p>
 * The tile pictures may have detail pictures ({@link DetailSet}) with the same tiles at a multiple of the tile size; a tile is drawn from the
 * detail of the canvas's pixel scale times the canvas units per tile pixel of its box. The pictures are uploaded to the canvas one by one when
 * a tile first shows them, and released by {@link #close}.
 */
public final class TilesetLayer implements WorldLayer, AutoCloseable {
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;
    private final @NotNull DetailSet<TileGraphics> graphics;
    private final @NotNull TilesetLegend legend;
    private final @NotNull TileCodeGrid codes;

    private final @NotNull VisibleCellRange cells;
    private final float @NotNull [] corners;
    /**
     * The uploaded tile pictures: row {@code 0} the tileset's own, row {@code n} those of detail {@code n - 1}.
     */
    private final @Nullable Texture @NotNull [] @NotNull [] textures;

    /**
     * Creates a {@link TilesetLayer} instance; it uploads nothing until it draws.
     *
     * @param canvas   the canvas the layer draws on
     * @param view     the game view
     * @param graphics the tile pictures, RGBA, with their detail pictures
     * @param legend   the pictures of each tile code
     * @param codes    the tile code of every tile of the map, of the terrain's size
     * @throws IllegalArgumentException if the pictures are not RGBA or the codes do not cover the terrain
     */
    public TilesetLayer(@NotNull Canvas canvas, @NotNull GameView view,
                        @NotNull DetailSet<TileGraphics> graphics, @NotNull TilesetLegend legend, @NotNull TileCodeGrid codes) {
        PixelFormat format = graphics.getResource().getPixels().getFormat();

        if (format != PixelFormat.RGBA_8888) {
            throw Exceptions.newIllegalArgumentException("A tileset layer draws RGBA tile pictures, not {0}", format);
        }

        if (codes.getWidth() != view.getTerrain().getWidth() || codes.getHeight() != view.getTerrain().getHeight()) {
            throw Exceptions.newIllegalArgumentException("The tile codes ({0}x{1}) do not cover the terrain ({2}x{3})",
                                                         codes.getWidth(), codes.getHeight(),
                                                         view.getTerrain().getWidth(), view.getTerrain().getHeight());
        }

        this.canvas   = canvas;
        this.view     = view;
        this.graphics = graphics;
        this.legend   = legend;
        this.codes    = codes;

        this.cells    = new VisibleCellRange();
        this.corners  = new float[WorldProjection.OUTLINE_LENGTH];
        this.textures = new Texture[graphics.getDetailCount() + 1][graphics.getResource().getTileCount()];
    }

    /**
     * Draws the layer for a frame: the picture of every tile the viewport may show.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        GridTopology tiles = this.view.getTerrain().getTileGrid();
        float pixelScale = this.canvas.getPixelScale();
        int tileWidth = this.graphics.getResource().getTileWidth();

        this.cells.update(viewport, tiles);

        for (int row = this.cells.getFirstRow(); row <= this.cells.getLastRow(); row++) {
            for (int column = this.cells.getFirstColumn(); column <= this.cells.getLastColumn(); column++) {
                char code = this.codes.getCode(column, row);

                if (!this.legend.hasCode(code)) {
                    continue;
                }

                this.cells.outlineCell(viewport, tiles, column, row, this.corners);

                float width = QuadrilateralUtils.getWidth(this.corners);
                int detail = this.graphics.findDetail(pixelScale * width / tileWidth);

                this.canvas.drawTexture(this.getTexture(detail, this.legend.selectTile(code, column, row)),
                                        QuadrilateralUtils.getLeft(this.corners), QuadrilateralUtils.getTop(this.corners),
                                        width, QuadrilateralUtils.getHeight(this.corners), OPAQUE_WHITE);
            }
        }
    }

    /**
     * Returns the texture of a tile picture, uploading it at the first call.
     *
     * @param detail the index of the detail the picture comes from, or {@code -1} for the tileset's own pictures
     * @param tile   the tile index
     * @return the texture
     */
    private @NotNull Texture getTexture(int detail, int tile) {
        Texture[] detailTextures = this.textures[detail + 1];
        Texture texture = detailTextures[tile];

        if (texture == null) {
            TileGraphics drawn = detail < 0 ? this.graphics.getResource() : this.graphics.getDetail(detail);
            PixelLayer pixels = drawn.getPixels();
            int offset = drawn.getTileOffset(tile);
            int[] colors = new int[drawn.getTileWidth() * drawn.getTileHeight()];

            for (int index = 0; index < colors.length; index++) {
                colors[index] = pixels.getValue(offset + index);
            }

            texture              = this.canvas.uploadTexture(drawn.getTileWidth(), drawn.getTileHeight(), colors);
            detailTextures[tile] = texture;
        }

        return texture;
    }

    /**
     * Disposes resources: releases the uploaded tile pictures.
     */
    @Override
    public void close() {
        for (Texture[] detailTextures : this.textures) {
            for (int tile = 0; tile < detailTextures.length; tile++) {
                Texture texture = detailTextures[tile];

                detailTextures[tile] = null;

                if (texture != null) {
                    texture.close();
                }
            }
        }
    }
}
