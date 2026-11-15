package org.fogofwarlabs.sc4j.engine.client.input;

import org.jetbrains.annotations.NotNull;

/**
 * Binds one key with its modifiers to an action of a {@link KeyBindingTable}.
 *
 * @param <A> the action class
 */
public final class KeyBinding<A> {
    private final @NotNull Key key;
    private final int modifiers;
    private final @NotNull A action;
    private final boolean repeatable;

    /**
     * Creates a {@link KeyBinding} instance.
     *
     * @param key        the key
     * @param modifiers  the modifier mask the key needs ({@link KeyModifier#getMask()} bits)
     * @param action     the bound action
     * @param repeatable whether a key repeat also produces the action
     */
    KeyBinding(@NotNull Key key, int modifiers, @NotNull A action, boolean repeatable) {
        this.key        = key;
        this.modifiers  = modifiers;
        this.action     = action;
        this.repeatable = repeatable;
    }

    /**
     * @return the key
     */
    public @NotNull Key getKey() {
        return this.key;
    }

    /**
     * @return the modifier mask the key needs ({@link KeyModifier#getMask()} bits)
     */
    public int getModifiers() {
        return this.modifiers;
    }

    /**
     * @return the bound action
     */
    public @NotNull A getAction() {
        return this.action;
    }

    /**
     * @return {@code true} if a key repeat also produces the action, {@code false} otherwise
     */
    public boolean isRepeatable() {
        return this.repeatable;
    }
}
