package org.fogofwarlabs.sc4j.launcher.ui.mods;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.launcher.catalog.ModEntry;
import org.fogofwarlabs.sc4j.launcher.catalog.ModIssue;
import org.fogofwarlabs.sc4j.launcher.catalog.SettingsIssue;
import org.fogofwarlabs.sc4j.launcher.form.SettingsForm;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.ui.ComponentUtils;
import org.fogofwarlabs.sc4j.launcher.ui.form.SettingsFormView;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.text.LocalizedText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the selected mod. A mod that can be started shows its banner, the details of its manifest, its screenshots and
 * the generated settings form with the Run button; a mod with an error shows the reasons and cannot be started. The
 * reasons of a warning are shown above the form. Either view offers Remove, and a newer release of a mod installed from a release is shown
 * with its Update button.
 */
public final class ModDetailView {
    private static final double ICON_SIZE = 72;
    private static final double BANNER_ICON_SIZE = 64;
    private static final double BANNER_HEIGHT = 128;
    private static final @NotNull BackgroundSize BANNER_COVER = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, false, true);
    private static final double ABOUT_PANEL_WIDTH = 330;
    private static final double SCREENSHOT_WIDTH = 92;
    private static final double SCREENSHOT_HEIGHT = 52;
    private static final @NotNull String LINE_SEPARATOR = "\n";

    /**
     * Receives the actions of the detail view.
     */
    public interface Actions {
        /**
         * Called when the user presses Run with a valid form.
         *
         * @param entry the mod
         * @param form  the mod's settings form, to be saved before the start
         */
        void onRun(@NotNull ModEntry entry, @NotNull SettingsForm form);

        /**
         * Called when the user copies text, such as the problems of a mod.
         *
         * @param text the text for the clipboard
         */
        void onCopy(@NotNull String text);

        /**
         * Called when the user opens the mod's website.
         *
         * @param address the website address of the manifest
         */
        void onOpenWebsite(@NotNull String address);

        /**
         * Called when the user updates the mod to its newer release.
         *
         * @param entry the mod
         */
        void onUpdate(@NotNull ModEntry entry);

        /**
         * Called when the user removes the mod.
         *
         * @param entry the mod
         */
        void onRemove(@NotNull ModEntry entry);
    }


    private final @NotNull ModEntry entry;
    private final @Nullable String updateRelease;
    private final @NotNull LauncherTexts texts;
    private final @NotNull ModArt art;
    private final @NotNull Actions actions;
    private final @NotNull Region root;

    /**
     * Creates a {@link ModDetailView} instance.
     *
     * @param entry         the mod
     * @param updateRelease the tag of the mod's newer release, or {@code null} when none is known
     * @param form          the mod's settings form, or {@code null} for a mod with an error
     * @param texts         the launcher texts of the display language
     * @param art           the decoded mod images
     * @param running       {@code true} while a run is in progress, which keeps Run disabled
     * @param actions       receives the actions
     */
    public ModDetailView(@NotNull ModEntry entry, @Nullable String updateRelease, @Nullable SettingsForm form, @NotNull LauncherTexts texts,
                         @NotNull ModArt art, @NotNull ObservableBooleanValue running, @NotNull Actions actions) {
        this.entry         = entry;
        this.updateRelease = updateRelease;
        this.texts         = texts;
        this.art           = art;
        this.actions       = actions;
        this.root          = form != null && entry.getStatus().isStartable() ? this.createUsableView(form, running) : this.createUnusableView();
    }

    /**
     * @return the view's root node
     */
    public @NotNull Region getRoot() {
        return this.root;
    }

    private @NotNull Region createUsableView(@NotNull SettingsForm form, @NotNull ObservableBooleanValue running) {
        VBox aboutPanel = this.createAboutPanel();
        VBox settingsPanel = this.createSettingsPanel(form, running);
        HBox lower = ComponentUtils.row(ComponentUtils.Spacing.L, aboutPanel, settingsPanel);
        VBox detail;

        aboutPanel.setPrefWidth(ABOUT_PANEL_WIDTH);
        aboutPanel.setMinWidth(ABOUT_PANEL_WIDTH);
        HBox.setHgrow(settingsPanel, Priority.ALWAYS);
        lower.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(lower, Priority.ALWAYS);

        detail = ComponentUtils.column(ComponentUtils.Spacing.L, this.createBanner());

        if (this.updateRelease != null) {
            detail.getChildren().add(this.createUpdateNotice(this.updateRelease));
        }

        detail.getChildren().add(lower);
        HBox.setHgrow(detail, Priority.ALWAYS);

        return detail;
    }

    private @NotNull HBox createUpdateNotice(@NotNull String release) {
        Button update = ComponentUtils.smallButton(this.texts.get("mods.update.start"), ComponentUtils.ButtonRole.PRIMARY);

        update.setOnAction(event -> this.actions.onUpdate(this.entry));

        return ComponentUtils.notice(ComponentUtils.Tone.ACCENT, this.texts.format("mods.update.available", release), update);
    }

    private @NotNull Button createRemoveButton() {
        Button remove = ComponentUtils.button(this.texts.get("mods.remove"), ComponentUtils.ButtonRole.DANGER);

        remove.setOnAction(event -> this.actions.onRemove(this.entry));

        return remove;
    }

    private @NotNull StackPane createBanner() {
        ModManifest manifest = this.requireManifest();
        Image bannerImage = this.art.getBanner(this.entry);
        VBox heading = ComponentUtils.column(ComponentUtils.Spacing.XS,
                                             ComponentUtils.label(this.texts.localize(manifest.getName()), "text-display"),
                                             ComponentUtils.label(this.texts.format("detail.version.line", manifest.getVersion(),
                                                                                    String.join(", ", manifest.getAuthors())),
                                                                  "text-subtitle", "text-muted"));
        HBox caption = ComponentUtils.row(ComponentUtils.Spacing.L,
                                          ComponentUtils.thumbnail(this.art.getIcon(this.entry, BANNER_ICON_SIZE), BANNER_ICON_SIZE, BANNER_ICON_SIZE),
                                          heading,
                                          ComponentUtils.growingSpacer(),
                                          ComponentUtils.statusBadge(this.entry.getStatus(), this.texts));
        StackPane banner = new StackPane();

        heading.getStyleClass().add("banner-heading");

        if (bannerImage != null) {
            Region background = new Region();

            // The banner covers the card at any width: scaled to cover, centred and cropped.
            background.setBackground(new Background(new BackgroundImage(bannerImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                                                        BackgroundPosition.CENTER, BANNER_COVER)));
            banner.getChildren().add(background);
        }

        caption.getStyleClass().add("banner-caption");
        caption.setMaxHeight(Region.USE_PREF_SIZE);
        banner.getChildren().add(caption);
        StackPane.setAlignment(caption, Pos.BOTTOM_LEFT);
        banner.getStyleClass().add("banner");
        // Every mod's banner has the same height, with or without a banner image or an icon.
        banner.setMinHeight(BANNER_HEIGHT);
        banner.setPrefHeight(BANNER_HEIGHT);
        banner.setMaxHeight(BANNER_HEIGHT);
        banner.setMaxWidth(Double.MAX_VALUE);

        return banner;
    }

    private @NotNull VBox createAboutPanel() {
        ModManifest manifest = this.requireManifest();
        GridPane meta = new GridPane();
        LocalizedText description = manifest.getDescription();
        String website = manifest.getWebsite();
        List<String[]> rows = new ArrayList<>();
        VBox body;
        VBox panel;

        rows.add(new String[]{this.texts.get("detail.id"), manifest.getId()});
        rows.add(new String[]{this.texts.get("detail.version"), manifest.getVersion().toString()});
        rows.add(new String[]{this.texts.get("detail.engine"), manifest.getEngine().toString()});
        rows.add(new String[]{this.texts.get("detail.authors"), String.join(", ", manifest.getAuthors())});

        if (manifest.getLicense() != null) {
            rows.add(new String[]{this.texts.get("detail.license"), manifest.getLicense()});
        }

        rows.add(new String[]{this.texts.get("detail.file"), this.entry.getJarName()});
        meta.getStyleClass().addAll("panel-sunken", "spacing-s");

        for (int row = 0; row < rows.size(); row++) {
            Label key = ComponentUtils.label(rows.get(row)[0], "meta-key");
            Label value = ComponentUtils.label(rows.get(row)[1], "meta-value");

            key.setMinWidth(Region.USE_PREF_SIZE);
            value.setWrapText(true);
            meta.addRow(row, key, value);
        }

        if (website != null) {
            Hyperlink link = new Hyperlink(website);
            Label key = ComponentUtils.label(this.texts.get("detail.website"), "meta-key");

            key.setMinWidth(Region.USE_PREF_SIZE);
            link.setOnAction(event -> this.actions.onOpenWebsite(website));
            meta.addRow(rows.size(), key, link);
        }

        body = ComponentUtils.panelBody();

        if (description != null) {
            body.getChildren().add(ComponentUtils.paragraph(this.texts.localize(description), "text-body"));
        }

        body.getChildren().add(meta);

        if (!manifest.getImages().getScreenshots().isEmpty()) {
            HBox screenshots = ComponentUtils.row(ComponentUtils.Spacing.S);

            for (String screenshot : manifest.getImages().getScreenshots()) {
                screenshots.getChildren().add(ComponentUtils.thumbnail(this.art.getImage(this.entry, screenshot, SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT),
                                                                       SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT));
            }

            body.getChildren().addAll(ComponentUtils.label(this.texts.get("detail.screenshots"), "text-heading"), screenshots);
        }

        panel = ComponentUtils.panel(this.texts.get("detail.about"), body);
        panel.getChildren().add(ComponentUtils.panelFooter(ComponentUtils.growingSpacer(), this.createRemoveButton()));
        panel.setMaxHeight(Double.MAX_VALUE);

        return panel;
    }

    private @NotNull VBox createSettingsPanel(@NotNull SettingsForm form, @NotNull ObservableBooleanValue running) {
        SettingsFormView formView = new SettingsFormView(form, this.texts);
        ScrollPane scroll = new ScrollPane(formView.getRoot());
        VBox body = ComponentUtils.panelBody();
        Button reset = ComponentUtils.button(this.texts.get("detail.reset"), ComponentUtils.ButtonRole.SECONDARY);
        Button run = ComponentUtils.button(this.texts.get("detail.run"), ComponentUtils.ButtonRole.PRIMARY);
        Label blocker = new Label();
        Region blockerIcon = new Region();
        VBox panel;

        for (ModIssue issue : this.entry.getIssues()) {
            if (issue instanceof SettingsIssue settingsIssue && settingsIssue.getDefinition() == null) {
                body.getChildren().add(ComponentUtils.notice(ComponentUtils.Tone.WARNING, issue.describe(this.texts)));
            }
        }

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        body.getChildren().add(scroll);

        reset.setOnAction(event -> formView.resetToDefaults());
        run.disableProperty().bind(formView.invalidCountProperty().greaterThan(0).or(running));
        run.setOnAction(event -> {
            formView.validate();

            if (form.isValid()) {
                this.actions.onRun(this.entry, form);
            }
        });

        blockerIcon.getStyleClass().add("field-message-icon");
        blocker.getStyleClass().add("run-blocker");
        blocker.setGraphic(blockerIcon);
        blocker.textProperty().bind(Bindings.createStringBinding(() -> this.texts.formatCount("detail.blocked", formView.invalidCountProperty().get()),
                                                                 formView.invalidCountProperty()));
        blocker.visibleProperty().bind(formView.invalidCountProperty().greaterThan(0));

        panel = ComponentUtils.panel(this.texts.get("detail.settings"), body);
        panel.getChildren().add(ComponentUtils.panelFooter(blocker, ComponentUtils.growingSpacer(), reset, run));
        panel.setMaxHeight(Double.MAX_VALUE);

        return panel;
    }

    private @NotNull Region createUnusableView() {
        ModManifest manifest = this.entry.getManifest();
        VBox identityText = ComponentUtils.column(ComponentUtils.Spacing.XS,
                                                  ComponentUtils.label(ModArt.getName(this.entry, this.texts), "text-title"),
                                                  ComponentUtils.label(this.entry.getJar().toString(), "text-mono", "text-muted"));
        HBox badgeRow = ComponentUtils.row(ComponentUtils.Spacing.S, ComponentUtils.statusBadge(this.entry.getStatus(), this.texts));
        HBox identity = ComponentUtils.row(ComponentUtils.Spacing.L,
                                           ComponentUtils.thumbnail(this.art.getIcon(this.entry, ICON_SIZE), ICON_SIZE, ICON_SIZE),
                                           identityText);
        VBox report = new VBox(ComponentUtils.label(this.texts.get("detail.error.title"), "text-subtitle", "text-danger"));
        List<String> details = new ArrayList<>();
        Button copy = ComponentUtils.button(this.texts.get("detail.copy.problems"), ComponentUtils.ButtonRole.SECONDARY);
        Button run = ComponentUtils.button(this.texts.get("detail.run"), ComponentUtils.ButtonRole.PRIMARY);
        VBox body;
        VBox panel;

        if (manifest != null) {
            String identityLine = this.texts.format("detail.identity.line", manifest.getId(), manifest.getVersion(), manifest.getEngine());

            badgeRow.getChildren().add(ComponentUtils.label(identityLine, "text-caption"));
        }

        identityText.getChildren().add(badgeRow);
        report.getStyleClass().add("panel-danger");
        details.add(this.entry.getJar().toString());

        for (ModIssue issue : this.entry.getIssues()) {
            Node reason = ComponentUtils.column(ComponentUtils.Spacing.XS, ComponentUtils.paragraph(issue.describe(this.texts)),
                                                ComponentUtils.paragraph(issue.describeDetail(), "text-caption", "text-mono"));
            Node marker = ComponentUtils.statusMarker(ComponentUtils.getTone(issue.getStatus()));

            report.getChildren().add(ComponentUtils.row(ComponentUtils.Spacing.S, marker, reason));
            details.add(issue.describeDetail());
        }

        copy.setOnAction(event -> this.actions.onCopy(String.join(LINE_SEPARATOR, details)));
        run.setDisable(true);

        body = ComponentUtils.panelBody(identity);

        if (this.updateRelease != null) {
            body.getChildren().add(this.createUpdateNotice(this.updateRelease));
        }

        body.getChildren().add(report);
        panel = ComponentUtils.panel(this.texts.get("detail.title"), body);
        panel.getChildren().add(ComponentUtils.panelFooter(copy, this.createRemoveButton(), ComponentUtils.growingSpacer(), run));
        HBox.setHgrow(panel, Priority.ALWAYS);
        panel.setMaxHeight(Double.MAX_VALUE);

        return panel;
    }

    private @NotNull ModManifest requireManifest() {
        ModManifest manifest = this.entry.getManifest();

        if (manifest == null) {
            throw Exceptions.newIllegalStateException("The mod {0} has no valid manifest.", this.entry.getJar());
        }

        return manifest;
    }
}
