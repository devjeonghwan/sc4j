package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the engine's default control of each {@link org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind}: panels (also
 * for animations and the mod's own controls), labels, buttons, check boxes and option buttons, lists, combo boxes, text
 * inputs and scroll bars.
 */
public final class DefaultControlFactory implements ControlFactory {
    private final int rowHeight;

    /**
     * Creates a {@link DefaultControlFactory} instance.
     *
     * @param rowHeight the row height of lists and combo boxes, usually the line height of the theme's font
     */
    public DefaultControlFactory(int rowHeight) {
        this.rowHeight = rowHeight;
    }


    /**
     * Creates the live control of a definition, without its children.
     *
     * @param dialog     the dialog being built; its control tree is not complete yet
     * @param definition the control's definition
     * @return the created {@link Control} instance
     */
    @Override
    public @NotNull Control createControl(@NotNull Dialog dialog, @NotNull DialogControl definition) {
        return switch (definition.getKind()) {
            case PANEL, ANIMATION, CUSTOM -> new Panel(definition);
            case LABEL -> new Label(definition);
            case BUTTON -> new Button(definition);
            case CHECK_BOX -> new CheckBox(definition, false, dialog);
            case OPTION_BUTTON -> new CheckBox(definition, true, dialog);
            case LIST_BOX -> new ListBox(definition, this.rowHeight);
            case COMBO_BOX -> new ComboBox(definition, this.rowHeight);
            case TEXT_INPUT -> new TextInput(definition);
            case SCROLL_BAR -> new ScrollBar(definition);
        };
    }
}
