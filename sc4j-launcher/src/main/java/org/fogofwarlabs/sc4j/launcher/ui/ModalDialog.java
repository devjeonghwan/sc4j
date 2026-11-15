package org.fogofwarlabs.sc4j.launcher.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Builds a dialog shown inside the launcher window over a dimmed screen: a panel with a title, a body, a message line for a problem, a progress
 * bar for long work and the action buttons in its footer. A screen shows it with {@link #getRoot()} on top of its content and removes it to
 * close it. Its primary button is the default button (Enter) and its first other button the cancel button (Escape).
 */
public final class ModalDialog {
    private static final double PANEL_WIDTH = 460;

    private static void setShown(@NotNull Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }


    private final @NotNull VBox body;
    private final @NotNull Label message;
    private final @NotNull Label progressText;
    private final @NotNull ProgressBar progress;
    private final @NotNull VBox progressRow;
    private final @NotNull HBox footer;
    private final @NotNull StackPane root;

    /**
     * Creates a {@link ModalDialog} instance without buttons, of the common width.
     *
     * @param title the title, shown in capitals by the stylesheet
     */
    public ModalDialog(@NotNull String title) {
        this(title, PANEL_WIDTH);
    }

    /**
     * Creates a {@link ModalDialog} instance without buttons, of a width for wider content such as a list beside a list.
     *
     * @param title the title, shown in capitals by the stylesheet
     * @param width the width of the panel, in logical units
     */
    public ModalDialog(@NotNull String title, double width) {
        VBox panel;

        this.body         = ComponentUtils.panelBody();
        this.message      = ComponentUtils.paragraph("", "field-message");
        this.progressText = ComponentUtils.label("", "text-caption");
        this.progress     = new ProgressBar(0);
        this.progressRow  = ComponentUtils.column(ComponentUtils.Spacing.XS, this.progressText, this.progress);
        this.footer       = ComponentUtils.panelFooter();
        this.root         = new StackPane();

        this.progress.setMaxWidth(Double.MAX_VALUE);
        this.body.getChildren().addAll(this.message, this.progressRow);
        setShown(this.message, false);
        setShown(this.progressRow, false);
        this.footer.setAlignment(Pos.CENTER_RIGHT);

        panel = ComponentUtils.panel(title, this.body);
        panel.getChildren().add(this.footer);
        panel.setPrefWidth(width);
        panel.setMaxWidth(width);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        this.root.getStyleClass().add("modal-scrim");
        this.root.getChildren().add(panel);
        StackPane.setAlignment(panel, Pos.CENTER);
    }

    /**
     * @return the dialog's root: the dimming layer with the centred panel, to be laid over a screen
     */
    public @NotNull StackPane getRoot() {
        return this.root;
    }

    /**
     * Adds content to the body, above the message line and the progress bar.
     *
     * @param content the content
     */
    public void addContent(@NotNull Node @NotNull ... content) {
        int index = this.body.getChildren().indexOf(this.message);

        this.body.getChildren().addAll(index, List.of(content));
    }

    /**
     * Sets the footer buttons from left to right, after a spacer: the cancel button (Escape) and the action button (Enter).
     *
     * @param cancel the button that closes the dialog without acting, or {@code null} for none
     * @param action the button that acts, or {@code null} for none
     */
    public void setButtons(@Nullable Button cancel, @Nullable Button action) {
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);
        this.footer.getChildren().setAll(spacer);

        if (cancel != null) {
            cancel.setCancelButton(true);
            this.footer.getChildren().add(cancel);
        }

        if (action != null) {
            action.setDefaultButton(true);
            this.footer.getChildren().add(action);
        }
    }

    /**
     * Shows a problem in one line under the content, or hides the line.
     *
     * @param text the problem, or {@code null} to hide the line
     */
    public void showMessage(@Nullable String text) {
        this.message.setText(text == null ? "" : text);
        setShown(this.message, text != null);
    }

    /**
     * Shows the progress of long work under the content, or hides it.
     *
     * @param text  the text over the bar, or {@code null} to hide the progress
     * @param share the share done, from {@code 0} to {@code 1}, or {@link ProgressBar#INDETERMINATE_PROGRESS} when it is not known
     */
    public void showProgress(@Nullable String text, double share) {
        this.progressText.setText(text == null ? "" : text);
        this.progress.setProgress(share);
        setShown(this.progressRow, text != null);
    }
}
