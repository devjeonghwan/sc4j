package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs one controller per computer slot of the setup in slot order: the engine default {@link ComputerPlayerSystem}. When it is attached
 * it creates, with the mod's {@link ComputerControllerFactory}, the controller of every player slot whose controller kind is
 * {@link ControllerKind#COMPUTER}, in ascending slot index order; each frame of its schedule it runs every controller once, in that order.
 * <p>
 * The state hash mixes every controller that is a {@link StateHashContributor}, and the state snapshot holds every controller that is a
 * {@link SnapshotContributor} (a controller with state between frames is both), in the order the controllers run.
 * <p>
 * Determinism contract: the controllers run in ascending slot index order; the system draws no random value itself and allocates nothing
 * per frame.
 */
public final class StandardComputerPlayerSystem implements ComputerPlayerSystem, StateHashContributor, SnapshotContributor {
    private final @NotNull ComputerControllerFactory factory;

    private @NotNull ComputerController @NotNull [] controllers;
    private @Nullable ComputerController @NotNull [] controllersByPlayer;

    /**
     * Creates a {@link StandardComputerPlayerSystem} instance; it holds no controller until it is attached.
     *
     * @param factory creates the controller of each computer slot when the system is attached
     */
    public StandardComputerPlayerSystem(@NotNull ComputerControllerFactory factory) {
        this.factory = factory;

        this.controllers         = new ComputerController[0];
        this.controllersByPlayer = new ComputerController[0];
    }

    /**
     * @return the controllers of the computer slots, in the order they run (ascending slot index); a copy
     */
    public @NotNull List<@NotNull ComputerController> getControllers() {
        return List.of(this.controllers);
    }

    /**
     * Finds the controller of a player.
     *
     * @param player the player
     * @return the controller that plays the player, or {@code null} if no computer plays it
     */
    @Override
    public @Nullable ComputerController getController(@NotNull Player player) {
        int index = player.getIndex();

        return index < this.controllersByPlayer.length ? this.controllersByPlayer[index] : null;
    }

    /**
     * Resolves what this system uses from the game. Called once, after every system exists.
     * <p>
     * Creates the controller of every computer slot of the setup, in ascending slot index order.
     *
     * @param game the game
     * @throws IllegalStateException if the factory returns a controller of another player
     */
    @Override
    public void attach(@NotNull Game game) {
        List<ComputerController> created = new ArrayList<>();
        ComputerController[] byPlayer = new ComputerController[game.getPlayerCount()];

        for (PlayerSlot slot : game.getPlayers()) {
            if (slot.getControllerKind() != ControllerKind.COMPUTER || slot.getSlotIndex() >= byPlayer.length) {
                continue;
            }

            Player player = game.getPlayer(slot.getSlotIndex());
            ComputerController controller = this.factory.create(game, player);

            if (controller.getPlayer() != player) {
                throw Exceptions.newIllegalStateException("The computer controller of {0} plays {1}.", player, controller.getPlayer());
            }

            created.add(controller);
            byPlayer[slot.getSlotIndex()] = controller;
        }

        this.controllers         = created.toArray(new ComputerController[0]);
        this.controllersByPlayer = byPlayer;
    }

    /**
     * Runs the computer players of the current frame: every controller once, in ascending slot index order.
     *
     * @param game the game
     */
    @Override
    public void run(@NotNull Game game) {
        ComputerController[] current = this.controllers;

        for (int index = 0; index < current.length; index++) {
            current[index].update(game);
        }
    }

    /**
     * Mixes this part of the simulation state into the hash: every controller that is a {@link StateHashContributor}, in the order they run.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        for (ComputerController controller : this.controllers) {
            if (controller instanceof StateHashContributor contributor) {
                contributor.contributeStateHash(hash);
            }
        }
    }

    /**
     * Writes this part of the simulation state: the number of controllers, then every controller that is a {@link SnapshotContributor}, in
     * the order they run.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.controllers.length);

        for (ComputerController controller : this.controllers) {
            if (controller instanceof SnapshotContributor contributor) {
                contributor.writeSnapshot(writer);
            }
        }
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another number of controllers
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readLength(this.controllers.length);

        for (ComputerController controller : this.controllers) {
            if (controller instanceof SnapshotContributor contributor) {
                contributor.readSnapshot(reader);
            }
        }
    }
}
