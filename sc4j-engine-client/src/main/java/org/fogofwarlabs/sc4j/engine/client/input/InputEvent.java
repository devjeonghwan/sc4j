package org.fogofwarlabs.sc4j.engine.client.input;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes one input event of a window: a key press, repeat or release, a typed character, a pointer movement, a pointer button press or
 * release, or a wheel movement. Pointer positions are in framebuffer pixels, the coordinates the client draws in; the source of the events
 * converts window coordinates.
 * <p>
 * Each kind of event has its own factory; a getter that does not apply to the event's {@link Type} returns its neutral value
 * ({@link Key#UNKNOWN}, {@code null}, {@code 0} or {@code false}).
 */
public final class InputEvent {
    /**
     * Names the kind of an input event.
     */
    public enum Type {
        /**
         * A key went down.
         */
        KEY_PRESSED,
        /**
         * A held key repeats.
         */
        KEY_REPEATED,
        /**
         * A key went up.
         */
        KEY_RELEASED,
        /**
         * A character was typed (after the keyboard layout and input method).
         */
        CHARACTER_TYPED,
        /**
         * The pointer moved.
         */
        POINTER_MOVED,
        /**
         * A pointer button went down.
         */
        BUTTON_PRESSED,
        /**
         * A pointer button went up.
         */
        BUTTON_RELEASED,
        /**
         * The wheel moved.
         */
        WHEEL_SCROLLED
    }

    /**
     * Creates an {@link InputEvent} instance of a key press, repeat or release.
     *
     * @param type      {@link Type#KEY_PRESSED}, {@link Type#KEY_REPEATED} or {@link Type#KEY_RELEASED}
     * @param key       the key
     * @param modifiers the modifier mask ({@link KeyModifier#getMask()} bits)
     * @return the created {@link InputEvent} instance
     * @throws IllegalArgumentException if the type is not a key type
     */
    public static @NotNull InputEvent createKey(@NotNull Type type, @NotNull Key key, int modifiers) {
        if (type != Type.KEY_PRESSED && type != Type.KEY_REPEATED && type != Type.KEY_RELEASED) {
            throw Exceptions.newIllegalArgumentException("Not a key event type: {0}", type);
        }

        return new InputEvent(type, key, modifiers, 0, null, 0, 0, false, 0, 0);
    }

    /**
     * Creates an {@link InputEvent} instance of a typed character.
     *
     * @param codePoint the Unicode code point
     * @return the created {@link InputEvent} instance
     */
    public static @NotNull InputEvent createCharacterTyped(int codePoint) {
        return new InputEvent(Type.CHARACTER_TYPED, Key.UNKNOWN, 0, codePoint, null, 0, 0, false, 0, 0);
    }

    /**
     * Creates an {@link InputEvent} instance of a pointer movement.
     *
     * @param x the framebuffer column of the pointer
     * @param y the framebuffer row of the pointer
     * @return the created {@link InputEvent} instance
     */
    public static @NotNull InputEvent createPointerMoved(float x, float y) {
        return new InputEvent(Type.POINTER_MOVED, Key.UNKNOWN, 0, 0, null, x, y, false, 0, 0);
    }

    /**
     * Creates an {@link InputEvent} instance of a pointer button press.
     *
     * @param button      the button
     * @param modifiers   the modifier mask ({@link KeyModifier#getMask()} bits)
     * @param x           the framebuffer column of the pointer
     * @param y           the framebuffer row of the pointer
     * @param doublePress whether the press completes a double press ({@link DoublePressDetector})
     * @return the created {@link InputEvent} instance
     */
    public static @NotNull InputEvent createButtonPressed(@NotNull PointerButton button, int modifiers, float x, float y, boolean doublePress) {
        return new InputEvent(Type.BUTTON_PRESSED, Key.UNKNOWN, modifiers, 0, button, x, y, doublePress, 0, 0);
    }

    /**
     * Creates an {@link InputEvent} instance of a pointer button release.
     *
     * @param button    the button
     * @param modifiers the modifier mask ({@link KeyModifier#getMask()} bits)
     * @param x         the framebuffer column of the pointer
     * @param y         the framebuffer row of the pointer
     * @return the created {@link InputEvent} instance
     */
    public static @NotNull InputEvent createButtonReleased(@NotNull PointerButton button, int modifiers, float x, float y) {
        return new InputEvent(Type.BUTTON_RELEASED, Key.UNKNOWN, modifiers, 0, button, x, y, false, 0, 0);
    }

    /**
     * Creates an {@link InputEvent} instance of a wheel movement.
     *
     * @param horizontalOffset the horizontal wheel offset, positive to the right
     * @param verticalOffset   the vertical wheel offset, positive away from the user
     * @param x                the framebuffer column of the pointer
     * @param y                the framebuffer row of the pointer
     * @return the created {@link InputEvent} instance
     */
    public static @NotNull InputEvent createWheelScrolled(double horizontalOffset, double verticalOffset, float x, float y) {
        return new InputEvent(Type.WHEEL_SCROLLED, Key.UNKNOWN, 0, 0, null, x, y, false, horizontalOffset, verticalOffset);
    }

    private final @NotNull Type type;

    private final @NotNull Key key;
    private final int modifiers;
    private final int codePoint;

    private final @Nullable PointerButton button;
    private final float x;
    private final float y;
    private final boolean doublePress;

    private final double horizontalOffset;
    private final double verticalOffset;

    private InputEvent(@NotNull Type type,
                       @NotNull Key key, int modifiers, int codePoint,
                       @Nullable PointerButton button, float x, float y, boolean doublePress,
                       double horizontalOffset, double verticalOffset) {
        this.type = type;

        this.key       = key;
        this.modifiers = modifiers;
        this.codePoint = codePoint;

        this.button      = button;
        this.x           = x;
        this.y           = y;
        this.doublePress = doublePress;

        this.horizontalOffset = horizontalOffset;
        this.verticalOffset   = verticalOffset;
    }

    /**
     * @return the kind of the event
     */
    public @NotNull Type getType() {
        return this.type;
    }

    /**
     * @return the key of a key event, otherwise {@link Key#UNKNOWN}
     */
    public @NotNull Key getKey() {
        return this.key;
    }

    /**
     * @return the modifier mask of a key or button event ({@link KeyModifier#getMask()} bits), otherwise {@code 0}
     */
    public int getModifiers() {
        return this.modifiers;
    }

    /**
     * @return the Unicode code point of a typed character, otherwise {@code 0}
     */
    public int getCodePoint() {
        return this.codePoint;
    }

    /**
     * @return the button of a button event, otherwise {@code null}
     */
    public @Nullable PointerButton getButton() {
        return this.button;
    }

    /**
     * @return the framebuffer column of the pointer of a pointer, button or wheel event, otherwise {@code 0}
     */
    public float getX() {
        return this.x;
    }

    /**
     * @return the framebuffer row of the pointer of a pointer, button or wheel event, otherwise {@code 0}
     */
    public float getY() {
        return this.y;
    }

    /**
     * @return {@code true} if a button press completes a double press, {@code false} otherwise
     */
    public boolean isDoublePress() {
        return this.doublePress;
    }

    /**
     * @return the horizontal offset of a wheel event, positive to the right, otherwise {@code 0}
     */
    public double getHorizontalOffset() {
        return this.horizontalOffset;
    }

    /**
     * @return the vertical offset of a wheel event, positive away from the user, otherwise {@code 0}
     */
    public double getVerticalOffset() {
        return this.verticalOffset;
    }


    /**
     * Returns whether a modifier is held during a key or button event.
     *
     * @param modifier the modifier
     * @return {@code true} if the event's modifier mask holds the modifier, {@code false} otherwise
     */
    public boolean isModifierDown(@NotNull KeyModifier modifier) {
        return modifier.isIn(this.modifiers);
    }

    /**
     * @return {@code true} if the event is a pointer movement or a button press or release, {@code false} otherwise
     */
    public boolean isPointerEvent() {
        return this.type == Type.POINTER_MOVED || this.type == Type.BUTTON_PRESSED || this.type == Type.BUTTON_RELEASED;
    }

    /**
     * @return {@code true} if the event is a key press, repeat or release, {@code false} otherwise
     */
    public boolean isKeyEvent() {
        return this.type == Type.KEY_PRESSED || this.type == Type.KEY_REPEATED || this.type == Type.KEY_RELEASED;
    }

    /**
     * @return {@code true} if the event is a key press or repeat, {@code false} otherwise
     */
    public boolean isKeyDown() {
        return this.type == Type.KEY_PRESSED || this.type == Type.KEY_REPEATED;
    }

    /**
     * Creates a copy of the event at another pointer position, for a client that hands events on in its own coordinates (for example
     * the coordinates of a scaled interface).
     *
     * @param x the column of the copy
     * @param y the row of the copy
     * @return the created {@link InputEvent} instance
     */
    public @NotNull InputEvent createAt(float x, float y) {
        return new InputEvent(this.type,
                              this.key, this.modifiers, this.codePoint,
                              this.button, x, y, this.doublePress,
                              this.horizontalOffset, this.verticalOffset);
    }
}
