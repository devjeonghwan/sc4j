package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.skin.ButtonStyle;
import org.fogofwarlabs.sc4j.engine.client.skin.ColorBoxStyle;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextStyle;
import org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests the skin boxes of the dialog theme: panels, buttons and fields draw the theme's colours without boxes and the boxes of their state with
 * them, a selected button draws the selected box of its style (the pressed box when the style has none, the selection colour without style), and
 * a selected list entry draws the selection box or the selection colour.
 */
final class DialogThemeTest {
    private static final int SIZE = 20;
    private static final int CENTER = SIZE / 2;
    private static final int LINE_HEIGHT = 10;
    private static final int DEFAULT_BUTTON_FILL = 0x283848;
    private static final int DEFAULT_FOCUS_FRAME = 0xFFD040;
    private static final int PANEL_COLOR = 0x00AA00FF;
    private static final int NORMAL_COLOR = 0xAA0000FF;
    private static final int HOVERED_COLOR = 0xBB0000FF;
    private static final int PRESSED_COLOR = 0xCC0000FF;
    private static final int DISABLED_COLOR = 0x555555FF;
    private static final int FIELD_COLOR = 0x0000AAFF;
    private static final int SELECTED_COLOR = 0xDDAA00FF;
    private static final int SELECTION_BOX_COLOR = 0x00DDDDFF;
    private static final int DEFAULT_SELECTION_COLOR = 0x4060A0;
    private static final float FIELD_PADDING = 4;
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int RGB_SHIFT = 8;

    /**
     * Measures nothing and draws nothing: the tests look at the boxes only.
     */
    private static final class EmptyFont implements Font {
        @Override
        public int getLineHeight() {
            return LINE_HEIGHT;
        }

        @Override
        public @NotNull TextStyle getDefaultStyle() {
            return TextStyle.DEFAULT;
        }

        @Override
        public float measure(@NotNull CharSequence text, @NotNull TextStyle style) {
            return 0;
        }

        @Override
        public float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color, @NotNull TextStyle style) {
            return x;
        }
    }

    private static @NotNull DialogControl createDefinition(@NotNull ControlKind kind) {
        return new DialogControl(1, kind, 0, 0, SIZE, SIZE, "text", TextAlignment.LEFT, null, Map.of(), List.of());
    }

    private static int drawCenter(@NotNull Control control, @NotNull DialogTheme theme, int x, int y) {
        ImageCanvas canvas = new ImageCanvas(SIZE, SIZE);

        canvas.begin(SIZE, SIZE);
        control.draw(canvas, theme, 0, 0);
        canvas.end();

        return canvas.getImage().getRGB(x, y) & RGB_MASK;
    }

    @Test
    void panels_and_buttons_draw_the_theme_boxes_and_the_colours_without_them() {
        DialogTheme colors = DialogTheme.createDefault(new EmptyFont());
        DialogTheme skinned = colors.withPanelStyle(new ColorBoxStyle(PANEL_COLOR))
                                    .withButtonStyle(new ButtonStyle(new ColorBoxStyle(NORMAL_COLOR), new ColorBoxStyle(HOVERED_COLOR),
                                                                     new ColorBoxStyle(PRESSED_COLOR), new ColorBoxStyle(DISABLED_COLOR)));
        Panel panel = new Panel(createDefinition(ControlKind.PANEL));
        Button button = new Button(createDefinition(ControlKind.BUTTON));

        Assertions.assertNull(colors.getButtonStyle());
        Assertions.assertEquals(DEFAULT_BUTTON_FILL, drawCenter(button, colors, CENTER, CENTER), "the default fill without boxes");
        Assertions.assertEquals(PANEL_COLOR >>> RGB_SHIFT, drawCenter(panel, skinned, CENTER, CENTER));
        Assertions.assertEquals(NORMAL_COLOR >>> RGB_SHIFT, drawCenter(button, skinned, CENTER, CENTER));

        button.setEnabled(false);

        Assertions.assertEquals(DISABLED_COLOR >>> RGB_SHIFT, drawCenter(button, skinned, CENTER, CENTER));
        Assertions.assertSame(colors.getFont(), skinned.getFont());
        Assertions.assertNotEquals(DEFAULT_FOCUS_FRAME, drawCenter(button, skinned, 0, 0), "no focus outline without focus");
    }

    @Test
    void fields_draw_the_field_box_and_a_selected_button_without_selected_box_draws_pressed() {
        DialogTheme colors = DialogTheme.createDefault(new EmptyFont());
        DialogTheme skinned = colors.withButtonStyle(new ButtonStyle(new ColorBoxStyle(NORMAL_COLOR), new ColorBoxStyle(HOVERED_COLOR),
                                                                     new ColorBoxStyle(PRESSED_COLOR), new ColorBoxStyle(DISABLED_COLOR)))
                                    .withFieldStyle(new ColorBoxStyle(FIELD_COLOR), FIELD_PADDING);
        Button tab = new Button(createDefinition(ControlKind.BUTTON));

        Assertions.assertNull(colors.getFieldStyle());
        Assertions.assertEquals(0, colors.getFieldPadding());
        Assertions.assertEquals(FIELD_PADDING, skinned.getFieldPadding());
        Assertions.assertEquals(DEFAULT_BUTTON_FILL, drawCenter(new TextInput(createDefinition(ControlKind.TEXT_INPUT)), colors, CENTER, CENTER),
                                "the default fill without field box");

        List<Control> fields = List.of(new TextInput(createDefinition(ControlKind.TEXT_INPUT)),
                                       new ComboBox(createDefinition(ControlKind.COMBO_BOX), LINE_HEIGHT),
                                       new ListBox(createDefinition(ControlKind.LIST_BOX), LINE_HEIGHT));

        for (Control field : fields) {
            Assertions.assertEquals(FIELD_COLOR >>> RGB_SHIFT, drawCenter(field, skinned, 1, CENTER), field.getClass().getSimpleName());
        }

        tab.setSelected(true);

        Assertions.assertEquals(PRESSED_COLOR >>> RGB_SHIFT, drawCenter(tab, skinned, CENTER, CENTER), "a style without selected box draws pressed");
        Assertions.assertFalse(tab.isPressable());
    }

    @Test
    void a_selected_button_draws_the_selected_box_and_a_selected_entry_the_selection_box() {
        DialogTheme colors = DialogTheme.createDefault(new EmptyFont());
        DialogTheme skinned = colors.withButtonStyle(new ButtonStyle(new ColorBoxStyle(NORMAL_COLOR), new ColorBoxStyle(HOVERED_COLOR),
                                                                     new ColorBoxStyle(PRESSED_COLOR), new ColorBoxStyle(DISABLED_COLOR),
                                                                     new ColorBoxStyle(SELECTED_COLOR)))
                                    .withSelectionStyle(new ColorBoxStyle(SELECTION_BOX_COLOR));
        Button tab = new Button(createDefinition(ControlKind.BUTTON));
        ListBox list = new ListBox(createDefinition(ControlKind.LIST_BOX), LINE_HEIGHT);

        tab.setSelected(true);
        list.setEntries(List.of("first", "second"));
        list.setSelection(0);

        Assertions.assertEquals(SELECTED_COLOR >>> RGB_SHIFT, drawCenter(tab, skinned, CENTER, CENTER), "the chosen tab draws the selected box");
        Assertions.assertEquals(DEFAULT_SELECTION_COLOR, drawCenter(tab, colors, CENTER, CENTER),
                                "without boxes the chosen tab takes the selection colour");
        Assertions.assertNull(colors.getSelectionStyle());
        Assertions.assertEquals(SELECTION_BOX_COLOR >>> RGB_SHIFT, drawCenter(list, skinned, CENTER, 1), "the selected entry draws the selection box");
        Assertions.assertEquals(DEFAULT_SELECTION_COLOR, drawCenter(list, colors, CENTER, 1),
                                "without box the selected entry takes the selection colour");
        Assertions.assertNotEquals(SELECTION_BOX_COLOR >>> RGB_SHIFT, drawCenter(list, skinned, CENTER, LINE_HEIGHT + 1),
                                   "the other entry stays plain");
    }
}
