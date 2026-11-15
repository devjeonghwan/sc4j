package org.fogofwarlabs.sc4j.engine.client.dialog;

/**
 * Holds the checked state of a check box or an option button: activating a check box toggles it, activating an option button checks
 * it. Clearing the other option buttons of a group is the dialog's part ({@link CheckBox}).
 */
public final class CheckModel {
    private final boolean option;

    private boolean checked;

    /**
     * Creates a {@link CheckModel} instance, unchecked.
     *
     * @param option {@code true} for an option button, {@code false} for a check box
     */
    public CheckModel(boolean option) {
        this.option = option;

        this.checked = false;
    }

    /**
     * @return {@code true} for an option button, {@code false} for a check box
     */
    public boolean isOption() {
        return this.option;
    }

    /**
     * @return {@code true} if the box is checked, {@code false} otherwise
     */
    public boolean isChecked() {
        return this.checked;
    }

    /**
     * Sets the checked state.
     *
     * @param checked {@code true} to check the box
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
    }


    /**
     * Activates the box (a released press, Enter or its hotkey): a check box toggles, an option button becomes checked.
     */
    public void activate() {
        this.checked = this.option || !this.checked;
    }
}
