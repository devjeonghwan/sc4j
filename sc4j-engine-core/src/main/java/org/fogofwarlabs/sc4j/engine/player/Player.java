package org.fogofwarlabs.sc4j.engine.player;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one player of a running game: the owner of units, the holder of resources and progress, the one who sees.
 * The game creates one instance per player slot of its content (setup players and the slots a mod adds, such as a
 * neutral player) when it is created; instances compare by identity and live as long as the game.
 * <p>
 * The player is an identity, not a store: what a player owns and has researched lives in the systems and tables of the
 * game, which use {@link #getIndex()} to address their arrays.
 */
public final class Player {
    private final int index;

    /**
     * Creates a {@link Player} instance. The game creates its players; a test may create its own.
     *
     * @param index the player slot index, {@code 0} up to the player count minus one
     */
    public Player(int index) {
        this.index = index;
    }

    /**
     * @return the player slot index, the position of this player in the arrays of the game's tables
     */
    public int getIndex() {
        return this.index;
    }


    /**
     * @return the player slot index, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return "Player " + this.index;
    }
}
