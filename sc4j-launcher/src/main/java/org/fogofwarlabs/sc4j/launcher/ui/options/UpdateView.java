package org.fogofwarlabs.sc4j.launcher.ui.options;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.ui.ComponentUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shows the update part of the Settings screen's launcher panel in one row: the update source (the GitHub repository whose releases update
 * the install), Check now, the running version over the result of the last check, and Update when a newer version is available.
 */
public final class UpdateView {
    private static final double SOURCE_WIDTH = 260;

    /**
     * Receives the actions of the update part.
     */
    public interface Actions {
        /**
         * Called when the user checks for a newer version.
         *
         * @param source the update source as typed
         */
        void onCheck(@NotNull String source);

        /**
         * Called when the user updates to the newer version.
         */
        void onUpdate();
    }


    private final @NotNull LauncherTexts texts;
    private final @NotNull TextField source;
    private final @NotNull Button check;
    private final @NotNull Label status;
    private final @NotNull Button update;
    private final @NotNull HBox root;

    /**
     * Creates a {@link UpdateView} instance.
     *
     * @param texts   the launcher texts of the display language
     * @param version the running launcher's version
     * @param source  the update source {@code owner/name}
     * @param actions receives the actions
     */
    public UpdateView(@NotNull LauncherTexts texts, @NotNull String version, @NotNull String source, @NotNull Actions actions) {
        VBox state;

        this.texts  = texts;
        this.source = new TextField(source);
        this.check  = ComponentUtils.button(texts.get("update.check"), ComponentUtils.ButtonRole.SECONDARY);
        this.status = ComponentUtils.label("", "text-caption");
        this.update = ComponentUtils.button(texts.get("update.start"), ComponentUtils.ButtonRole.PRIMARY);

        this.source.setPrefWidth(SOURCE_WIDTH);
        this.check.setOnAction(event -> actions.onCheck(this.source.getText()));
        this.update.setOnAction(event -> actions.onUpdate());

        state     = ComponentUtils.column(ComponentUtils.Spacing.XS, ComponentUtils.label(texts.format("update.version", version), "text-caption"),
                                          this.status);
        this.root = ComponentUtils.row(ComponentUtils.Spacing.M, ComponentUtils.field(texts.get("update.source"), this.source), this.check, state,
                                       this.update);
        this.root.setAlignment(Pos.BOTTOM_LEFT);

        this.showStatus(null, ComponentUtils.Tone.NEUTRAL, false);
    }

    /**
     * @return the view's root node
     */
    public @NotNull HBox getRoot() {
        return this.root;
    }

    /**
     * Shows that a check runs; Check now is disabled until its result is shown.
     */
    public void showChecking() {
        this.showStatus(this.texts.get("update.checking"), ComponentUtils.Tone.ACCENT, false);
        this.check.setDisable(true);
    }

    /**
     * Shows the result of a check and enables Check now again.
     *
     * @param text      the result, or {@code null} to show none
     * @param tone      the result's colour role
     * @param updatable whether a newer version is available, which shows Update
     */
    public void showStatus(@Nullable String text, @NotNull ComponentUtils.Tone tone, boolean updatable) {
        this.check.setDisable(false);
        this.status.setText(text == null ? "" : text);
        this.status.getStyleClass().setAll("label", "text-caption");

        if (tone != ComponentUtils.Tone.NEUTRAL && tone != ComponentUtils.Tone.ACCENT) {
            this.status.getStyleClass().add(OptionsScreen.getToneStyle(tone));
        }
        this.status.setVisible(text != null);
        this.status.setManaged(text != null);
        this.update.setVisible(updatable);
        this.update.setManaged(updatable);
    }
}
