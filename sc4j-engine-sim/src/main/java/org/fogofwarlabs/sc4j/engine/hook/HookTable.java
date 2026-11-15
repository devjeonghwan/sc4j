package org.fogofwarlabs.sc4j.engine.hook;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the hooks of one game in registration order. Lookups by point search the registrations; they are meant for
 * {@link GameSystem#attach} and game creation, not for the frame.
 */
public final class HookTable {
    private final @NotNull HookPoint<?> @NotNull [] points;
    private final @NotNull Object @NotNull [] hooks;

    private boolean attached;

    /**
     * Creates a {@link HookTable} instance of created, not yet attached hooks.
     *
     * @param points the point of each hook, in registration order
     * @param hooks  the hooks, one per point entry
     */
    HookTable(@NotNull HookPoint<?> @NotNull [] points, @NotNull Object @NotNull [] hooks) {
        this.points = points;
        this.hooks  = hooks;

        this.attached = false;
    }

    /**
     * @return the number of hooks, of every point
     */
    public int getCount() {
        return this.hooks.length;
    }

    /**
     * @return {@code true} if {@link #attach} ran, {@code false} otherwise
     */
    public boolean isAttached() {
        return this.attached;
    }


    /**
     * Returns the hooks of a point, in registration order. A point without hooks gives an empty list.
     *
     * @param <H>   the hook interface
     * @param point the point
     * @return the hooks of the point
     */
    public <H> @NotNull HookList<H> get(@NotNull HookPoint<H> point) {
        int count = 0;

        for (HookPoint<?> registered : this.points) {
            if (registered == point) {
                count++;
            }
        }

        if (count == 0) {
            return HookList.createEmpty(point);
        }

        H[] pointHooks = HookList.createArray(point, count);
        int next = 0;

        for (int index = 0; index < this.points.length; index++) {
            if (this.points[index] == point) {
                pointHooks[next++] = point.getType().cast(this.hooks[index]);
            }
        }

        return new HookList<>(point, pointHooks);
    }

    /**
     * Attaches every hook that is also a {@link GameSystem} to its game, in registration order. The game calls this after
     * it attached its systems.
     *
     * @param game the game the hooks belong to
     * @throws IllegalStateException if the hooks are already attached
     */
    public void attach(@NotNull Game game) {
        if (this.attached) {
            throw Exceptions.newIllegalStateException("The hooks are already attached.");
        }

        this.attached = true;

        for (Object hook : this.hooks) {
            if (hook instanceof GameSystem system) {
                system.attach(game);
            }
        }
    }
}
