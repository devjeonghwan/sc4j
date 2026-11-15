package org.fogofwarlabs.sc4j.launcher.ui.form;

import org.fogofwarlabs.sc4j.launcher.form.FormField;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the control of a form field for one setting type. {@link SettingsFormView} holds one factory per
 * {@link org.fogofwarlabs.sc4j.manifest.settings.SettingType}; the control passes every change to
 * {@link FormField#setInput(Object)} and then calls the change callback.
 */
public interface FieldControlFactory {
    /**
     * Creates the control of a field.
     *
     * @param field    the field, whose setting has this factory's type
     * @param texts    the launcher texts of the display language
     * @param onChange called after the control passed a changed value to the field
     * @return the created control, showing the field's current value
     */
    @NotNull FieldControl create(@NotNull FormField field, @NotNull LauncherTexts texts, @NotNull Runnable onChange);
}
