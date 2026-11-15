package org.fogofwarlabs.sc4j.engine.visibility;

/**
 * Stores what each player sees of the map tiles: the storage the sight algorithm ({@link SightDescriptors#reveal}) writes. A tile's flags hold
 * one bit per player that does not see it now, at bit {@code playerIndex}, and may hold further bits that block sight (the terrain heights a
 * ground viewer does not see past, {@link #getGroundBlockers}). The engine default storage is {@link TileVisibilityGrid}; a mod may keep its own
 * layout.
 * <p>
 * Tiles are numbered row by row from the top left: {@code row * getWidth() + column}.
 */
public interface SightGrid {
    /**
     * @return the map width in tiles
     */
    int getWidth();

    /**
     * @return the map height in tiles
     */
    int getHeight();

    /**
     * @return the flags a ground reveal assumes for a tile before it reveals it: no player sees the tile
     */
    int getNotVisibleFlags();

    /**
     * Returns the tile flag bits that block the sight of a ground reveal from an elevation: the bits of the tiles a viewer standing at that
     * elevation does not see past.
     *
     * @param elevation the elevation of the viewer
     * @return the blocking terrain bits, {@code 0} for none
     */
    int getGroundBlockers(int elevation);

    /**
     * Reveals a tile to players: they see it now and have explored it.
     *
     * @param tileIndex  the tile index
     * @param playerMask the players, one bit per player index
     * @return the tile flags afterwards, which the sight test of the farther tiles reads
     */
    int revealTile(int tileIndex, int playerMask);
}
