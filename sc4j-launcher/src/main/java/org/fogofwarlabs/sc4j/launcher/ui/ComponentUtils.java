package org.fogofwarlabs.sc4j.launcher.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.fogofwarlabs.sc4j.launcher.catalog.ModStatus;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Builds the shared components of the launcher theme. Every screen builds its controls here, so each component has one
 * definition: the style classes of {@code sc4j-launcher.css} plus the node structure below. Spacing and padding come from
 * the stylesheet's spacing classes, never from code.
 */
public final class ComponentUtils {
    private static final @NotNull String SPACING_PREFIX = "spacing-";
    private static final @NotNull String BADGE_PREFIX = "badge-";
    private static final @NotNull String STATUS_MARKER_PREFIX = "status-marker-";
    private static final @NotNull String NOTICE_PREFIX = "notice-";
    private static final @NotNull String STATUS_KEY_PREFIX = "status.";

    /**
     * Lists the spacing scale of the stylesheet: 4, 8, 12, 16, 24 and 32 pixels.
     */
    public enum Spacing {
        /**
         * 4 pixels.
         */
        XS,
        /**
         * 8 pixels.
         */
        S,
        /**
         * 12 pixels.
         */
        M,
        /**
         * 16 pixels.
         */
        L,
        /**
         * 24 pixels.
         */
        XL,
        /**
         * 32 pixels.
         */
        XXL;

        /**
         * @return the style class of the spacing, such as {@code spacing-m}
         */
        public @NotNull String getStyleClass() {
            return SPACING_PREFIX + this.name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Lists the colour roles of badges, status markers and notices.
     */
    public enum Tone {
        /**
         * Plain steel.
         */
        NEUTRAL,
        /**
         * The cyan accent.
         */
        ACCENT,
        /**
         * Green: ready, online.
         */
        SUCCESS,
        /**
         * Amber: needs attention.
         */
        WARNING,
        /**
         * Red: failed, unusable.
         */
        DANGER;

        /**
         * @return the style class suffix of the tone, such as {@code danger}
         */
        public @NotNull String getSuffix() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Lists the button roles: one primary action per screen, secondary for the rest, danger for destructive actions.
     */
    public enum ButtonRole {
        /**
         * The main action of a screen.
         */
        PRIMARY,
        /**
         * Any other action.
         */
        SECONDARY,
        /**
         * A destructive action.
         */
        DANGER
    }

    /**
     * Creates a label.
     *
     * @param text         the text
     * @param styleClasses the style classes to add
     * @return the created label
     */
    public static @NotNull Label label(@NotNull String text, @NotNull String @NotNull ... styleClasses) {
        Label label = new Label(text);

        label.getStyleClass().addAll(styleClasses);

        return label;
    }

    /**
     * Creates a label that wraps its text, for paragraphs.
     *
     * @param text         the text
     * @param styleClasses the style classes to add
     * @return the created label
     */
    public static @NotNull Label paragraph(@NotNull String text, @NotNull String @NotNull ... styleClasses) {
        Label label = label(text, styleClasses);

        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);

        return label;
    }

    /**
     * Creates a button.
     *
     * @param text the text
     * @param role the role, which selects the look
     * @return the created button
     */
    public static @NotNull Button button(@NotNull String text, @NotNull ButtonRole role) {
        Button button = new Button(text);

        switch (role) {
            case PRIMARY -> button.getStyleClass().add("primary");
            case DANGER -> button.getStyleClass().add("danger");
            case SECONDARY -> {
                // The default button look is the secondary look.
            }
        }

        return button;
    }

    /**
     * Creates a small button, for panel headers and footers.
     *
     * @param text the text
     * @param role the role, which selects the look
     * @return the created button
     */
    public static @NotNull Button smallButton(@NotNull String text, @NotNull ButtonRole role) {
        Button button = button(text, role);

        button.getStyleClass().add("small");

        return button;
    }

    /**
     * Creates a menu entry: a flat button as wide as its menu, its text on the left.
     *
     * @param text the text
     * @return the created button
     */
    public static @NotNull Button menuButton(@NotNull String text) {
        Button button = new Button(text);

        button.getStyleClass().add("menu-item");
        button.setMaxWidth(Double.MAX_VALUE);

        return button;
    }

    /**
     * Creates a chamfered badge.
     *
     * @param text the text, shown in capitals by the stylesheet
     * @param tone the colour role
     * @return the created badge
     */
    public static @NotNull Label badge(@NotNull String text, @NotNull Tone tone) {
        Label badge = label(text, "badge", BADGE_PREFIX + tone.getSuffix());

        badge.setMinWidth(Region.USE_PREF_SIZE);

        return badge;
    }

    /**
     * Creates the badge of a mod status; every view shows a status with it.
     *
     * @param status the mod status
     * @param texts  the launcher texts of the display language
     * @return the created badge
     */
    public static @NotNull Label statusBadge(@NotNull ModStatus status, @NotNull LauncherTexts texts) {
        return badge(texts.get(STATUS_KEY_PREFIX + status.name().toLowerCase(Locale.ROOT)), getTone(status));
    }

    /**
     * Returns the colour role of a mod status.
     *
     * @param status the mod status
     * @return {@link Tone#SUCCESS} for ready, {@link Tone#WARNING} for a warning and {@link Tone#DANGER} for an error
     */
    public static @NotNull Tone getTone(@NotNull ModStatus status) {
        return switch (status) {
            case READY -> Tone.SUCCESS;
            case WARNING -> Tone.WARNING;
            case ERROR -> Tone.DANGER;
        };
    }

    /**
     * Creates a diamond status marker, the cell of the logo grid.
     *
     * @param tone the colour role
     * @return the created marker
     */
    public static @NotNull Region statusMarker(@NotNull Tone tone) {
        Region marker = new Region();

        marker.getStyleClass().addAll("status-marker", STATUS_MARKER_PREFIX + tone.getSuffix());

        return marker;
    }

    /**
     * Creates an inline notice: a coloured bar, a wrapping text and the notice's actions after the text, such as a small button.
     *
     * @param tone     the colour role
     * @param text     the text
     * @param trailing the nodes after the text
     * @return the created notice
     */
    public static @NotNull HBox notice(@NotNull Tone tone, @NotNull String text, @NotNull Node @NotNull ... trailing) {
        Label label = paragraph(text);
        HBox notice = new HBox(label);

        notice.getChildren().addAll(trailing);
        HBox.setHgrow(label, Priority.ALWAYS);
        notice.getStyleClass().add("notice");

        if (tone != Tone.ACCENT && tone != Tone.NEUTRAL) {
            notice.getStyleClass().add(NOTICE_PREFIX + tone.getSuffix());
        }

        return notice;
    }

    /**
     * Creates a form field: a label above its control.
     *
     * @param label   the label text
     * @param control the control
     * @return the created field
     */
    public static @NotNull VBox field(@NotNull String label, @NotNull Node control) {
        VBox field = new VBox(label(label, "field-label"), control);

        field.getStyleClass().add("field");

        return field;
    }

    /**
     * Creates a row with the stylesheet's spacing, its children centred vertically.
     *
     * @param spacing  the spacing between the children
     * @param children the children
     * @return the created row
     */
    public static @NotNull HBox row(@NotNull Spacing spacing, @NotNull Node @NotNull ... children) {
        HBox row = new HBox(children);

        row.getStyleClass().add(spacing.getStyleClass());
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    /**
     * Creates a column with the stylesheet's spacing.
     *
     * @param spacing  the spacing between the children
     * @param children the children
     * @return the created column
     */
    public static @NotNull VBox column(@NotNull Spacing spacing, @NotNull Node @NotNull ... children) {
        VBox column = new VBox(children);

        column.getStyleClass().add(spacing.getStyleClass());

        return column;
    }

    /**
     * Creates an empty region that takes the free space of a row or a column.
     *
     * @return the created spacer
     */
    public static @NotNull Region growingSpacer() {
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox.setVgrow(spacer, Priority.ALWAYS);

        return spacer;
    }

    /**
     * Creates an image view of a square image.
     *
     * @param image the image
     * @param size  the width and height in pixels
     * @return the created image view
     */
    public static @NotNull ImageView imageView(@NotNull Image image, double size) {
        ImageView view = new ImageView(image);

        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        return view;
    }

    /**
     * Creates an image in the thin thumbnail frame, or the frame with a question mark when there is no image.
     * <p>
     * The frame always has the given size, with or without an image: the image is scaled to fit inside the frame's border, keeping its
     * aspect ratio, and centred.
     *
     * @param image  the image, or {@code null} for the placeholder
     * @param width  the width of the frame in pixels
     * @param height the height of the frame in pixels
     * @return the created thumbnail
     */
    public static @NotNull StackPane thumbnail(@Nullable Image image, double width, double height) {
        StackPane frame = new StackPane();

        if (image != null && !image.isError()) {
            ImageView view = new ImageView(image);

            view.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(0, width - frame.getInsets().getLeft() - frame.getInsets().getRight()),
                                                                      frame.insetsProperty()));
            view.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> Math.max(0, height - frame.getInsets().getTop() - frame.getInsets().getBottom()),
                                                                       frame.insetsProperty()));
            view.setPreserveRatio(true);
            view.setSmooth(true);
            frame.getChildren().add(view);
        } else {
            frame.getChildren().add(label("?", "text-title", "text-muted"));
        }

        frame.getStyleClass().add("thumbnail");
        frame.setMinSize(width, height);
        frame.setPrefSize(width, height);
        frame.setMaxSize(width, height);

        return frame;
    }

    /**
     * Creates a raised metal panel with a title header; the body grows to fill the panel.
     *
     * @param title          the title, shown in capitals by the stylesheet
     * @param body           the body
     * @param headerTrailing the nodes at the right end of the header
     * @return the created panel
     */
    public static @NotNull VBox panel(@NotNull String title, @NotNull Node body, @NotNull Node @NotNull ... headerTrailing) {
        HBox header = new HBox(label(title, "panel-title"), growingSpacer());
        VBox panel;

        header.getChildren().addAll(headerTrailing);
        header.getStyleClass().add("panel-header");
        VBox.setVgrow(body, Priority.ALWAYS);

        panel = new VBox(header, body);
        panel.getStyleClass().add("panel");

        return panel;
    }

    /**
     * Creates a raised metal panel without a header.
     *
     * @param body the body
     * @return the created panel
     */
    public static @NotNull VBox panel(@NotNull Node body) {
        VBox panel = new VBox(body);

        VBox.setVgrow(body, Priority.ALWAYS);
        panel.getStyleClass().add("panel");

        return panel;
    }

    /**
     * Creates a padded panel body holding its children in a column.
     *
     * @param children the children
     * @return the created body
     */
    public static @NotNull VBox panelBody(@NotNull Node @NotNull ... children) {
        VBox body = new VBox(children);

        body.getStyleClass().add("panel-body");

        return body;
    }

    /**
     * Creates a panel body for a list that runs edge to edge.
     *
     * @param children the children
     * @return the created body
     */
    public static @NotNull VBox flushPanelBody(@NotNull Node @NotNull ... children) {
        VBox body = panelBody(children);

        body.getStyleClass().add("flush");

        return body;
    }

    /**
     * Creates the footer strip of a panel, usually holding the panel's actions.
     *
     * @param children the children
     * @return the created footer
     */
    public static @NotNull HBox panelFooter(@NotNull Node @NotNull ... children) {
        HBox footer = new HBox(children);

        footer.getStyleClass().add("panel-footer");

        return footer;
    }

    /**
     * Creates a read-only monospaced text area, for logs and file contents.
     *
     * @param text       the text
     * @param styleClass {@code log-area} or {@code code-area}
     * @return the created text area
     */
    public static @NotNull TextArea codeArea(@NotNull String text, @NotNull String styleClass) {
        TextArea area = new TextArea(text);

        area.getStyleClass().add(styleClass);
        area.setEditable(false);

        return area;
    }

    /**
     * Creates a read-only text area of wrapped plain text in the body font, of a fixed height that scrolls, for longer text such as release
     * notes.
     *
     * @param text   the text
     * @param height the height in pixels
     * @return the created text area
     */
    public static @NotNull TextArea textArea(@NotNull String text, double height) {
        TextArea area = new TextArea(text);

        area.setEditable(false);
        area.setWrapText(true);
        area.setMinHeight(height);
        area.setPrefHeight(height);
        area.setMaxHeight(height);

        return area;
    }

    /**
     * Creates an empty key/value metadata grid in a sunken frame; {@link #addMetaRow} adds its rows.
     *
     * @return the created grid
     */
    public static @NotNull GridPane metaGrid() {
        GridPane grid = new GridPane();

        grid.getStyleClass().addAll("panel-sunken", "spacing-s");

        return grid;
    }

    /**
     * Adds a row of text to a metadata grid ({@link #metaGrid}); a long value wraps.
     *
     * @param grid  the grid
     * @param key   the key, in the faint colour
     * @param value the value
     */
    public static void addMetaRow(@NotNull GridPane grid, @NotNull String key, @NotNull String value) {
        Label valueLabel = label(value, "meta-value");

        valueLabel.setWrapText(true);
        addMetaRow(grid, key, valueLabel);
    }

    /**
     * Adds a row to a metadata grid ({@link #metaGrid}) under its last row.
     *
     * @param grid  the grid
     * @param key   the key, in the faint colour
     * @param value the value node, such as a link
     */
    public static void addMetaRow(@NotNull GridPane grid, @NotNull String key, @NotNull Node value) {
        Label keyLabel = label(key, "meta-key");

        keyLabel.setMinWidth(Region.USE_PREF_SIZE);
        grid.addRow(grid.getRowCount(), keyLabel, value);
    }

    /**
     * Sets a rich cell graphic that spans the cell width (inside its border and padding), so trailing badges align.
     *
     * @param cell    the list cell
     * @param graphic the graphic
     */
    public static void setCellGraphic(@NotNull ListCell<?> cell, @NotNull Region graphic) {
        graphic.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> cell.getWidth() - cell.getInsets().getLeft() - cell.getInsets().getRight(),
                                                                      cell.widthProperty(), cell.insetsProperty()));
        graphic.setMinWidth(0);
        cell.setText(null);
        cell.setGraphic(graphic);
    }

    /**
     * Creates a combo box whose drop-down list is exactly as wide as the combo box.
     *
     * @param width the preferred width
     * @param <T>   the item type
     * @return the combo box
     */
    public static <T> @NotNull ComboBox<T> comboBox(double width) {
        ComboBox<T> comboBox = new ComboBox<>();

        comboBox.setPrefWidth(width);
        comboBox.addEventHandler(ComboBoxBase.ON_SHOWING, event -> {
            if (comboBox.getSkin() instanceof ComboBoxListViewSkin<?> skin && skin.getPopupContent() instanceof Region list) {
                list.setMinWidth(comboBox.getWidth());
                list.setPrefWidth(comboBox.getWidth());
                list.setMaxWidth(comboBox.getWidth());
            }
        });

        return comboBox;
    }


    private ComponentUtils() {
    }
}
