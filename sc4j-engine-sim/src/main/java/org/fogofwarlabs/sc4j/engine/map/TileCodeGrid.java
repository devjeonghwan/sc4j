package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Holds the tile code of every tile of a map of the engine map format: the one-character codes of its {@code terrain} rows, which the map
 * legend gives the terrain of and a {@link TilesetLegend} the pictures of. Tiles are numbered from the top left like the terrain tiles.
 */
public final class TileCodeGrid {
    private final int width;
    private final int height;
    private final char @NotNull [] codes;

    /**
     * Creates a {@link TileCodeGrid} instance from the rows of a map.
     *
     * @param rows the rows, top row first, each of the same length; copied
     * @throws IllegalArgumentException if there is no row, a row is empty or the rows differ in length
     */
    public TileCodeGrid(@NotNull List<@NotNull String> rows) {
        if (rows.isEmpty() || rows.getFirst().isEmpty()) {
            throw Exceptions.newIllegalArgumentException("A tile code grid must not be empty.");
        }

        int width = rows.getFirst().length();
        char[] codes = new char[width * rows.size()];

        for (int tileY = 0; tileY < rows.size(); tileY++) {
            String row = rows.get(tileY);

            if (row.length() != width) {
                throw Exceptions.newIllegalArgumentException("Row {0} has {1} tile codes, the first row {2}.", tileY, row.length(), width);
            }

            row.getChars(0, width, codes, tileY * width);
        }

        this.width  = width;
        this.height = rows.size();
        this.codes  = codes;
    }

    /**
     * @return the map width in tiles
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the map height in tiles
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Returns the code of a tile.
     *
     * @param tileX the horizontal tile
     * @param tileY the vertical tile
     * @return the tile code
     * @throws IndexOutOfBoundsException if the tile is outside the map
     */
    public char getCode(int tileX, int tileY) {
        if (tileX < 0 || tileX >= this.width || tileY < 0 || tileY >= this.height) {
            throw Exceptions.newIndexOutOfBoundsException("No tile {0}, {1} on a {2}x{3} map.", tileX, tileY, this.width, this.height);
        }

        return this.codes[tileY * this.width + tileX];
    }
}
