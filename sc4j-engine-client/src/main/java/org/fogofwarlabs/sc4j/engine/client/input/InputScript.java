package org.fogofwarlabs.sc4j.engine.client.input;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Runs a test input script for a hidden or offscreen client: window input events fed on client frames, so that menus, dialogs and in-game
 * input can be exercised without a person at the keyboard. The client hands its window input entry points to the script as a {@link Sink},
 * so the scripted events take the same paths as real input. Any client loop can run a script: it loads it once ({@link #load(Path)}) and
 * calls {@link #advance(long, Sink)} once per frame.
 * <p>
 * One step per line; blank lines and lines starting with {@code #} are ignored:
 * <ul>
 * <li>{@code wait N}: the next steps run {@code N} client frames later;</li>
 * <li>{@code pause MS}: the next steps run {@code MS} milliseconds later (screens that animate on the clock, such as an animated menu, take the same
 * time however fast a hidden client draws);</li>
 * <li>{@code key NAME [mods]}: press and release a key, named as its engine {@link Key} (for example {@code F5}, {@code ENTER}, {@code ESCAPE},
 * {@code A}); a digit names its main keyboard key ({@code 0} is {@link Key#DIGIT_0}) and {@code KP_} stands for {@code KEYPAD_}; {@code mods} is a
 * {@code +}-separated list of {@code shift}, {@code ctrl} and {@code alt}. A letter or digit without Ctrl or Alt also types its character
 * between the press and the release, as the window's character callback does for a real key;</li>
 * <li>{@code keydown NAME [mods]}, {@code keyup NAME [mods]}: only the press or only the release of a key, to hold it (for example camera
 * scroll keys);</li>
 * <li>{@code keyrepeat NAME [mods]}: one key repeat of a held key, as the window sends while a key stays down;</li>
 * <li>{@code type TEXT}: the characters of the rest of the line;</li>
 * <li>{@code move X,Y}: the pointer moves to a point given as fractions ({@code 0..1}) of the framebuffer;</li>
 * <li>{@code click X,Y [left|right|middle] [mods]}, {@code press X,Y [button] [mods]}, {@code release X,Y [button] [mods]}: a button at a point (a
 * click is a press and a release on the same frame), with the modifiers as for {@code key};</li>
 * <li>{@code scroll N}: the mouse wheel turns by {@code N} steps (positive away from the user) at the pointer;</li>
 * <li>{@code shot FILE}: the next frame is saved as a PNG;</li>
 * <li>{@code exit}: the client closes as when its window is closed.</li>
 * </ul>
 */
public final class InputScript {
    private static final int STEP_PART_COUNT = 2;
    private static final int POINT_COORDINATE_COUNT = 2;
    private static final int MODIFIERS_PART_INDEX = 2;
    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;
    private static final int DECIMAL_RADIX = 10;
    private static final @NotNull String KEYPAD_SHORT_PREFIX = "KP_";
    private static final @NotNull String KEYPAD_PREFIX = "KEYPAD_";
    private static final @NotNull String COMMENT_PREFIX = "#";
    private static final @NotNull String WORD_SEPARATOR_PATTERN = "\\s+";
    private static final @NotNull String MODIFIER_SEPARATOR_PATTERN = "\\+";
    private static final @NotNull String COORDINATE_SEPARATOR = ",";

    private static final @NotNull String WAIT_STEP = "wait";
    private static final @NotNull String PAUSE_STEP = "pause";
    private static final @NotNull String KEY_STEP = "key";
    private static final @NotNull String KEY_DOWN_STEP = "keydown";
    private static final @NotNull String KEY_UP_STEP = "keyup";
    private static final @NotNull String KEY_REPEAT_STEP = "keyrepeat";
    private static final @NotNull String TYPE_STEP = "type";
    private static final @NotNull String MOVE_STEP = "move";
    private static final @NotNull String CLICK_STEP = "click";
    private static final @NotNull String PRESS_STEP = "press";
    private static final @NotNull String RELEASE_STEP = "release";
    private static final @NotNull String SCROLL_STEP = "scroll";
    private static final @NotNull String SHOT_STEP = "shot";
    private static final @NotNull String EXIT_STEP = "exit";

    private static final @NotNull String SHIFT_MODIFIER = "shift";
    private static final @NotNull String CONTROL_MODIFIER = "ctrl";
    private static final @NotNull String ALT_MODIFIER = "alt";
    private static final @NotNull String LEFT_BUTTON = "left";
    private static final @NotNull String RIGHT_BUTTON = "right";
    private static final @NotNull String MIDDLE_BUTTON = "middle";

    /**
     * Receives the scripted events, in the window input's terms.
     */
    public interface Sink {
        /**
         * @return the framebuffer width
         */
        int getFramebufferWidth();

        /**
         * @return the framebuffer height
         */
        int getFramebufferHeight();

        /**
         * Handles a scripted key event.
         *
         * @param event the key press, repeat or release
         */
        void onKeyEvent(@NotNull InputEvent event);

        /**
         * Handles a scripted character input.
         *
         * @param codePoint the typed code point
         */
        void onCharacterTyped(int codePoint);

        /**
         * Moves the pointer to the scripted framebuffer position.
         *
         * @param horizontal the framebuffer column
         * @param vertical   the framebuffer row
         */
        void onPointerMoved(float horizontal, float vertical);

        /**
         * Handles a scripted pointer button event.
         *
         * @param button    the button
         * @param pressed   whether the button goes down, else up
         * @param modifiers the modifier mask ({@link KeyModifier#getMask()} bits)
         */
        void onPointerButton(@NotNull PointerButton button, boolean pressed, int modifiers);

        /**
         * Handles a scripted vertical wheel movement.
         *
         * @param offset the vertical wheel offset
         */
        void onWheelScrolled(double offset);

        /**
         * Saves the next drawn frame to the requested PNG file.
         *
         * @param file the PNG the next drawn frame is saved to
         */
        void captureNext(@NotNull Path file);

        /**
         * Closes the client as the window's close button does.
         */
        void exit();
    }


    /**
     * Creates an {@link InputScript} instance from a script file.
     *
     * @param file the script
     * @return the created {@link InputScript} instance
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if a step is malformed
     */
    public static @NotNull InputScript load(@NotNull Path file) throws IOException {
        return parse(Files.readAllLines(file));
    }

    /**
     * Creates an {@link InputScript} instance from script lines; every step is checked before anything runs.
     *
     * @param lines the lines
     * @return the created {@link InputScript} instance
     * @throws IllegalArgumentException if a step is malformed
     */
    public static @NotNull InputScript parse(@NotNull List<String> lines) {
        List<String> steps = new ArrayList<>();

        for (String line : lines) {
            String step = line.strip();

            if (step.isEmpty() || step.startsWith(COMMENT_PREFIX)) {
                continue;
            }

            validateStep(step);
            steps.add(step);
        }

        return new InputScript(steps);
    }

    private static void validateStep(@NotNull String step) {
        String[] words = step.split(WORD_SEPARATOR_PATTERN, STEP_PART_COUNT);
        String argument = words.length > 1 ? words[1] : "";

        switch (words[0].toLowerCase(Locale.ROOT)) {
            case SCROLL_STEP -> Double.parseDouble(argument);
            case WAIT_STEP, PAUSE_STEP -> {
                if (Long.parseLong(argument) < 0) {
                    throw Exceptions.newIllegalArgumentException("{0} needs a count: {1}", words[0], step);
                }
            }
            case KEY_STEP, KEY_DOWN_STEP, KEY_UP_STEP, KEY_REPEAT_STEP -> {
                String[] parts = argument.split(WORD_SEPARATOR_PATTERN);

                parseKey(parts[0]);

                if (parts.length > 1) {
                    parseModifiers(parts[1]);
                }
            }
            case EXIT_STEP -> {
            }
            case TYPE_STEP, SHOT_STEP -> {
                if (argument.isEmpty()) {
                    throw Exceptions.newIllegalArgumentException("{0} needs an argument: {1}", words[0], step);
                }
            }
            case MOVE_STEP, CLICK_STEP, PRESS_STEP, RELEASE_STEP -> {
                String[] parts = argument.split(WORD_SEPARATOR_PATTERN);

                parsePointFractions(parts[0]);

                if (parts.length > 1) {
                    parsePointerButton(parts[1]);
                }

                if (parts.length > MODIFIERS_PART_INDEX) {
                    parseModifiers(parts[MODIFIERS_PART_INDEX]);
                }
            }
            default -> throw Exceptions.newIllegalArgumentException("Unknown input script step: {0}", step);
        }
    }

    private static @NotNull Key parseKey(@NotNull String name) {
        String upperCase = name.toUpperCase(Locale.ROOT);

        if (upperCase.length() == 1 && Character.isDigit(upperCase.charAt(0))) {
            return Key.ofDigit(Character.digit(upperCase.charAt(0), DECIMAL_RADIX));
        }

        if (upperCase.startsWith(KEYPAD_SHORT_PREFIX)) {
            upperCase = KEYPAD_PREFIX + upperCase.substring(KEYPAD_SHORT_PREFIX.length());
        }

        try {
            Key key = Key.valueOf(upperCase);

            if (key != Key.UNKNOWN) {
                return key;
            }
        } catch (IllegalArgumentException exception) {
            throw Exceptions.newIllegalArgumentException(exception, "Unknown key: {0}", name);
        }

        throw Exceptions.newIllegalArgumentException("Unknown key: {0}", name);
    }

    private static int parseModifiers(@NotNull String names) {
        int modifiers = 0;

        for (String name : names.toLowerCase(Locale.ROOT).split(MODIFIER_SEPARATOR_PATTERN)) {
            modifiers |= switch (name) {
                case SHIFT_MODIFIER -> KeyModifier.SHIFT.getMask();
                case CONTROL_MODIFIER -> KeyModifier.CONTROL.getMask();
                case ALT_MODIFIER -> KeyModifier.ALT.getMask();
                default -> throw Exceptions.newIllegalArgumentException("Unknown modifier: {0}", name);
            };
        }

        return modifiers;
    }

    private static @NotNull PointerButton parsePointerButton(@NotNull String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case LEFT_BUTTON -> PointerButton.LEFT;
            case RIGHT_BUTTON -> PointerButton.RIGHT;
            case MIDDLE_BUTTON -> PointerButton.MIDDLE;
            default -> throw Exceptions.newIllegalArgumentException("Unknown button: {0}", name);
        };
    }

    private static float @NotNull [] parsePointFractions(@NotNull String text) {
        String[] parts = text.split(COORDINATE_SEPARATOR);

        if (parts.length != POINT_COORDINATE_COUNT) {
            throw Exceptions.newIllegalArgumentException("A point is X,Y: {0}", text);
        }

        float horizontal = Float.parseFloat(parts[0]);
        float vertical = Float.parseFloat(parts[1]);

        if (horizontal < 0 || horizontal > 1 || vertical < 0 || vertical > 1) {
            throw Exceptions.newIllegalArgumentException("A point is two fractions from 0 to 1: {0}", text);
        }

        return new float[]{horizontal, vertical};
    }

    private static float @NotNull [] getFramebufferPoint(@NotNull String text, @NotNull Sink sink) {
        float[] fractions = parsePointFractions(text);

        return new float[]{fractions[0] * sink.getFramebufferWidth(), fractions[1] * sink.getFramebufferHeight()};
    }


    private final @NotNull List<String> steps;

    private int nextStepIndex;
    private long resumeFrame;
    private long resumeNanoseconds;

    /**
     * Creates an {@link InputScript} instance that runs the checked steps from the first one.
     *
     * @param steps the checked steps in order
     */
    private InputScript(@NotNull List<String> steps) {
        this.steps = steps;

        this.nextStepIndex     = 0;
        this.resumeFrame       = 0;
        this.resumeNanoseconds = System.nanoTime();
    }


    /**
     * @return {@code true} if every step ran, {@code false} otherwise
     */
    public boolean isFinished() {
        return this.nextStepIndex >= this.steps.size();
    }

    /**
     * Runs the steps due at a client frame: steps run in order until a {@code wait} holds the rest.
     *
     * @param frame the client frame counter
     * @param sink  the receiver of the events
     * @throws IllegalStateException if a step is not one of the known step kinds
     */
    public void advance(long frame, @NotNull Sink sink) {
        while (this.nextStepIndex < this.steps.size() && frame >= this.resumeFrame && System.nanoTime() - this.resumeNanoseconds >= 0) {
            String step = this.steps.get(this.nextStepIndex++);
            String[] words = step.split(WORD_SEPARATOR_PATTERN, STEP_PART_COUNT);
            String argument = words.length > 1 ? words[1] : "";

            switch (words[0].toLowerCase(Locale.ROOT)) {
                case WAIT_STEP -> this.resumeFrame = frame + Long.parseLong(argument);
                case PAUSE_STEP -> this.resumeNanoseconds = System.nanoTime() + Long.parseLong(argument) * NANOSECONDS_PER_MILLISECOND;
                case KEY_STEP -> {
                    String[] parts = argument.split(WORD_SEPARATOR_PATTERN);
                    Key key = parseKey(parts[0]);
                    int modifiers = parts.length > 1 ? parseModifiers(parts[1]) : 0;

                    sink.onKeyEvent(InputEvent.createKey(InputEvent.Type.KEY_PRESSED, key, modifiers));

                    if (parts[0].length() == 1 && Character.isLetterOrDigit(parts[0].charAt(0)) &&
                        !KeyModifier.CONTROL.isIn(modifiers) && !KeyModifier.ALT.isIn(modifiers)) {
                        char character = parts[0].charAt(0);

                        sink.onCharacterTyped(KeyModifier.SHIFT.isIn(modifiers)
                                              ? Character.toUpperCase(character)
                                              : Character.toLowerCase(character));
                    }

                    sink.onKeyEvent(InputEvent.createKey(InputEvent.Type.KEY_RELEASED, key, modifiers));
                }
                case KEY_DOWN_STEP, KEY_UP_STEP, KEY_REPEAT_STEP -> {
                    String[] parts = argument.split(WORD_SEPARATOR_PATTERN);
                    InputEvent.Type type = switch (words[0].toLowerCase(Locale.ROOT)) {
                        case KEY_DOWN_STEP -> InputEvent.Type.KEY_PRESSED;
                        case KEY_UP_STEP -> InputEvent.Type.KEY_RELEASED;
                        default -> InputEvent.Type.KEY_REPEATED;
                    };

                    sink.onKeyEvent(InputEvent.createKey(type, parseKey(parts[0]), parts.length > 1 ? parseModifiers(parts[1]) : 0));
                }
                case TYPE_STEP -> argument.codePoints().forEach(sink::onCharacterTyped);
                case MOVE_STEP -> {
                    float[] point = getFramebufferPoint(argument.split(WORD_SEPARATOR_PATTERN)[0], sink);

                    sink.onPointerMoved(point[0], point[1]);
                }
                case CLICK_STEP, PRESS_STEP, RELEASE_STEP -> {
                    String[] parts = argument.split(WORD_SEPARATOR_PATTERN);
                    float[] point = getFramebufferPoint(parts[0], sink);
                    PointerButton button = parts.length > 1 ? parsePointerButton(parts[1]) : PointerButton.LEFT;
                    int modifiers = parts.length > MODIFIERS_PART_INDEX ? parseModifiers(parts[MODIFIERS_PART_INDEX]) : 0;

                    sink.onPointerMoved(point[0], point[1]);

                    if (!words[0].equalsIgnoreCase(RELEASE_STEP)) {
                        sink.onPointerButton(button, true, modifiers);
                    }

                    if (!words[0].equalsIgnoreCase(PRESS_STEP)) {
                        sink.onPointerButton(button, false, modifiers);
                    }
                }
                case SCROLL_STEP -> sink.onWheelScrolled(Double.parseDouble(argument));
                case SHOT_STEP -> sink.captureNext(Path.of(argument));
                case EXIT_STEP -> sink.exit();
                default -> throw Exceptions.newIllegalStateException("Unchecked input script step: {0}", step);
            }
        }
    }
}
