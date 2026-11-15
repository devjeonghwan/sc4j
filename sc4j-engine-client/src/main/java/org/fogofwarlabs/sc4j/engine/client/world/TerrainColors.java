package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;

/**
 * Colours the terrain of a map for the engine default world view and minimap, one colour per tile: walkable ground has the colour of its
 * elevation level (levels above the last colour use the last), ground units cannot stand on an unwalkable tile, which has the obstacle colour.
 * A tile takes the walk cell at its centre. Colours are packed {@code 0xAARRGGBB}, as the resource model stores pixels, except the draw
 * colours of {@link #getDrawColor}. The terrain image of the minimap shows the map through the client's projection.
 */
public final class TerrainColors {
    private static final int @NotNull [] DEFAULT_GROUND_COLORS = {0xFF4A6B2F, 0xFF6B7D3A, 0xFF8C8A4F};
    private static final int DEFAULT_OBSTACLE_COLOR = 0xFF2B2B33;

    private static final int ALPHA_SHIFT = 24;
    private static final float PIXEL_CENTER = 0.5f;

    /**
     * Finds the tile under the centre of each pixel of an image of a map's projected bounds, as {@link #createImage} draws it: the tile's
     * index {@code row * columns + column}, or {@code -1} where the pixel shows no part of the map.
     *
     * @param terrain    the terrain
     * @param projection the projection the image shows the map in
     * @param width      the image width in pixels
     * @param height     the image height in pixels
     * @return the tile indexes, row-major, top row first
     * @throws IllegalArgumentException if a dimension is not positive
     */
    public static int @NotNull [] findPixelTiles(@NotNull TerrainGrid terrain, @NotNull WorldProjection projection, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid image size {0}x{1}", width, height);
        }

        GridTopology tiles = terrain.getTileGrid();
        float left = projection.getProjectedLeft(tiles.getWorldWidth(), tiles.getWorldHeight());
        float top = projection.getProjectedTop(tiles.getWorldWidth(), tiles.getWorldHeight());
        float pixelWidth = (projection.getProjectedRight(tiles.getWorldWidth(), tiles.getWorldHeight()) - left) / width;
        float pixelHeight = (projection.getProjectedBottom(tiles.getWorldWidth(), tiles.getWorldHeight()) - top) / height;
        int[] pixelTiles = new int[width * height];

        for (int pixelY = 0; pixelY < height; pixelY++) {
            for (int pixelX = 0; pixelX < width; pixelX++) {
                float projectedX = left + (pixelX + PIXEL_CENTER) * pixelWidth;
                float projectedY = top + (pixelY + PIXEL_CENTER) * pixelHeight;
                int worldX = (int) Math.floor(projection.toWorldX(projectedX, projectedY));
                int worldY = (int) Math.floor(projection.toWorldY(projectedX, projectedY));
                int column = tiles.findColumn(worldX, worldY);
                int row = tiles.findRow(worldX, worldY);

                pixelTiles[pixelY * width + pixelX] = worldX >= 0 && worldY >= 0 && tiles.contains(column, row)
                                                      ? row * tiles.getColumnCount() + column
                                                      : -1;
            }
        }

        return pixelTiles;
    }


    private final int @NotNull [] groundColors;
    private final int obstacleColor;

    /**
     * Creates a {@link TerrainColors} instance with the default colours: three greens and olives by level and a dark grey for obstacles.
     */
    public TerrainColors() {
        this(DEFAULT_GROUND_COLORS, DEFAULT_OBSTACLE_COLOR);
    }

    /**
     * Creates a {@link TerrainColors} instance.
     *
     * @param groundColors  the ground colours by elevation level, low ground first, copied
     * @param obstacleColor the colour of unwalkable tiles
     * @throws IllegalArgumentException if no ground colour is given
     */
    public TerrainColors(int @NotNull [] groundColors, int obstacleColor) {
        if (groundColors.length == 0) {
            throw Exceptions.newIllegalArgumentException("Terrain colours need at least one ground colour.");
        }

        this.groundColors  = groundColors.clone();
        this.obstacleColor = obstacleColor;
    }

    /**
     * Returns the colour of one tile.
     *
     * @param terrain the terrain
     * @param tileX   the horizontal tile
     * @param tileY   the vertical tile
     * @return the colour, packed {@code 0xAARRGGBB}
     */
    public int getColor(@NotNull TerrainGrid terrain, int tileX, int tileY) {
        GridTopology tiles = terrain.getTileGrid();
        GridTopology walkCells = terrain.getWalkGrid();
        int centerX = tiles.getCenterX(tileX, tileY);
        int centerY = tiles.getCenterY(tileX, tileY);
        int walkX = walkCells.findColumn(centerX, centerY);
        int walkY = walkCells.findRow(centerX, centerY);

        if (!terrain.isWalkable(walkX, walkY)) {
            return this.obstacleColor;
        }

        return this.groundColors[Math.clamp(terrain.getElevation(walkX, walkY), 0, this.groundColors.length - 1)];
    }

    /**
     * Returns the colour of one tile as a draw colour of the {@link org.fogofwarlabs.sc4j.engine.client.render.Canvas}.
     *
     * @param terrain the terrain
     * @param tileX   the horizontal tile
     * @param tileY   the vertical tile
     * @return the colour, packed {@code 0xRRGGBBAA}
     */
    public int getDrawColor(@NotNull TerrainGrid terrain, int tileX, int tileY) {
        int color = this.getColor(terrain, tileX, tileY);

        return color << Byte.SIZE | color >>> ALPHA_SHIFT;
    }

    /**
     * Reads the width of the terrain image of a map in a projection: the width of the map's projected bounds in tile widths, rounded up.
     *
     * @param terrain    the terrain
     * @param projection the projection the image shows the map in
     * @return the width in pixels, at least {@code 1}
     */
    public int getImageWidth(@NotNull TerrainGrid terrain, @NotNull WorldProjection projection) {
        GridTopology tiles = terrain.getTileGrid();
        float width = projection.getProjectedRight(tiles.getWorldWidth(), tiles.getWorldHeight()) -
                      projection.getProjectedLeft(tiles.getWorldWidth(), tiles.getWorldHeight());

        return Math.max((int) Math.ceil(width / tiles.getCellWidth()), 1);
    }

    /**
     * Reads the height of the terrain image of a map in a projection: the height of the map's projected bounds in tile heights, rounded up.
     *
     * @param terrain    the terrain
     * @param projection the projection the image shows the map in
     * @return the height in pixels, at least {@code 1}
     */
    public int getImageHeight(@NotNull TerrainGrid terrain, @NotNull WorldProjection projection) {
        GridTopology tiles = terrain.getTileGrid();
        float height = projection.getProjectedBottom(tiles.getWorldWidth(), tiles.getWorldHeight()) -
                       projection.getProjectedTop(tiles.getWorldWidth(), tiles.getWorldHeight());

        return Math.max((int) Math.ceil(height / tiles.getCellHeight()), 1);
    }

    /**
     * Creates the image of a map's terrain as a projection shows it: {@link #getImageWidth} by {@link #getImageHeight} pixels over the map's
     * projected bounds, row-major, top row first. Each pixel has the colour of the tile under its centre; a pixel whose centre shows no part
     * of the map is transparent. From above, the image has one pixel per tile.
     *
     * @param terrain    the terrain
     * @param projection the projection the image shows the map in
     * @return the pixels, packed {@code 0xAARRGGBB}
     */
    public int @NotNull [] createImage(@NotNull TerrainGrid terrain, @NotNull WorldProjection projection) {
        return this.createImage(terrain, projection, this.getImageWidth(terrain, projection), this.getImageHeight(terrain, projection));
    }

    /**
     * Creates the image of a map's terrain as a projection shows it at a given size, for example the size of the minimap in framebuffer
     * pixels: the pixels of {@link #findPixelTiles}, each in the colour of its tile, transparent where it shows no part of the map.
     *
     * @param terrain    the terrain
     * @param projection the projection the image shows the map in
     * @param width      the image width in pixels
     * @param height     the image height in pixels
     * @return the pixels, packed {@code 0xAARRGGBB}
     * @throws IllegalArgumentException if a dimension is not positive
     */
    public int @NotNull [] createImage(@NotNull TerrainGrid terrain, @NotNull WorldProjection projection, int width, int height) {
        GridTopology tiles = terrain.getTileGrid();
        int[] pixelTiles = findPixelTiles(terrain, projection, width, height);
        int[] colors = new int[pixelTiles.length];

        for (int index = 0; index < pixelTiles.length; index++) {
            int tile = pixelTiles[index];

            if (tile >= 0) {
                colors[index] = this.getColor(terrain, tile % tiles.getColumnCount(), tile / tiles.getColumnCount());
            }
        }

        return colors;
    }
}
