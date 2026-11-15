package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests the text input's editing over characters outside the Basic Multilingual Plane: whole code points move, delete and count.
 */
final class TextInputTest {
    private static final int INPUT = 1;
    private static final int WIDTH = 80;
    private static final int HEIGHT = 12;
    private static final @NotNull String LIMIT = "3";
    /**
     * A character outside the Basic Multilingual Plane: two UTF-16 units, one code point.
     */
    private static final @NotNull String SUPPLEMENTARY = "😀";

    private static @NotNull TextInput createInput() {
        DialogControl definition = new DialogControl(INPUT, ControlKind.TEXT_INPUT, 0, 0, WIDTH, HEIGHT, "", TextAlignment.LEFT, null,
                                                     Map.of(TextInput.MAXIMUM_LENGTH_ATTRIBUTE, LIMIT), List.of());

        return new TextInput(definition);
    }

    @Test
    void backspace_and_cursor_keys_step_over_surrogate_pairs() {
        TextInput input = createInput();

        input.handleCharacter('a');
        input.handleCharacter(SUPPLEMENTARY.codePointAt(0));

        Assertions.assertEquals(3, input.getCursor());
        Assertions.assertTrue(input.handleKey(DialogKey.LEFT));
        Assertions.assertEquals(1, input.getCursor());
        Assertions.assertTrue(input.handleKey(DialogKey.RIGHT));
        Assertions.assertEquals(3, input.getCursor());
        Assertions.assertTrue(input.handleKey(DialogKey.BACKSPACE));
        Assertions.assertEquals("a", input.getText());

        input.setText(SUPPLEMENTARY + "b");
        input.handleKey(DialogKey.HOME);

        Assertions.assertTrue(input.handleKey(DialogKey.DELETE));
        Assertions.assertEquals("b", input.getText());
    }

    @Test
    void maximum_length_counts_code_points() {
        TextInput input = createInput();

        for (int index = 0; index <= Integer.parseInt(LIMIT); index++) {
            Assertions.assertTrue(input.handleCharacter(SUPPLEMENTARY.codePointAt(0)));
        }

        Assertions.assertEquals(SUPPLEMENTARY.repeat(Integer.parseInt(LIMIT)), input.getText());
        Assertions.assertFalse(input.handleCharacter('\n'));
        Assertions.assertFalse(input.handleKey(DialogKey.ENTER));
    }
}
