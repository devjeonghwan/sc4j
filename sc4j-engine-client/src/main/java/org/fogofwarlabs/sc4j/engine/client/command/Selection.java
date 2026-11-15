package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Shows the units a local player has selected, in selection order, each unit once and at most {@link #getCapacity} of them. Commands of the
 * player's input go to the selected units ({@link CommandTranslator}); the HUD shows them.
 * <p>
 * This is what the input and the HUD read. Who changes the selection is the client's choice: the engine default {@link OrderedSelection}
 * is changed by the client's own input; a mod whose selection is synchronized game state (changed by selection commands) shows that state
 * through its own implementation.
 */
public interface Selection {
    /**
     * @return the most units the selection holds
     */
    int getCapacity();

    /**
     * @return the selected units in selection order, a read-only list
     */
    @NotNull List<Unit> getUnits();

    /**
     * @return the number of selected units
     */
    int getSize();

    /**
     * @return {@code true} if no unit is selected, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * @return {@code true} if the selection holds {@link #getCapacity} units, {@code false} otherwise
     */
    boolean isFull();

    /**
     * Tells whether a unit is selected.
     *
     * @param unit the unit
     * @return {@code true} if the unit is selected, {@code false} otherwise
     */
    boolean contains(@NotNull Unit unit);
}
