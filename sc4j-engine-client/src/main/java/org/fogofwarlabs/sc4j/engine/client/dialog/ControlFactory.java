package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the live control of each control definition when a {@link Dialog} is built; a mod wraps
 * {@link DefaultControlFactory} to build its own controls ({@link org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind#CUSTOM}).
 */
public interface ControlFactory {
    /**
     * Creates the live control of a definition, without its children.
     *
     * @param dialog     the dialog being built; its control tree is not complete yet
     * @param definition the control's definition
     * @return the created {@link Control} instance
     */
    @NotNull Control createControl(@NotNull Dialog dialog, @NotNull DialogControl definition);
}
