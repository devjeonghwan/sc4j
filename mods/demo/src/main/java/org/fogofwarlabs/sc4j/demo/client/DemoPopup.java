package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.app.ClientScene;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.dialog.Control;
import org.fogofwarlabs.sc4j.engine.client.dialog.Dialog;
import org.fogofwarlabs.sc4j.engine.client.dialog.DialogListener;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Shows one dialog of the demo's pack as a popup over the current screen: a modal scene of the client's scene stack, drawn centred over the dimmed
 * screen, which keeps updating under it; the popup takes every input while it is open and needs its dialog's size with a margin, so the client draws
 * a smaller window at a lower scale instead of cutting the dialog. Its owner fills its controls after it opened and acts on its activated controls
 * ({@link Listener}); the popup closes itself on {@link #close()}. The {@value #PROMPT} dialog asks one question: a title ({@link #TITLE_ID}), a line
 * of text ({@link #TEXT_ID}), an input ({@link #INPUT_ID}, hidden when the question needs none) and OK ({@link #OK_ID}) and Cancel
 * ({@link #CANCEL_ID}); the {@value #QUESTION} dialog asks one question without input in the same controls, a row lower.
 */
public final class DemoPopup implements ClientScene, DialogListener {
    /**
     * The pack id of the dialog of one question.
     */
    public static final @NotNull String PROMPT = "prompt";
    /**
     * The pack id of the dialog of one question without input.
     */
    public static final @NotNull String QUESTION = "question";
    /**
     * The id of the prompt's title.
     */
    public static final int TITLE_ID = 2;
    /**
     * The id of the prompt's line of text.
     */
    public static final int TEXT_ID = 3;
    /**
     * The id of the prompt's input; the question has none.
     */
    public static final int INPUT_ID = 10;
    /**
     * The id of the OK button of every popup dialog (prompt, question, options), the dialog's default control.
     */
    public static final int OK_ID = 20;
    /**
     * The id of the Cancel button of every popup dialog (prompt, question, options), which Escape activates.
     */
    public static final int CANCEL_ID = 21;

    private static final int DIM_COLOR = 0x000000A0;
    /**
     * The logical margin the client keeps around the popup's dialog on each side.
     */
    private static final int DIALOG_MARGIN = 16;
    private static final float HALF = 2;

    /**
     * Receives the activated controls of a popup.
     */
    public interface Listener {
        /**
         * Called when a control of the popup is activated.
         *
         * @param popup the popup
         * @param id    the id of the activated control
         */
        void onActivated(@NotNull DemoPopup popup, int id);
    }

    /**
     * Opens a popup over the client's current scene and plays the open sound.
     *
     * @param demo     the demo's shared services
     * @param dialogId the pack id of the popup's dialog
     * @param listener the receiver of its activated controls
     * @return the open popup
     * @throws IOException if the dialog cannot be read
     */
    public static @NotNull DemoPopup open(@NotNull DemoClient demo, @NotNull String dialogId, @NotNull Listener listener) throws IOException {
        DemoPopup popup = new DemoPopup(demo, dialogId, listener);

        demo.getClient().getScenes().pushModal(popup);
        demo.getSounds().play(DemoSounds.OPEN);

        return popup;
    }

    /**
     * Opens the prompt over the client's current scene: one question with its title and line, with an input, or the question dialog
     * without one.
     *
     * @param demo     the demo's shared services
     * @param title    the title
     * @param text     the line of text
     * @param input    {@code true} to ask for a text, which the input starts empty and focused for, {@code false} for OK and Cancel alone
     * @param listener the receiver of OK and Cancel
     * @return the open prompt
     * @throws IOException if the dialog cannot be read
     */
    public static @NotNull DemoPopup openPrompt(@NotNull DemoClient demo, @NotNull String title, @NotNull String text, boolean input,
                                                @NotNull Listener listener) throws IOException {
        DemoPopup popup = open(demo, input ? PROMPT : QUESTION, listener);

        popup.requireControl(TITLE_ID).setText(title);
        popup.requireControl(TEXT_ID).setText(text);

        if (input) {
            popup.requireDialog().setFocused(popup.requireControl(INPUT_ID));
        }

        return popup;
    }


    private final @NotNull DemoClient demo;
    private final @NotNull String dialogId;
    private final @NotNull Listener listener;

    private @Nullable Dialog dialog;
    private boolean closed;

    private DemoPopup(@NotNull DemoClient demo, @NotNull String dialogId, @NotNull Listener listener) {
        this.demo     = demo;
        this.dialogId = dialogId;
        this.listener = listener;

        this.dialog = null;
        this.closed = false;
    }

    /**
     * @return the pack id of the popup's dialog
     */
    public @NotNull String getDialogId() {
        return this.dialogId;
    }

    /**
     * Returns the dialog of the open popup.
     *
     * @return the dialog
     * @throws IllegalStateException if the popup is not open
     */
    public @NotNull Dialog requireDialog() {
        Dialog current = this.dialog;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The {0} popup is not open.", this.dialogId);
        }

        return current;
    }

    /**
     * Returns a control of the dialog.
     *
     * @param id the control id
     * @return the control
     * @throws IllegalStateException if the popup is not open or its dialog has no such control
     */
    public @NotNull Control requireControl(int id) {
        Control control = this.requireDialog().findControl(id);

        if (control == null) {
            throw Exceptions.newIllegalStateException("The {0} dialog has no control {1}.", this.dialogId, id);
        }

        return control;
    }

    /**
     * @return {@code true} if the popup is open, {@code false} once it closed
     */
    public boolean isOpen() {
        return this.dialog != null && !this.closed;
    }


    /**
     * Returns the logical width the scene needs to show whole: the dialog's width with a margin on each side.
     *
     * @return the logical width the scene needs, {@code 0} before the popup opened
     */
    @Override
    public int getMinimumWidth() {
        Dialog current = this.dialog;

        return current != null ? current.getDefinition().getWidth() + 2 * DIALOG_MARGIN : 0;
    }

    /**
     * Returns the logical height the scene needs to show whole: the dialog's height with a margin on each side.
     *
     * @return the logical height the scene needs, {@code 0} before the popup opened
     */
    @Override
    public int getMinimumHeight() {
        Dialog current = this.dialog;

        return current != null ? current.getDefinition().getHeight() + 2 * DIALOG_MARGIN : 0;
    }

    /**
     * Opens the scene: loads what it shows. Called once, before the first {@link #update}.
     * <p>
     * Builds the dialog from the pack.
     *
     * @param context the client's shared services
     * @throws IOException if the dialog cannot be read
     */
    @Override
    public void open(@NotNull SceneClient context) throws IOException {
        this.dialog = this.demo.createDialog(this.dialogId, this);
    }

    /**
     * Updates the scene at the supplied time. The popup changes only on input; the screen under it updates itself.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void update(long time) {
    }

    /**
     * Draws the scene: the screen under it dimmed, then the dialog, centred.
     *
     * @param surface the surface
     */
    @Override
    public void draw(@NotNull Canvas surface) {
        Dialog current = this.requireDialog();
        float width = current.getDefinition().getWidth();
        float height = current.getDefinition().getHeight();

        surface.fillRectangle(0, 0, surface.getWidth(), surface.getHeight(), DIM_COLOR);
        current.setPosition((surface.getWidth() - width) / HALF, (surface.getHeight() - height) / HALF);
        current.draw(surface, this.demo.getTheme());
    }

    /**
     * Handles an input event the scene's stack hands to it: the dialog takes it, and the popup keeps every event from the screen under it.
     *
     * @param event the event
     * @return {@code true}: the popup uses every event
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        this.requireDialog().handleInput(event);

        return true;
    }

    /**
     * Called when a control is activated: a released press, Enter, Escape or a hotkey.
     * <p>
     * Hands the control's id to the listener.
     *
     * @param dialog  the dialog
     * @param control the activated control
     */
    @Override
    public void onActivated(@NotNull Dialog dialog, @NotNull Control control) {
        this.listener.onActivated(this, control.getDefinition().getId());
    }

    /**
     * Closes the scene: takes the popup off the client's scene stack the first time; later calls do nothing.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        this.demo.getClient().getScenes().closeModal(this);
    }
}
