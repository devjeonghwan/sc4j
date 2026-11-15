package org.fogofwarlabs.sc4j.engine.visibility;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

/**
 * Stores what each player sees of the map tiles as two words per tile, one bit per player index: the players that do not see the tile now and
 * those that have never seen it. The engine default {@link SightGrid}; it holds up to {@value #MAXIMUM_PLAYER_COUNT} players.
 * <p>
 * A grid created over a terrain also holds the elevation of every tile, the highest elevation of its walk cells: the tile flags carry one
 * elevation bit above the player bits (bit {@code 24 + elevation}), and the ground blockers of a viewer are the bits of the higher elevations
 * ({@link #getGroundBlockers}), so a ground viewer does not see past higher ground. A grid without a terrain has no blocking terrain bits.
 */
public final class TileVisibilityGrid implements SightGrid, StateHashContributor, SnapshotContributor {
    /**
     * The largest number of players a grid holds: one bit of a tile word per player, below the elevation bits.
     */
    public static final int MAXIMUM_PLAYER_COUNT = 24;
    /**
     * The number of elevation levels a grid tells apart; a higher terrain elevation counts as the highest level.
     */
    public static final int ELEVATION_LEVEL_COUNT = Integer.SIZE - MAXIMUM_PLAYER_COUNT;

    /**
     * The tile word in which no player sees or has seen the tile.
     */
    private static final int NO_PLAYER = -1;
    /**
     * The player bits of a tile word.
     */
    private static final int PLAYER_BITS = (1 << MAXIMUM_PLAYER_COUNT) - 1;
    /**
     * The ground blockers of a viewer at each elevation level: the bits of every higher level.
     */
    private static final int @NotNull [] GROUND_BLOCKERS = new int[ELEVATION_LEVEL_COUNT];

    static {
        for (int elevation = 0; elevation < ELEVATION_LEVEL_COUNT; elevation++) {
            for (int higher = elevation + 1; higher < ELEVATION_LEVEL_COUNT; higher++) {
                GROUND_BLOCKERS[elevation] |= 1 << (MAXIMUM_PLAYER_COUNT + higher);
            }
        }
    }

    private final int width;
    private final int height;
    private final int @NotNull [] elevationBits;

    private final int @NotNull [] unseen;
    private final int @NotNull [] unexplored;

    /**
     * Creates a {@link TileVisibilityGrid} instance without terrain in which no player sees or has seen any tile.
     *
     * @param width  the map width in tiles
     * @param height the map height in tiles
     * @throws IllegalArgumentException if a dimension is not positive
     */
    public TileVisibilityGrid(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw Exceptions.newIllegalArgumentException("A tile visibility grid requires positive dimensions: {0} x {1}", width, height);
        }

        this.width         = width;
        this.height        = height;
        this.elevationBits = new int[width * height];

        this.unseen     = new int[width * height];
        this.unexplored = new int[width * height];

        Arrays.fill(this.unseen, NO_PLAYER);
        Arrays.fill(this.unexplored, NO_PLAYER);
    }

    /**
     * Creates a {@link TileVisibilityGrid} instance over the tiles and elevations of a terrain in which no player sees or has seen any tile.
     *
     * @param terrain the terrain, read once
     * @throws IllegalArgumentException if a dimension is not positive
     */
    public TileVisibilityGrid(@NotNull TerrainGrid terrain) {
        this(terrain.getWidth(), terrain.getHeight());

        int cellsPerTile = terrain.getTileSize() / terrain.getWalkCellSize();

        for (int row = 0; row < this.height; row++) {
            for (int column = 0; column < this.width; column++) {
                int elevation = 0;

                for (int walkY = row * cellsPerTile; walkY < (row + 1) * cellsPerTile; walkY++) {
                    for (int walkX = column * cellsPerTile; walkX < (column + 1) * cellsPerTile; walkX++) {
                        elevation = Math.max(elevation, terrain.getElevation(walkX, walkY));
                    }
                }

                this.elevationBits[row * this.width + column] = 1 << (MAXIMUM_PLAYER_COUNT + Math.clamp(elevation, 0, ELEVATION_LEVEL_COUNT - 1));
            }
        }
    }

    /**
     * @return the map width in tiles
     */
    @Override
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the map height in tiles
     */
    @Override
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the flags a ground reveal assumes for a tile before it reveals it: no player sees the tile
     */
    @Override
    public int getNotVisibleFlags() {
        return NO_PLAYER;
    }

    /**
     * Returns the tile flag bits that block the sight of a ground reveal from an elevation: the bits of the tiles a viewer standing at that
     * elevation does not see past.
     * <p>
     * The bits of the elevation levels above the viewer's; a viewer at or above the highest level is blocked by none.
     *
     * @param elevation the elevation of the viewer
     * @return the blocking terrain bits, {@code 0} for none
     */
    @Override
    public int getGroundBlockers(int elevation) {
        return elevation >= 0 && elevation < ELEVATION_LEVEL_COUNT ? GROUND_BLOCKERS[elevation] : 0;
    }

    /**
     * Reveals a tile to players: they see it now and have explored it.
     *
     * @param tileIndex  the tile index
     * @param playerMask the players, one bit per player index
     * @return the tile flags afterwards, which the sight test of the farther tiles reads
     */
    @Override
    public int revealTile(int tileIndex, int playerMask) {
        this.unexplored[tileIndex] &= ~playerMask;
        this.unseen[tileIndex] &= ~playerMask;

        return this.unseen[tileIndex] & PLAYER_BITS | this.elevationBits[tileIndex];
    }

    /**
     * Checks whether a player sees a tile now.
     *
     * @param tileIndex   the tile index
     * @param playerIndex the player index
     * @return {@code true} if the player sees the tile, {@code false} otherwise
     */
    public boolean isVisible(int tileIndex, int playerIndex) {
        return (this.unseen[tileIndex] & 1 << playerIndex) == 0;
    }

    /**
     * Checks whether a player has ever seen a tile.
     *
     * @param tileIndex   the tile index
     * @param playerIndex the player index
     * @return {@code true} if the player has explored the tile, {@code false} otherwise
     */
    public boolean isExplored(int tileIndex, int playerIndex) {
        return (this.unexplored[tileIndex] & 1 << playerIndex) == 0;
    }

    /**
     * Hides every tile from every player, keeping what they explored: the start of a sight update.
     */
    public void hideAll() {
        Arrays.fill(this.unseen, NO_PLAYER);
    }

    /**
     * Mixes this part of the simulation state into the hash: the unseen words, then the unexplored words, in tile order.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        hash.mixArray(this.unseen);
        hash.mixArray(this.unexplored);
    }

    /**
     * Writes this part of the simulation state: the unseen and the unexplored masks of every tile.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.unseen);
        writer.writeInts(this.unexplored);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another number of values
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.unseen);
        reader.readInts(this.unexplored);
    }
}
