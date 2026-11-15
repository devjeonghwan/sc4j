package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Gives the pictures of each tile code of the engine map format in a tileset's
 * {@link org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics}: one or more tile indices per code, the variants of that terrain.
 * {@link #selectTile} picks the variant of a map tile from its position only, so every view of a map shows the same pictures.
 */
public final class TilesetLegend {
    /**
     * The multiplier of the horizontal tile in the variant hash.
     */
    private static final int HORIZONTAL_HASH_FACTOR = 0x9E3779B1;
    /**
     * The multiplier of the vertical tile in the variant hash.
     */
    private static final int VERTICAL_HASH_FACTOR = 0x85EBCA77;
    /**
     * The shift that mixes the high bits of the variant hash into the low ones.
     */
    private static final int HASH_MIX_SHIFT = 15;

    private final @NotNull String codes;
    private final int @NotNull [] @NotNull [] tiles;

    /**
     * Creates a {@link TilesetLegend} instance.
     *
     * @param tiles the tile indices of each code, at least one per code, each at least {@code 0}; copied
     * @throws IllegalArgumentException if a code has no tile index or a negative one
     */
    public TilesetLegend(@NotNull Map<@NotNull Character, int @NotNull []> tiles) {
        Map<Character, int[]> sorted = new TreeMap<>(tiles);
        StringBuilder codes = new StringBuilder(sorted.size());
        int[][] variants = new int[sorted.size()][];

        for (Map.Entry<Character, int[]> entry : sorted.entrySet()) {
            int[] indices = entry.getValue();

            if (indices.length == 0 || Arrays.stream(indices).anyMatch(index -> index < 0)) {
                throw Exceptions.newIllegalArgumentException("The tile code {0} needs tile indices of at least 0: {1}", entry.getKey(),
                                                             Arrays.toString(indices));
            }

            variants[codes.length()] = indices.clone();
            codes.append(entry.getKey().charValue());
        }

        this.codes = codes.toString();
        this.tiles = variants;
    }

    /**
     * @return the tile codes the legend has pictures for, in ascending order
     */
    public @NotNull String getCodes() {
        return this.codes;
    }

    /**
     * @param code the tile code
     * @return {@code true} if the legend has pictures for the code, {@code false} otherwise
     */
    public boolean hasCode(char code) {
        return this.codes.indexOf(code) >= 0;
    }

    /**
     * Returns the number of variants of a tile code.
     *
     * @param code the tile code
     * @return the number of tile indices of the code, at least {@code 1}
     * @throws IllegalArgumentException if the legend has no pictures for the code
     */
    public int getVariantCount(char code) {
        return this.requireTiles(code).length;
    }

    /**
     * Returns one variant of a tile code.
     *
     * @param code    the tile code
     * @param variant the variant, {@code 0} up to {@link #getVariantCount} minus one
     * @return the tile index of the variant
     * @throws IllegalArgumentException  if the legend has no pictures for the code
     * @throws IndexOutOfBoundsException if the code has no such variant
     */
    public int getTile(char code, int variant) {
        int[] indices = this.requireTiles(code);

        if (variant < 0 || variant >= indices.length) {
            throw Exceptions.newIndexOutOfBoundsException("The tile code {0} has no variant {1}; it has {2}.", code, variant, indices.length);
        }

        return indices[variant];
    }

    /**
     * Selects the picture of a map tile: the variant of its code that a hash of the tile position picks.
     *
     * @param code  the tile code
     * @param tileX the horizontal tile
     * @param tileY the vertical tile
     * @return the tile index
     * @throws IllegalArgumentException if the legend has no pictures for the code
     */
    public int selectTile(char code, int tileX, int tileY) {
        int[] indices = this.requireTiles(code);
        int hash = tileX * HORIZONTAL_HASH_FACTOR ^ tileY * VERTICAL_HASH_FACTOR;

        hash ^= hash >>> HASH_MIX_SHIFT;

        return indices[Integer.remainderUnsigned(hash, indices.length)];
    }

    private int @NotNull [] requireTiles(char code) {
        int position = this.codes.indexOf(code);

        if (position < 0) {
            throw Exceptions.newIllegalArgumentException("The tileset has no pictures for the tile code {0}.", code);
        }

        return this.tiles[position];
    }
}
