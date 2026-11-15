package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.scene.Scene;
import org.fogofwarlabs.sc4j.engine.client.scene.SceneStack;
import org.jetbrains.annotations.NotNull;

/**
 * Shows a {@link Dialog} as a scene: a screen of its own, or a modal popup pushed on the scene stack's modal layer
 * ({@link SceneStack#pushModal}). While open, the dialog is on top of the client's
 * {@link DialogStack}, which routes the input to it.
 *
 * @param <C> the type of the client context the scene opens with
 */
public final class DialogScene<C> implements Scene<C, Canvas> {
    private final @NotNull Dialog dialog;
    private final @NotNull DialogTheme theme;
    private final @NotNull DialogStack<Control, Dialog> dialogs;

    /**
     * Creates a {@link DialogScene} instance.
     *
     * @param dialog  the dialog
     * @param theme   the look of its controls
     * @param dialogs the client's open dialogs the scene joins while open
     */
    public DialogScene(@NotNull Dialog dialog, @NotNull DialogTheme theme, @NotNull DialogStack<Control, Dialog> dialogs) {
        this.dialog  = dialog;
        this.theme   = theme;
        this.dialogs = dialogs;
    }

    /**
     * @return the dialog
     */
    public @NotNull Dialog getDialog() {
        return this.dialog;
    }


    /**
     * Opens the scene: loads what it shows. Called once, before the first {@link #update}. The dialog goes on top of the open
     * dialogs.
     *
     * @param context the client's shared services
     */
    @Override
    public void open(@NotNull C context) {
        this.dialogs.push(this.dialog);
    }

    /**
     * Updates the scene at the supplied time. A dialog changes only on input.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void update(long time) {
    }

    /**
     * Draws the scene: the dialog.
     *
     * @param surface the surface, between its {@link Canvas#begin} and {@link Canvas#end}
     */
    @Override
    public void draw(@NotNull Canvas surface) {
        this.dialog.draw(surface, this.theme);
    }

    /**
     * Handles an input event while the scene is the top scene of its stack ({@link SceneStack#handleInput}): the dialog takes it
     * ({@link Dialog#handleInput}).
     *
     * @param event the event
     * @return {@code true} if the scene used the event, {@code false} otherwise
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        return this.dialog.handleInput(event);
    }

    /**
     * Closes the scene: releases what it holds. The dialog leaves the open dialogs.
     */
    @Override
    public void close() {
        this.dialogs.remove(this.dialog);
    }
}
