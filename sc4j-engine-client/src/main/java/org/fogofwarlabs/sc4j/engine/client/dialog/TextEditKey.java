package org.fogofwarlabs.sc4j.engine.client.dialog;

/**
 * Names the editing keys of a {@link TextEditModel}, beside the typed characters.
 */
public enum TextEditKey {
    /**
     * Removes the selection, or the character before the caret.
     */
    BACKSPACE,
    /**
     * Removes the selection, or the character after the caret.
     */
    DELETE,
    /**
     * Moves the caret one character left, or to the start of the selection.
     */
    LEFT,
    /**
     * Moves the caret one character right, or to the end of the selection.
     */
    RIGHT,
    /**
     * Moves the caret to the beginning.
     */
    HOME,
    /**
     * Moves the caret to the end.
     */
    END
}
