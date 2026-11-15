package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Names the keys that dialogs and their controls act on; typed characters reach them separately as code points.
 */
public enum DialogKey {
    /**
     * Activates the focused button or the dialog's default control.
     */
    ENTER,
    /**
     * Activates the dialog's cancel control.
     */
    ESCAPE,
    /**
     * Moves the focus to the next focusable control.
     */
    TAB,
    /**
     * Deletes the character before the cursor of a text input.
     */
    BACKSPACE,
    /**
     * Deletes the character after the cursor of a text input.
     */
    DELETE,
    /**
     * Moves a cursor or a value left.
     */
    LEFT,
    /**
     * Moves a cursor or a value right.
     */
    RIGHT,
    /**
     * Moves a selection up.
     */
    UP,
    /**
     * Moves a selection down.
     */
    DOWN,
    /**
     * Moves a cursor or a selection to the start.
     */
    HOME,
    /**
     * Moves a cursor or a selection to the end.
     */
    END,
    /**
     * Moves a selection up by a page.
     */
    PAGE_UP,
    /**
     * Moves a selection down by a page.
     */
    PAGE_DOWN;

    /**
     * Finds the dialog key of an input key: both Enter keys are {@link #ENTER}.
     *
     * @param key the input key
     * @return the dialog key, or {@code null} if dialogs do not act on the key
     */
    public static @Nullable DialogKey find(@NotNull Key key) {
        return switch (key) {
            case ENTER, KEYPAD_ENTER -> ENTER;
            case ESCAPE -> ESCAPE;
            case TAB -> TAB;
            case BACKSPACE -> BACKSPACE;
            case DELETE -> DELETE;
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            case UP -> UP;
            case DOWN -> DOWN;
            case HOME -> HOME;
            case END -> END;
            case PAGE_UP -> PAGE_UP;
            case PAGE_DOWN -> PAGE_DOWN;
            default -> null;
        };
    }
}
