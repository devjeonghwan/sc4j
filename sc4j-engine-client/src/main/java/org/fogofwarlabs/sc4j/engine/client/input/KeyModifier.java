package org.fogofwarlabs.sc4j.engine.client.input;

/**
 * Names a modifier of a key or pointer button event. An event holds its modifiers as one mask of {@link #getMask()} bits
 * ({@link InputEvent#getModifiers()}): the bits follow the declaration order, {@link #SHIFT} is bit {@code 0}.
 */
public enum KeyModifier {
    /**
     * Either Shift key is held.
     */
    SHIFT,
    /**
     * Either Control key is held.
     */
    CONTROL,
    /**
     * Either Alt key is held.
     */
    ALT,
    /**
     * Either Super (Windows, Command) key is held.
     */
    SUPER,
    /**
     * Caps Lock is on.
     */
    CAPS_LOCK,
    /**
     * Num Lock is on.
     */
    NUM_LOCK;

    /**
     * The mask of every modifier.
     */
    public static final int ALL_MASK = (1 << values().length) - 1;

    /**
     * @return the bit of the modifier in a modifier mask
     */
    public int getMask() {
        return 1 << this.ordinal();
    }

    /**
     * Returns whether a modifier mask holds this modifier.
     *
     * @param modifiers the modifier mask
     * @return {@code true} if the mask holds the modifier's bit, {@code false} otherwise
     */
    public boolean isIn(int modifiers) {
        return (modifiers & this.getMask()) != 0;
    }
}
