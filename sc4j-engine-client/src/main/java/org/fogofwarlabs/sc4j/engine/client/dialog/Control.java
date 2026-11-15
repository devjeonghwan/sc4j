package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the live state of one control of a {@link Dialog}: its definition, its children and whether it is shown, enabled,
 * hovered, pressed and focused, and draws it with a {@link DialogTheme}.
 * <p>
 * Positions a control receives are relative to its own left and top edges. The definition's attributes
 * {@value #HOTKEY_ATTRIBUTE}, {@value #DEFAULT_ATTRIBUTE}, {@value #CANCEL_ATTRIBUTE}, {@value #HIDDEN_ATTRIBUTE} and
 * {@value #DISABLED_ATTRIBUTE} set the hotkey, the control Enter and Escape activate and the initial visibility and enablement.
 * <p>
 * This class is the extension point of the dialog framework: the engine's default controls ({@link DefaultControlFactory}) and a
 * mod's own controls are subclasses that a {@link ControlFactory} creates. A subclass keeps the shared live state here, which the
 * {@link Dialog} routes input by, and adds its own behaviour through the overridable methods: {@link #isPressable},
 * {@link #isFocusable} and {@link #contains} for hit tests, {@link #onPressed}, {@link #onDragged}, {@link #onReleased} and
 * {@link #onFocusLost} for the pointer and the focus, {@link #activate} for a released press, Enter or the hotkey,
 * {@link #handleKey}, {@link #handleCharacter} and {@link #handleWheel} for keys, characters and the wheel, and
 * {@link #drawControl} and {@link #drawOverlay} for its look. A control whose look reads resources that can fail reports the
 * failure as a {@link ControlDrawException}.
 * <p>
 * Content that gives a control its behaviour after the dialog is built (a list its screen fills, a widget a mod attaches) puts a
 * control of its own class in place of the built one ({@link Dialog#replaceControl}); the replacement takes over the shared live
 * state and receives the replaced control in {@link #onReplacing} for its own state.
 */
public abstract class Control {
    /**
     * The attribute naming the hotkey character of a control.
     */
    public static final @NotNull String HOTKEY_ATTRIBUTE = "hotkey";
    /**
     * The attribute ({@value #TRUE_VALUE}) of the control Enter activates when no focused control takes it.
     */
    public static final @NotNull String DEFAULT_ATTRIBUTE = "default";
    /**
     * The attribute ({@value #TRUE_VALUE}) of the control Escape activates.
     */
    public static final @NotNull String CANCEL_ATTRIBUTE = "cancel";
    /**
     * The attribute ({@value #TRUE_VALUE}) of a control that starts hidden.
     */
    public static final @NotNull String HIDDEN_ATTRIBUTE = "hidden";
    /**
     * The attribute ({@value #TRUE_VALUE}) of a control that starts disabled.
     */
    public static final @NotNull String DISABLED_ATTRIBUTE = "disabled";
    /**
     * The value of a set flag attribute.
     */
    public static final @NotNull String TRUE_VALUE = "true";
    /**
     * The hotkey of a control without one.
     */
    public static final int NO_HOTKEY = -1;

    private static final float HALF = 2;

    private final @NotNull DialogControl definition;
    private final @NotNull List<Control> children;
    private @Nullable Control parent;
    private @Nullable Dialog dialog;

    private boolean visible;
    private boolean enabled;
    private @NotNull String text;

    private boolean hovered;
    private boolean pressed;
    private boolean focused;

    /**
     * Creates a {@link Control} instance in the state its definition gives.
     *
     * @param definition the control's definition
     */
    protected Control(@NotNull DialogControl definition) {
        String text = definition.getText();

        this.definition = definition;
        this.children   = new ArrayList<>();
        this.parent     = null;
        this.dialog     = null;

        this.visible = !this.isFlagSet(HIDDEN_ATTRIBUTE);
        this.enabled = !this.isFlagSet(DISABLED_ATTRIBUTE);
        this.text    = text == null ? "" : text;

        this.hovered = false;
        this.pressed = false;
        this.focused = false;
    }

    /**
     * @return the control's definition
     */
    public final @NotNull DialogControl getDefinition() {
        return this.definition;
    }

    /**
     * @return the child controls in drawing order, unmodifiable
     */
    public final @NotNull List<Control> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    /**
     * @return the control this one is a child of, or {@code null} for a top-level control
     */
    public final @Nullable Control getParent() {
        return this.parent;
    }

    /**
     * @return the dialog that built the control, or {@code null} while its factory creates it
     */
    public final @Nullable Dialog getDialog() {
        return this.dialog;
    }

    /**
     * @return the left edge relative to the dialog
     */
    public final float getLeft() {
        Control parent = this.parent;

        return this.definition.getX() + (parent == null ? 0 : parent.getLeft());
    }

    /**
     * @return the top edge relative to the dialog
     */
    public final float getTop() {
        Control parent = this.parent;

        return this.definition.getY() + (parent == null ? 0 : parent.getTop());
    }

    /**
     * @return {@code true} if the control itself is set visible, {@code false} otherwise
     */
    public final boolean isVisible() {
        return this.visible;
    }

    /**
     * Sets whether the control is shown. Hiding the control that holds its dialog's press ends the press, so its release
     * activates nothing.
     *
     * @param visible {@code true} to show the control and its children
     */
    public final void setVisible(boolean visible) {
        Dialog dialog = this.dialog;

        this.visible = visible;

        if (!visible && dialog != null && dialog.getPressed() == this) {
            dialog.cancelPress();
        }
    }

    /**
     * @return {@code true} if the control and every parent are visible, {@code false} otherwise
     */
    public final boolean isShown() {
        Control parent = this.parent;

        return this.visible && (parent == null || parent.isShown());
    }

    /**
     * @return {@code true} if the control takes input, {@code false} otherwise
     */
    public final boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether the control takes input.
     *
     * @param enabled {@code true} to enable the control
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return {@code true} if the control is shown ({@link #isShown}) and enabled, {@code false} otherwise
     */
    public final boolean isInteractive() {
        return this.enabled && this.isShown();
    }

    /**
     * @return the text the control shows
     */
    public final @NotNull String getText() {
        return this.text;
    }

    /**
     * Sets the text the control shows.
     *
     * @param text the text
     */
    public void setText(@NotNull String text) {
        this.text = text;
    }

    /**
     * @return the hotkey code point, or {@link #NO_HOTKEY}
     */
    public final int getHotkey() {
        String hotkey = this.definition.getAttributes().get(HOTKEY_ATTRIBUTE);

        return hotkey == null || hotkey.isEmpty() ? NO_HOTKEY : hotkey.codePointAt(0);
    }

    /**
     * @return {@code true} if Enter activates the control when no focused control takes it, {@code false} otherwise
     */
    public final boolean isDefault() {
        return this.isFlagSet(DEFAULT_ATTRIBUTE);
    }

    /**
     * @return {@code true} if Escape activates the control, {@code false} otherwise
     */
    public final boolean isCancel() {
        return this.isFlagSet(CANCEL_ATTRIBUTE);
    }

    /**
     * @return {@code true} if the pointer hovers the control, {@code false} otherwise
     */
    public final boolean isHovered() {
        return this.hovered;
    }

    /**
     * @return {@code true} if the control holds a press, {@code false} otherwise
     */
    public final boolean isPressed() {
        return this.pressed;
    }

    /**
     * @return {@code true} if the control has the focus, {@code false} otherwise
     */
    public final boolean isFocused() {
        return this.focused;
    }

    /**
     * @return {@code true} if the control takes presses, {@code false} otherwise
     */
    public boolean isPressable() {
        return false;
    }

    /**
     * @return {@code true} if the control takes the focus, {@code false} otherwise
     */
    public boolean isFocusable() {
        return false;
    }

    /**
     * @return {@code true} if the control shows a part over the other controls (an open list), {@code false} otherwise
     */
    public boolean isOverlayOpen() {
        return false;
    }

    final void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    final void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    final void setFocused(boolean focused) {
        this.focused = focused;

        if (!focused) {
            this.onFocusLost();
        }
    }


    /**
     * Checks whether a point is on the control.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     * @return {@code true} if the point is inside the control's bounds, {@code false} otherwise
     */
    public boolean contains(float x, float y) {
        return x >= 0 && y >= 0 && x < this.definition.getWidth() && y < this.definition.getHeight();
    }

    /**
     * Called when a press goes to the control.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     */
    protected void onPressed(float x, float y) {
    }

    /**
     * Called when the pointer moves while the control holds the press.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     */
    protected void onDragged(float x, float y) {
    }

    /**
     * Called when the press of the control is released on it.
     *
     * @param x the x relative to the control
     * @param y the y relative to the control
     * @return {@code true} if the release activates the control (the dialog reports it), {@code false} otherwise
     */
    protected boolean onReleased(float x, float y) {
        return this.activate();
    }

    /**
     * Called when the control loses the focus.
     */
    protected void onFocusLost() {
    }

    /**
     * Called when this control has taken the place of another control of its dialog ({@link Dialog#replaceControl}), after the
     * shared live state moved over; a subclass takes over its own state from the replaced control here.
     *
     * @param replaced the replaced control, no longer in the dialog
     */
    protected void onReplacing(@NotNull Control replaced) {
    }

    /**
     * Activates the control: a released press, Enter on the focused control or its hotkey.
     *
     * @return {@code true} if the dialog reports the activation, {@code false} otherwise
     */
    public boolean activate() {
        return true;
    }

    /**
     * Takes a key while the control has the focus.
     *
     * @param key the key
     * @return {@code true} if the control used the key, {@code false} to let the dialog handle it
     */
    public boolean handleKey(@NotNull DialogKey key) {
        return false;
    }

    /**
     * Takes a typed character while the control has the focus.
     *
     * @param codePoint the character
     * @return {@code true} if the control used the character, {@code false} to let the dialog handle it
     */
    public boolean handleCharacter(int codePoint) {
        return false;
    }

    /**
     * Takes wheel steps while the control is hovered or focused.
     *
     * @param steps the steps, positive toward the end
     * @return {@code true} if the control used the steps, {@code false} otherwise
     */
    public boolean handleWheel(int steps) {
        return false;
    }

    /**
     * Draws the control and its children when it is visible.
     *
     * @param canvas the canvas that draws, between its {@link Canvas#begin} and {@link Canvas#end}
     * @param theme  the look of the controls
     * @param left   the left edge of the dialog on the canvas
     * @param top    the top edge of the dialog on the canvas
     * @throws ControlDrawException if a resource the control or a child draws cannot be read
     */
    public final void draw(@NotNull Canvas canvas, @NotNull DialogTheme theme, float left, float top) {
        if (!this.visible) {
            return;
        }

        this.drawControl(canvas, theme, left + this.getLeft(), top + this.getTop());

        for (Control child : this.children) {
            child.draw(canvas, theme, left, top);
        }
    }

    /**
     * Draws the control itself, without its children.
     *
     * @param canvas the canvas that draws
     * @param theme  the look of the controls
     * @param x      the control's left edge on the canvas
     * @param y      the control's top edge on the canvas
     * @throws ControlDrawException if a resource the control draws (an image, a sheet frame) cannot be read
     */
    protected abstract void drawControl(@NotNull Canvas canvas, @NotNull DialogTheme theme, float x, float y);

    /**
     * Draws the part of the control shown over all controls; called after every control is drawn.
     *
     * @param canvas the canvas that draws
     * @param theme  the look of the controls
     * @param x      the control's left edge on the canvas
     * @param y      the control's top edge on the canvas
     * @throws ControlDrawException if a resource the overlay draws cannot be read
     */
    protected void drawOverlay(@NotNull Canvas canvas, @NotNull DialogTheme theme, float x, float y) {
    }

    /**
     * Draws a text in the control's bounds, vertically centred and aligned as the definition says.
     *
     * @param canvas the canvas
     * @param theme  the look of the controls
     * @param text   the one-line text
     * @param x      the left edge of the area
     * @param y      the top edge of the area
     * @param width  the width of the area
     * @param height the height of the area
     * @param color  the packed text colour
     */
    protected final void drawText(@NotNull Canvas canvas, @NotNull DialogTheme theme, @NotNull String text,
                                  float x, float y, float width, float height,
                                  int color) {
        float textWidth = theme.getFont().measure(text);
        float left = switch (this.definition.getTextAlignment()) {
            case LEFT -> x;
            case CENTER -> x + (width - textWidth) / HALF;
            case RIGHT -> x + width - textWidth;
        };

        theme.getFont().draw(canvas, text, left, y + (height - theme.getFont().getLineHeight()) / HALF, color);
    }

    /**
     * @param theme the look of the controls
     * @return the text colour of the control's state: disabled, hovered or plain
     */
    protected final int getTextColor(@NotNull DialogTheme theme) {
        if (!this.enabled) {
            return theme.getDisabledTextColor();
        }

        return this.hovered ? theme.getHighlightTextColor() : theme.getTextColor();
    }


    final void addChild(@NotNull Control child) {
        child.parent = this;
        this.children.add(child);
    }

    final void attach(@NotNull Dialog dialog) {
        this.dialog = dialog;
    }

    /**
     * Puts a replacement in this control's place: it takes the parent, the children, the dialog and the shown, enabled and text
     * state, and this control leaves the tree.
     *
     * @param replacement the replacement, of the same definition and not in a dialog
     */
    final void replaceWith(@NotNull Control replacement) {
        Control parent = this.parent;

        replacement.parent = parent;
        replacement.dialog = this.dialog;

        replacement.visible = this.visible;
        replacement.enabled = this.enabled;
        replacement.text    = this.text;

        for (Control child : this.children) {
            child.parent = replacement;
            replacement.children.add(child);
        }

        if (parent != null) {
            parent.children.set(parent.children.indexOf(this), replacement);
        }

        this.children.clear();
        this.parent = null;
        this.dialog = null;

        replacement.onReplacing(this);
    }


    private boolean isFlagSet(@NotNull String attribute) {
        return TRUE_VALUE.equals(this.definition.getAttributes().get(attribute));
    }
}
