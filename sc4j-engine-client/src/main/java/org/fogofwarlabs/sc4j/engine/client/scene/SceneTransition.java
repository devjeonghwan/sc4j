package org.fogofwarlabs.sc4j.engine.client.scene;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Runs between two scenes of a {@link SceneStack}: while it runs, the outgoing scene stays current (updated and drawn) and
 * the transition draws over it; when it completes, the stack switches to the incoming scene. A transition object runs once.
 *
 * @param <S> the type of the surface the scenes draw on
 */
public interface SceneTransition<S> {
    /**
     * Starts the transition at the supplied time; called once, on the first frame the stack applies it.
     *
     * @param time the client time in milliseconds
     */
    void start(long time);

    /**
     * Advances the transition.
     *
     * @param time the client time in milliseconds
     * @return {@code true} if the transition is complete and the stack switches scenes, {@code false} otherwise
     */
    boolean update(long time);

    /**
     * Draws the transition over the scenes.
     *
     * @param surface the surface
     * @throws IOException if a resource the transition draws cannot be read
     */
    void draw(@NotNull S surface) throws IOException;
}
