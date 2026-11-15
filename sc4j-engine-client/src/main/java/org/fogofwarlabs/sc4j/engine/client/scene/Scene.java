package org.fogofwarlabs.sc4j.engine.client.scene;

import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * One screen of a client: a menu, a lobby, a score screen, the game view or a modal dialog over another scene. A
 * {@link SceneStack} opens it once before its first update, updates and draws it every frame while it is current or modal,
 * and closes it when it is left.
 *
 * @param <C> the type of the context a scene opens with: the client's shared services
 * @param <S> the type of the surface a scene draws on
 */
public interface Scene<C, S> {
    /**
     * Opens the scene: loads what it shows. Called once, before the first {@link #update}.
     *
     * @param context the client's shared services
     * @throws IOException if a resource of the scene cannot be read
     */
    void open(@NotNull C context) throws IOException;

    /**
     * Updates the scene at the supplied time.
     *
     * @param time the client time in milliseconds
     */
    void update(long time);

    /**
     * Draws the scene.
     *
     * @param surface the surface
     * @throws IOException if a resource the scene draws cannot be read
     */
    void draw(@NotNull S surface) throws IOException;

    /**
     * Handles an input event the scene's stack hands to it ({@link SceneStack#handleInput}): by default while it is the top scene. The
     * default ignores it.
     *
     * @param event the event
     * @return {@code true} if the scene used the event, {@code false} otherwise
     */
    default boolean handleInput(@NotNull InputEvent event) {
        return false;
    }

    /**
     * Closes the scene: releases what it holds. A scene that was never opened is closed only if the stack opened it.
     */
    void close();
}
