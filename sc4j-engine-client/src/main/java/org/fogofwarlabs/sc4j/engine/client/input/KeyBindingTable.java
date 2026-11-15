package org.fogofwarlabs.sc4j.engine.client.input;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the key bindings of one input mode: each key with an exact set of the compared modifiers binds at most one action. A key event
 * matches the binding of its key and of its modifiers within the compared mask (the other modifiers, for example Caps Lock, do not matter); a
 * repeat matches only a repeatable binding.
 * <p>
 * A lookup is an array read and allocates nothing.
 *
 * @param <A> the action class
 */
public final class KeyBindingTable<A> {
    /**
     * The modifiers a table compares by default: Shift, Control, Alt and Super.
     */
    public static final int DEFAULT_COMPARED_MODIFIERS = KeyModifier.SHIFT.getMask() | KeyModifier.CONTROL.getMask() | KeyModifier.ALT.getMask() |
                                                         KeyModifier.SUPER.getMask();

    private static final int MODIFIER_SLOTS = KeyModifier.ALL_MASK + 1;

    private final int comparedModifiers;

    private final @Nullable KeyBinding<A> @NotNull [] slots;
    private final @NotNull List<KeyBinding<A>> bindings;

    /**
     * Creates an empty {@link KeyBindingTable} instance that compares {@link #DEFAULT_COMPARED_MODIFIERS}.
     */
    public KeyBindingTable() {
        this(DEFAULT_COMPARED_MODIFIERS);
    }

    /**
     * Creates an empty {@link KeyBindingTable} instance.
     *
     * @param comparedModifiers the modifier mask a key event and a binding must agree on ({@link KeyModifier#getMask()} bits)
     */
    @SuppressWarnings("unchecked")
    public KeyBindingTable(int comparedModifiers) {
        this.comparedModifiers = comparedModifiers & KeyModifier.ALL_MASK;

        this.slots    = (KeyBinding<A>[]) new KeyBinding<?>[Key.values().length * MODIFIER_SLOTS];
        this.bindings = new ArrayList<>();
    }

    /**
     * @return the modifier mask a key event and a binding must agree on
     */
    public int getComparedModifiers() {
        return this.comparedModifiers;
    }

    /**
     * @return the bindings in the order they were bound, without the replaced ones
     */
    public @NotNull List<KeyBinding<A>> getBindings() {
        return Collections.unmodifiableList(this.bindings);
    }


    /**
     * Binds a key with modifiers to an action, replacing an earlier binding of the same key and compared modifiers.
     *
     * @param key        the key
     * @param modifiers  the modifier mask the key needs; only the compared modifiers count
     * @param action     the action
     * @param repeatable whether a key repeat also produces the action
     * @return the created binding
     * @throws IllegalArgumentException if the key is {@link Key#UNKNOWN}
     */
    public @NotNull KeyBinding<A> bind(@NotNull Key key, int modifiers, @NotNull A action, boolean repeatable) {
        if (key == Key.UNKNOWN) {
            throw Exceptions.newIllegalArgumentException("An unknown key cannot be bound to {0}", action);
        }

        KeyBinding<A> binding = new KeyBinding<>(key, modifiers & this.comparedModifiers, action, repeatable);
        int slot = this.getSlot(key, modifiers);
        KeyBinding<A> replaced = this.slots[slot];

        if (replaced != null) {
            this.bindings.remove(replaced);
        }

        this.slots[slot] = binding;
        this.bindings.add(binding);

        return binding;
    }

    /**
     * Removes the binding of a key with modifiers.
     *
     * @param key       the key
     * @param modifiers the modifier mask the key needs; only the compared modifiers count
     * @return the removed binding, or {@code null} if the key with these modifiers was not bound
     */
    public @Nullable KeyBinding<A> unbind(@NotNull Key key, int modifiers) {
        int slot = this.getSlot(key, modifiers);
        KeyBinding<A> removed = this.slots[slot];

        if (removed != null) {
            this.bindings.remove(removed);
            this.slots[slot] = null;
        }

        return removed;
    }

    /**
     * Finds the binding a key event produces.
     *
     * @param event the event
     * @return the binding of a key press or repeat ({@link #find(Key, int, boolean)}), or {@code null} for a release, another event or no binding
     */
    public @Nullable KeyBinding<A> find(@NotNull InputEvent event) {
        if (!event.isKeyDown()) {
            return null;
        }

        return this.find(event.getKey(), event.getModifiers(), event.getType() == InputEvent.Type.KEY_REPEATED);
    }

    /**
     * Finds the binding a key press or repeat produces.
     *
     * @param key       the key
     * @param modifiers the modifier mask of the event
     * @param repeat    whether the event is a key repeat
     * @return the binding, or {@code null} if the key with these modifiers is not bound or the event is a repeat of a binding that does not repeat
     */
    public @Nullable KeyBinding<A> find(@NotNull Key key, int modifiers, boolean repeat) {
        KeyBinding<A> binding = this.slots[this.getSlot(key, modifiers)];

        if (binding == null || repeat && !binding.isRepeatable()) {
            return null;
        }

        return binding;
    }

    private int getSlot(@NotNull Key key, int modifiers) {
        return key.ordinal() * MODIFIER_SLOTS + (modifiers & this.comparedModifiers);
    }
}
