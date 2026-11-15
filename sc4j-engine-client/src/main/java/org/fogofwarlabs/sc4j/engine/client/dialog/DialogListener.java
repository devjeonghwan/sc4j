package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.NotNull;

/**
 * Receives what the user does with a {@link Dialog}: the mod's dialog procedure.
 */
public interface DialogListener {
    /**
     * Called when a control is activated: a released press, Enter, Escape or a hotkey.
     *
     * @param dialog  the dialog
     * @param control the activated control
     */
    void onActivated(@NotNull Dialog dialog, @NotNull Control control);

    /**
     * Called when the pointer moves onto a control that takes it, for example to play a hover sound. The default does nothing.
     *
     * @param dialog  the dialog
     * @param control the hovered control
     */
    default void onHovered(@NotNull Dialog dialog, @NotNull Control control) {
    }

    /**
     * Called when Escape reaches a dialog without a cancel control.
     *
     * @param dialog the dialog
     */
    default void onCancelled(@NotNull Dialog dialog) {
    }
}
