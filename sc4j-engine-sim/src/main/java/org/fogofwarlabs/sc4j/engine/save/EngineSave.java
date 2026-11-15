package org.fogofwarlabs.sc4j.engine.save;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Holds one saved game of the engine save format ({@link EngineSaveFormat}): the identity of the mod that saved it, the setup the game was
 * created from and the state snapshot of the game when it was saved. {@link EngineSaveFormat#createSave} creates it from a running game,
 * {@link EngineSaveFormat#read} from a file; the mod creates a game from {@link #getSetup()} the way it creates any game and {@link #restore}s
 * the saved state into it.
 * <p>
 * A saved game is immutable.
 */
public final class EngineSave {
    private final @NotNull ModIdentity identity;
    private final @NotNull GameSetup setup;
    private final @NotNull StateSnapshot snapshot;

    /**
     * Creates an {@link EngineSave} instance.
     *
     * @param identity the identity of the mod that saved the game
     * @param setup    the setup of the game
     * @param snapshot the state of the game when it was saved
     */
    EngineSave(@NotNull ModIdentity identity, @NotNull GameSetup setup, @NotNull StateSnapshot snapshot) {
        this.identity = identity;
        this.setup    = setup;
        this.snapshot = snapshot;
    }

    /**
     * @return the identity of the mod that saved the game
     */
    public @NotNull ModIdentity getIdentity() {
        return this.identity;
    }

    /**
     * @return the setup the game was created from; the loading client creates its game from it
     */
    public @NotNull GameSetup getSetup() {
        return this.setup;
    }

    /**
     * @return the state of the game when it was saved
     */
    public @NotNull StateSnapshot getSnapshot() {
        return this.snapshot;
    }


    /**
     * Restores the saved state into a game created from {@link #getSetup()} ({@link StateSnapshot#restore}); the game then runs on from the
     * saved frame.
     *
     * @param game the game
     * @throws IOException if the saved state does not fit the game or does not reach the saved state hash
     */
    public void restore(@NotNull Game game) throws IOException {
        this.snapshot.restore(game);
    }
}
