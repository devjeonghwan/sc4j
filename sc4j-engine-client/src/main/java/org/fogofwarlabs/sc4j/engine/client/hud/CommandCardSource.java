package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Creates the buttons of the {@link CommandCard} for the local player's selection; the game screen asks again whenever the selection or the
 * game changes what the selected units can do.
 */
@FunctionalInterface
public interface CommandCardSource {
    /**
     * Creates the buttons for a selection.
     *
     * @param player    the local player
     * @param selection the selected units in selection order, not empty
     * @return the buttons in grid order
     */
    @NotNull List<@NotNull CommandButton> createButtons(@NotNull Player player, @NotNull List<@NotNull Unit> selection);
}
