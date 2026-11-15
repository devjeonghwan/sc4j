package org.fogofwarlabs.sc4j.engine.resource.dialog;

/**
 * Names the kinds of dialog controls the client dialog framework builds.
 */
public enum ControlKind {
    /**
     * A plain area that frames or groups other controls.
     */
    PANEL,
    /**
     * A text that the user cannot edit.
     */
    LABEL,
    /**
     * A push button.
     */
    BUTTON,
    /**
     * A button of a group of which one is selected.
     */
    OPTION_BUTTON,
    /**
     * A box that is checked or not.
     */
    CHECK_BOX,
    /**
     * A list of entries to select from.
     */
    LIST_BOX,
    /**
     * A closed list that opens to select an entry.
     */
    COMBO_BOX,
    /**
     * A text the user edits.
     */
    TEXT_INPUT,
    /**
     * A scroll bar or slider.
     */
    SCROLL_BAR,
    /**
     * A playing animation or video.
     */
    ANIMATION,
    /**
     * A control the mod builds itself, described by its attributes.
     */
    CUSTOM
}
