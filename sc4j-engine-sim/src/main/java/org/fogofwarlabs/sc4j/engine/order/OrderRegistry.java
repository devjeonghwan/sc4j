package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds the {@link OrderBehavior} of every order type of one order system: one slot per type of a fixed list of order types, the position of
 * a type in the list being its slot.
 * <p>
 * An order system creates its registry when the game is created, over the order types of the content (or the mod's own identity list in
 * content order), registers the behaviors before the first frame and then {@link #freeze freezes} it. Registering again before the freeze
 * replaces the behavior of the type. A lookup is an array read and allocates nothing.
 * <p>
 * The index form {@link #find(int)} is the fast path of a dispatcher whose unit stores its current order as the slot of the order type (the
 * content index), as a mod's own order byte does; every other caller names the type.
 *
 * @param <B> the behavior type of the order system
 */
public final class OrderRegistry<B extends OrderBehavior<?, ?>> {
    private final @NotNull List<@NotNull OrderType> types;

    private final @Nullable Object @NotNull [] behaviors;
    private boolean frozen;

    /**
     * Creates an empty {@link OrderRegistry} instance over a list of order types.
     *
     * @param types the order types; the position of a type is its slot, and the list is not copied, so it must not change
     */
    public OrderRegistry(@NotNull List<@NotNull OrderType> types) {
        this.types = types;

        this.behaviors = new Object[types.size()];
        this.frozen    = false;
    }

    /**
     * @return the order types of the registry in slot order
     */
    public @NotNull List<@NotNull OrderType> getTypes() {
        return this.types;
    }

    /**
     * @return the number of slots: the number of order types
     */
    public int getCapacity() {
        return this.behaviors.length;
    }

    /**
     * @return {@code true} if the registration has ended, {@code false} otherwise
     */
    public boolean isFrozen() {
        return this.frozen;
    }


    /**
     * Returns the behavior of an order type.
     *
     * @param type the order type
     * @return the behavior
     * @throws IllegalArgumentException if the type is not one of the registry's types or has no behavior
     */
    public @NotNull B get(@NotNull OrderType type) {
        B behavior = this.find(type);

        if (behavior == null) {
            throw Exceptions.newIllegalArgumentException("The order type {0} has no behavior.", type);
        }

        return behavior;
    }

    /**
     * Finds the behavior of an order type.
     *
     * @param type the order type
     * @return the behavior, or {@code null} if the type has none or is not one of the registry's types
     */
    public @Nullable B find(@NotNull OrderType type) {
        int slot = this.findSlot(type);

        return slot >= 0 ? this.cast(this.behaviors[slot]) : null;
    }

    /**
     * Finds the behavior of the order type in a slot: the fast path of a dispatcher whose unit stores the slot.
     *
     * @param slot the slot of the order type, any value
     * @return the behavior, or {@code null} if the type has none or the slot is outside the registry
     */
    public @Nullable B find(int slot) {
        return slot >= 0 && slot < this.behaviors.length ? this.cast(this.behaviors[slot]) : null;
    }

    /**
     * Registers the behavior of an order type, replacing a previous one.
     *
     * @param type     the order type
     * @param behavior the behavior
     * @throws IllegalArgumentException if the type is not one of the registry's types
     * @throws IllegalStateException    if the registry is frozen
     */
    public void register(@NotNull OrderType type, @NotNull B behavior) {
        if (this.frozen) {
            throw Exceptions.newIllegalStateException("The order registry is frozen; {0} cannot be registered.", type);
        }

        int slot = this.findSlot(type);

        if (slot < 0) {
            throw Exceptions.newIllegalArgumentException("The order type {0} is not a type of this registry.", type);
        }

        this.behaviors[slot] = behavior;
    }

    /**
     * Ends the registration; later registrations fail. Freezing again has no effect.
     */
    public void freeze() {
        this.frozen = true;
    }

    /**
     * Returns the slot of a type: its content index when the type sits there, otherwise its position found in the list (a type whose content
     * registered it elsewhere, or not at all).
     */
    private int findSlot(@NotNull OrderType type) {
        int index = type.getIndex();

        if (index >= 0 && index < this.behaviors.length && this.types.get(index) == type) {
            return index;
        }

        for (int slot = 0; slot < this.behaviors.length; slot++) {
            if (this.types.get(slot) == type) {
                return slot;
            }
        }

        return -1;
    }

    @SuppressWarnings("unchecked")
    private @Nullable B cast(@Nullable Object behavior) {
        return (B) behavior;
    }
}
