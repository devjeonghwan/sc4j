package org.fogofwarlabs.sc4j.engine.hook;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.system.SystemContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the hook factories of a game definition in registration order. Unlike a system, a hook never replaces another:
 * every registration adds one hook to its point, except at a point of {@link HookComposition#AT_MOST_ONE}.
 */
public final class HookRegistry {
    private final @NotNull List<@NotNull HookPoint<?>> points;
    private final @NotNull List<@NotNull HookFactory<?>> factories;

    /**
     * Creates an empty {@link HookRegistry} instance.
     */
    public HookRegistry() {
        this.points    = new ArrayList<>();
        this.factories = new ArrayList<>();
    }

    /**
     * Creates a {@link HookRegistry} instance with the registrations of another registry.
     *
     * @param other the registry to copy
     */
    public HookRegistry(@NotNull HookRegistry other) {
        this.points    = new ArrayList<>(other.points);
        this.factories = new ArrayList<>(other.factories);
    }

    /**
     * @return the number of registered hooks, of every point
     */
    public int getCount() {
        return this.points.size();
    }

    /**
     * Counts the hooks registered at a point.
     *
     * @param point the point
     * @return the number of hooks of the point
     */
    public int getCount(@NotNull HookPoint<?> point) {
        int count = 0;

        for (HookPoint<?> registered : this.points) {
            if (registered == point) {
                count++;
            }
        }

        return count;
    }


    /**
     * Registers the factory of one hook: appends it after the hooks already registered, at any point.
     *
     * @param <H>     the hook interface
     * @param point   the point
     * @param factory creates the hook of each game
     * @throws IllegalStateException if the point allows at most one hook and has one
     */
    public <H> void add(@NotNull HookPoint<H> point, @NotNull HookFactory<? extends H> factory) {
        if (point.getComposition() == HookComposition.AT_MOST_ONE && this.getCount(point) > 0) {
            throw Exceptions.newIllegalStateException("The hook point {0} allows at most one hook.", point);
        }

        this.points.add(point);
        this.factories.add(factory);
    }

    /**
     * Creates every registered hook for a new game, in registration order. The hooks are not attached yet.
     *
     * @param context the setup of the game and its created systems
     * @return the created hooks
     * @throws IllegalStateException if a factory creates a hook that does not implement its point's interface
     */
    public @NotNull HookTable createHooks(@NotNull SystemContext context) {
        HookPoint<?>[] createdPoints = this.points.toArray(new HookPoint<?>[0]);
        Object[] hooks = new Object[createdPoints.length];

        for (int index = 0; index < createdPoints.length; index++) {
            Object hook = this.factories.get(index).create(context);

            if (!createdPoints[index].getType().isInstance(hook)) {
                throw Exceptions.newIllegalStateException("The factory of {0} created a {1}.", createdPoints[index], hook.getClass().getName());
            }

            hooks[index] = hook;
        }

        return new HookTable(createdPoints, hooks);
    }
}
