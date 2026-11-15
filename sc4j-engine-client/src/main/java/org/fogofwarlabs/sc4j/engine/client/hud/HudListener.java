package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.command.CommandRefusal;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Receives what the player does with the HUD of the game screen and what holds the player's production back, for the mod's feedback: a button
 * pressed (for example a click sound), units selected (for example an acknowledgement sound), an action refused and a producer that starts
 * waiting for supply (for example a message and a sound).
 */
public interface HudListener {
    /**
     * Called when a press on a HUD button runs it: a command card button or a queue slot.
     */
    default void onButtonPressed() {
    }

    /**
     * Called when the player's input (a click, a selection box, a selection panel cell, a key) changed the selection and units are selected;
     * a selection the game changes (a unit that died) is not reported.
     *
     * @param units the selected units in selection order, a read-only view that changes with the selection
     */
    default void onUnitsSelected(@NotNull List<@NotNull Unit> units) {
    }

    /**
     * Called when the player tries to run an action that is refused now, by a button or a hotkey.
     *
     * @param refusal the refusal
     * @param message the refusal as a short message line, for example {@code Not enough Gold.}
     */
    default void onCommandRefused(@NotNull CommandRefusal refusal, @NotNull String message) {
    }

    /**
     * Called when the front entry of a producer of the local player starts waiting for supply
     * ({@link org.fogofwarlabs.sc4j.engine.client.view.UnitView#isWaitingForSupply}); the screen calls it once per producer and wait, for at
     * most one producer per update.
     *
     * @param producer the producer
     * @param refusal  the refusal of the wait, {@link CommandRefusal#MISSING_SUPPLY}
     * @param message  the wait as a short message line, for example {@code Not enough Supply.}
     */
    default void onProductionWaiting(@NotNull Unit producer, @NotNull CommandRefusal refusal, @NotNull String message) {
    }
}
