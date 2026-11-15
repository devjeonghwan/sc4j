package org.fogofwarlabs.sc4j.engine.hook;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;

/**
 * Holds the hooks of one point of one game, in registration order. A system resolves its lists once, when it is
 * attached, keeps them in fields and calls the hooks by index:
 * {@snippet :
 * for (int index = 0; index < this.removedHooks.size(); index++) {
 *     this.removedHooks.get(index).onUnitRemoved(unit);
 * }
 *}
 * The list is immutable and its calls allocate nothing.
 *
 * @param <H> the hook interface
 */
public final class HookList<H> {
    /**
     * Creates an empty {@link HookList} instance: the list of a point without hooks.
     *
     * @param <H>   the hook interface
     * @param point the point
     * @return the created {@link HookList} instance
     */
    static <H> @NotNull HookList<H> createEmpty(@NotNull HookPoint<H> point) {
        return new HookList<>(point, createArray(point, 0));
    }

    /**
     * Creates the hook array of a point.
     *
     * @param <H>    the hook interface
     * @param point  the point
     * @param length the number of hooks
     * @return the created array
     */
    @SuppressWarnings("unchecked")
    static <H> @NotNull H @NotNull [] createArray(@NotNull HookPoint<H> point, int length) {
        return (H[]) Array.newInstance(point.getType(), length);
    }

    private final @NotNull HookPoint<H> point;
    private final @NotNull H @NotNull [] hooks;

    /**
     * Creates a {@link HookList} instance.
     *
     * @param point the point
     * @param hooks the hooks in registration order, owned by the list
     */
    HookList(@NotNull HookPoint<H> point, @NotNull H @NotNull [] hooks) {
        this.point = point;
        this.hooks = hooks;
    }

    /**
     * @return the point of the hooks
     */
    public @NotNull HookPoint<H> getPoint() {
        return this.point;
    }

    /**
     * @return the number of hooks
     */
    public int size() {
        return this.hooks.length;
    }

    /**
     * @return {@code true} if the point has no hook and the system runs its default behavior, {@code false} otherwise
     */
    public boolean isEmpty() {
        return this.hooks.length == 0;
    }

    /**
     * Returns one hook.
     *
     * @param index the position in registration order
     * @return the hook
     */
    public @NotNull H get(int index) {
        return this.hooks[index];
    }
}
