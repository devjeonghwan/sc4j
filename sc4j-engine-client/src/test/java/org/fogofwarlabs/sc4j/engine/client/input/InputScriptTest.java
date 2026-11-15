package org.fogofwarlabs.sc4j.engine.client.input;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the parsing and frame scheduling of the engine input script.
 */
final class InputScriptTest {
    private static final int FRAMEBUFFER_WIDTH = 1000;
    private static final int FRAMEBUFFER_HEIGHT = 500;

    /**
     * Records the scripted events as text lines.
     */
    private static final class Recorder implements InputScript.Sink {
        private final @NotNull List<String> events;

        private Recorder() {
            this.events = new ArrayList<>();
        }

        /**
         * @return the framebuffer width
         */
        @Override
        public int getFramebufferWidth() {
            return FRAMEBUFFER_WIDTH;
        }

        /**
         * @return the framebuffer height
         */
        @Override
        public int getFramebufferHeight() {
            return FRAMEBUFFER_HEIGHT;
        }

        /**
         * Handles a scripted key event.
         *
         * @param event the key press, repeat or release
         */
        @Override
        public void onKeyEvent(@NotNull InputEvent event) {
            this.events.add("key " + event.getKey() + " " + event.getType() + " " + event.getModifiers());
        }

        /**
         * Handles a scripted character input.
         *
         * @param codePoint the typed code point
         */
        @Override
        public void onCharacterTyped(int codePoint) {
            this.events.add("char " + (char) codePoint);
        }

        /**
         * Moves the pointer to the scripted framebuffer position.
         *
         * @param horizontal the framebuffer column
         * @param vertical   the framebuffer row
         */
        @Override
        public void onPointerMoved(float horizontal, float vertical) {
            this.events.add("move " + (int) horizontal + "," + (int) vertical);
        }

        /**
         * Handles a scripted pointer button event.
         *
         * @param button    the button
         * @param pressed   whether the button goes down, else up
         * @param modifiers the modifier mask ({@link KeyModifier#getMask()} bits)
         */
        @Override
        public void onPointerButton(@NotNull PointerButton button, boolean pressed, int modifiers) {
            this.events.add("button " + button + " " + pressed);
        }

        /**
         * Handles a scripted vertical wheel movement.
         *
         * @param offset the vertical wheel offset
         */
        @Override
        public void onWheelScrolled(double offset) {
            this.events.add("scroll " + offset);
        }

        /**
         * Saves the next drawn frame to the requested PNG file.
         *
         * @param file the PNG the next drawn frame is saved to
         */
        @Override
        public void captureNext(@NotNull Path file) {
            this.events.add("shot " + file.getFileName());
        }

        /**
         * Closes the client as the window's close button does.
         */
        @Override
        public void exit() {
            this.events.add("exit");
        }
    }

    @Test
    void advance_should_run_steps_on_their_frames() {
        InputScript script = InputScript.parse(List.of("# F5 in the menus, then a click",
                                                       "key F5",
                                                       "wait 10",
                                                       "click 0.5,0.2",
                                                       "type hi",
                                                       "key A ctrl+shift",
                                                       "key B",
                                                       "shot out.png"));
        Recorder recorder = new Recorder();

        script.advance(0, recorder);

        Assertions.assertEquals(List.of("key F5 KEY_PRESSED 0", "key F5 KEY_RELEASED 0"), recorder.events);

        script.advance(9, recorder);

        Assertions.assertEquals(2, recorder.events.size());
        Assertions.assertFalse(script.isFinished());

        script.advance(10, recorder);

        int controlShift = KeyModifier.CONTROL.getMask() | KeyModifier.SHIFT.getMask();

        Assertions.assertEquals(List.of("key F5 KEY_PRESSED 0", "key F5 KEY_RELEASED 0",
                                        "move 500,100",
                                        "button LEFT true", "button LEFT false",
                                        "char h", "char i",
                                        "key A KEY_PRESSED " + controlShift,
                                        "key A KEY_RELEASED " + controlShift,
                                        "key B KEY_PRESSED 0",
                                        "char b",
                                        "key B KEY_RELEASED 0",
                                        "shot out.png"),
                                recorder.events);
        Assertions.assertTrue(script.isFinished());
    }

    @Test
    void scroll_steps_turn_the_wheel_and_reject_non_numeric_amounts() {
        InputScript script = InputScript.parse(List.of("scroll -2", "scroll 1.5"));
        Recorder recorder = new Recorder();

        script.advance(0, recorder);

        Assertions.assertEquals(List.of("scroll -2.0", "scroll 1.5"), recorder.events);
        Assertions.assertThrows(IllegalArgumentException.class, () -> InputScript.parse(List.of("scroll down")));
    }

    @Test
    void key_steps_take_engine_key_names_and_the_digit_and_keypad_short_names() {
        InputScript script = InputScript.parse(List.of("keydown 7", "keyup KP_ENTER", "keyrepeat KEYPAD_3 shift"));
        Recorder recorder = new Recorder();

        script.advance(0, recorder);

        Assertions.assertEquals(List.of("key DIGIT_7 KEY_PRESSED 0", "key KEYPAD_ENTER KEY_RELEASED 0",
                                        "key KEYPAD_3 KEY_REPEATED " + KeyModifier.SHIFT.getMask()),
                                recorder.events);
        Assertions.assertThrows(IllegalArgumentException.class, () -> InputScript.parse(List.of("key UNKNOWN")));
    }

    @Test
    void parse_should_reject_malformed_steps_before_running() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> InputScript.parse(List.of("key NOPE")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> InputScript.parse(List.of("click 2,0")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> InputScript.parse(List.of("jump")));
    }
}
