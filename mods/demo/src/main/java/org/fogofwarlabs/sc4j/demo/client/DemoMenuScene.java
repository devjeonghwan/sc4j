package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.app.ClientScene;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorContext;
import org.fogofwarlabs.sc4j.engine.client.dialog.Control;
import org.fogofwarlabs.sc4j.engine.client.dialog.Dialog;
import org.fogofwarlabs.sc4j.engine.client.dialog.DialogListener;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Shows one dialog of the demo's pack as a screen: the dialog is built from the pack each time the scene opens, drawn centred over a screen picture
 * of the demo's skin (by default the menu background), and takes the input; the pointer shows the arrow cursor and the menu music plays. A subclass
 * fills the dialog's controls when it opens, acts on its activated controls, and may draw over the screen, for example the factions' emblems. The
 * scene needs the dialog's size with a margin ({@link #getMinimumWidth}, {@link #getMinimumHeight}), so the client draws a smaller window at a lower
 * scale instead of cutting the dialog.
 */
public abstract class DemoMenuScene implements ClientScene, DialogListener {
    /**
     * The largest side of an emblem drawn beside the dialog.
     */
    protected static final float EMBLEM_SIZE = 160;

    private static final float EMBLEM_MARGIN = 16;
    /**
     * The logical margin the client keeps around the dialog on each side.
     */
    private static final int DIALOG_MARGIN = 16;
    private static final int HALF = 2;

    private final @NotNull DemoClient demo;
    private final @NotNull String dialogId;

    private @Nullable Dialog dialog;

    /**
     * Creates a {@link DemoMenuScene} instance; it reads its dialog when it opens.
     *
     * @param demo     the demo's shared services
     * @param dialogId the pack id of the dialog
     */
    protected DemoMenuScene(@NotNull DemoClient demo, @NotNull String dialogId) {
        this.demo     = demo;
        this.dialogId = dialogId;

        this.dialog = null;
    }

    /**
     * @return the demo's shared services
     */
    public final @NotNull DemoClient getDemo() {
        return this.demo;
    }

    /**
     * Returns the dialog of the open scene.
     *
     * @return the dialog
     * @throws IllegalStateException if the scene is not open
     */
    public final @NotNull Dialog requireDialog() {
        Dialog current = this.dialog;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The {0} screen is not open.", this.dialogId);
        }

        return current;
    }

    /**
     * Returns a control of the dialog.
     *
     * @param id the control id
     * @return the control
     * @throws IllegalStateException if the scene is not open or its dialog has no such control
     */
    public final @NotNull Control requireControl(int id) {
        Control control = this.requireDialog().findControl(id);

        if (control == null) {
            throw Exceptions.newIllegalStateException("The {0} dialog has no control {1}.", this.dialogId, id);
        }

        return control;
    }

    /**
     * Returns the logical width the scene needs to show whole: the dialog's width with a margin on each side.
     *
     * @return the logical width the scene needs, {@code 0} before the scene opened
     */
    @Override
    public int getMinimumWidth() {
        Dialog current = this.dialog;

        return current != null ? current.getDefinition().getWidth() + 2 * DIALOG_MARGIN : 0;
    }

    /**
     * Returns the logical height the scene needs to show whole: the dialog's height with a margin on each side.
     *
     * @return the logical height the scene needs, {@code 0} before the scene opened
     */
    @Override
    public int getMinimumHeight() {
        Dialog current = this.dialog;

        return current != null ? current.getDefinition().getHeight() + 2 * DIALOG_MARGIN : 0;
    }

    /**
     * Opens the scene: loads what it shows. Called once, before the first {@link #update}.
     * <p>
     * Builds the dialog from the pack, lets the subclass fill it, shows the arrow cursor and plays the menu music.
     *
     * @param context the client's shared services
     * @throws IOException if the dialog cannot be read
     */
    @Override
    public final void open(@NotNull SceneClient context) throws IOException {
        this.dialog = this.demo.createDialog(this.dialogId, this);
        this.onOpened();
        this.demo.getSkin().getCursor().update(CursorContext.NORMAL, null);
        this.demo.playMusic(DemoClient.MENU_MUSIC);
    }

    /**
     * Called when the scene opened and its dialog is built: fills the dialog's controls. The default fills nothing.
     */
    protected void onOpened() {
    }

    /**
     * Updates the scene at the supplied time. A menu changes only on input.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void update(long time) {
    }

    /**
     * Draws the scene: the screen picture, the dialog, centred, and what the subclass draws over them.
     *
     * @param surface the surface
     * @throws IOException if a picture cannot be read
     */
    @Override
    public final void draw(@NotNull Canvas surface) throws IOException {
        Dialog current = this.requireDialog();
        float width = current.getDefinition().getWidth();
        float height = current.getDefinition().getHeight();
        float left = (surface.getWidth() - width) / HALF;
        float top = (surface.getHeight() - height) / HALF;

        this.demo.getSkin().drawScreen(surface, this.getScreenPicture());
        current.setPosition(left, top);
        current.draw(surface, this.demo.getTheme());
        this.drawOver(surface, left, top, width, height);
    }

    /**
     * Returns the screen picture under the dialog. The default is the menu background.
     *
     * @return the pack id of the picture
     */
    protected @NotNull String getScreenPicture() {
        return DemoSkin.MENU_BACKGROUND;
    }

    /**
     * Draws over the screen after the dialog. The default draws nothing.
     *
     * @param surface the surface
     * @param left    the left coordinate of the dialog
     * @param top     the top coordinate of the dialog
     * @param width   the width of the dialog
     * @param height  the height of the dialog
     * @throws IOException if a picture cannot be read
     */
    protected void drawOver(@NotNull Canvas surface, float left, float top, float width, float height) throws IOException {
    }

    /**
     * Draws two emblems beside the dialog, each centred in the free space between the dialog and the screen edge, at most
     * {@value #EMBLEM_SIZE} pixels and smaller where the space is narrower.
     *
     * @param surface      the surface
     * @param left         the left coordinate of the dialog
     * @param top          the top coordinate of the dialog
     * @param width        the width of the dialog
     * @param height       the height of the dialog
     * @param leftFaction  the faction whose emblem shows left of the dialog, or {@code null} for none
     * @param rightFaction the faction whose emblem shows right of the dialog, or {@code null} for none
     * @throws IOException if a picture cannot be read
     */
    protected final void drawEmblemsBeside(@NotNull Canvas surface, float left, float top, float width, float height,
                                           @Nullable Faction leftFaction, @Nullable Faction rightFaction) throws IOException {
        float size = Math.min(EMBLEM_SIZE, left - EMBLEM_MARGIN * HALF);
        float centerY = top + height / HALF;

        this.demo.getSkin().drawEmblem(surface, leftFaction, left / HALF, centerY, size);
        this.demo.getSkin().drawEmblem(surface, rightFaction, left + width + left / HALF, centerY, size);
    }

    /**
     * Handles an input event the scene's stack hands to it: the dialog takes it.
     *
     * @param event the event
     * @return {@code true} if the scene used the event, {@code false} otherwise
     */
    @Override
    public final boolean handleInput(@NotNull InputEvent event) {
        return this.requireDialog().handleInput(event);
    }

    /**
     * Called when a control is activated: a released press, Enter, Escape or a hotkey.
     * <p>
     * Hands the control's id to the subclass.
     *
     * @param dialog  the dialog
     * @param control the activated control
     */
    @Override
    public final void onActivated(@NotNull Dialog dialog, @NotNull Control control) {
        this.onActivated(control.getDefinition().getId());
    }

    /**
     * Called when a control of the dialog is activated.
     *
     * @param id the id of the activated control
     */
    protected abstract void onActivated(int id);

    /**
     * Closes the scene: releases what it holds. The dialog holds nothing to release.
     */
    @Override
    public void close() {
        this.dialog = null;
    }
}
