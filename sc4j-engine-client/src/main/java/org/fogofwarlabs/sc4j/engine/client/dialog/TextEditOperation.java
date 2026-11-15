package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.input.KeyModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Names the edit operations of a {@link TextEditModel}, in the order of an edit menu: select all, cut, copy, paste, undo and redo.
 */
public enum TextEditOperation {
    /**
     * Selects all text.
     */
    SELECT_ALL,
    /**
     * Cuts the selected text.
     */
    CUT,
    /**
     * Copies the selected text.
     */
    COPY,
    /**
     * Pastes the clipboard text.
     */
    PASTE,
    /**
     * Restores the previous edit.
     */
    UNDO,
    /**
     * Reapplies an undone edit.
     */
    REDO;

    /**
     * Returns the edit operation of a key, the one key mapping every text input uses: Ctrl+A, Ctrl+X, Ctrl+C, Ctrl+V, Ctrl+Z and
     * Ctrl+Shift+Z.
     *
     * @param key       the key
     * @param modifiers the modifier mask ({@link KeyModifier#getMask()} bits)
     * @return the operation, or {@code null} when the key is none
     */
    public static @Nullable TextEditOperation ofKey(@NotNull Key key, int modifiers) {
        int held = modifiers & (KeyModifier.SHIFT.getMask() | KeyModifier.CONTROL.getMask() | KeyModifier.ALT.getMask());

        if (held == (KeyModifier.CONTROL.getMask() | KeyModifier.SHIFT.getMask())) {
            return key == Key.Z ? REDO : null;
        }

        if (held != KeyModifier.CONTROL.getMask()) {
            return null;
        }

        return switch (key) {
            case A -> SELECT_ALL;
            case X -> CUT;
            case C -> COPY;
            case V -> PASTE;
            case Z -> UNDO;
            default -> null;
        };
    }
}
