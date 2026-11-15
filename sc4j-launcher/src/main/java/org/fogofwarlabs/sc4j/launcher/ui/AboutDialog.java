package org.fogofwarlabs.sc4j.launcher.ui;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.text.LocalizedText;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Shows the About box: the SC4J logo, the launcher and engine API versions, the publisher, SC4J's licence and fonts, the Beacon.Link privacy
 * statement (what other players see, what this install keeps), then the legal notices the installed mods declare, each mod's notices under
 * its name.
 */
public final class AboutDialog {
    private static final double DIALOG_WIDTH = 580;
    private static final double SC4J_LOGO_SIZE = 76;
    private static final double PUBLISHER_LOGO_SIZE = 44;
    private static final @NotNull String PUBLISHER_NAME = "Fog of War Labs";
    private static final @NotNull String PUBLISHER_WEBSITE = "https://github.com/fogofwarlabs";

    /**
     * Creates the notices of the mods in the display language (with its fallbacks), each mod's under its name; a mod without notices is left
     * out, and a mod id is shown once.
     *
     * @param texts the launcher texts of the display language
     * @param mods  the manifests of the installed mods, in listed order
     * @return the created column, empty when no mod declares notices
     */
    private static @NotNull VBox createModNotices(@NotNull LauncherTexts texts, @NotNull List<@NotNull ModManifest> mods) {
        VBox column = ComponentUtils.column(ComponentUtils.Spacing.M);
        Set<String> shownIds = new HashSet<>();

        for (ModManifest mod : mods) {
            if (mod.getNotices().isEmpty() || !shownIds.add(mod.getId())) {
                continue;
            }

            VBox modColumn = ComponentUtils.column(ComponentUtils.Spacing.XS, ComponentUtils.label(texts.localize(mod.getName()), "text-heading"));

            for (LocalizedText notice : mod.getNotices()) {
                modColumn.getChildren().add(ComponentUtils.paragraph(texts.localize(notice), "text-caption"));
            }

            column.getChildren().add(modColumn);
        }

        return column;
    }


    private final @NotNull Dialog<ButtonType> dialog;

    /**
     * Creates a {@link AboutDialog} instance.
     *
     * @param owner       the launcher window
     * @param texts       the launcher texts of the display language
     * @param version     the launcher version
     * @param imageScale  the screen pixels per logical unit the logos are decoded for
     * @param mods        the manifests of the installed mods, whose notices are shown in listed order
     * @param openWebsite opens a web address in the system browser
     */
    public AboutDialog(@NotNull Window owner, @NotNull LauncherTexts texts, @NotNull String version, double imageScale,
                       @NotNull List<@NotNull ModManifest> mods, @NotNull Consumer<@NotNull String> openWebsite) {
        DialogPane pane = new DialogPane();
        Hyperlink website = new Hyperlink(PUBLISHER_WEBSITE);
        Label versionLabel = ComponentUtils.label(texts.format("about.version", version, EngineVersion.CURRENT), "text-caption");
        Label titleLabel = ComponentUtils.label(texts.get("about.title"), "text-title");
        HBox header = ComponentUtils.row(ComponentUtils.Spacing.L,
                                         ComponentUtils.imageView(ThemeUtils.loadImage(ThemeUtils.SC4J_LOGO, SC4J_LOGO_SIZE, imageScale), SC4J_LOGO_SIZE),
                                         ComponentUtils.column(ComponentUtils.Spacing.XS, titleLabel, versionLabel));
        Image publisherLogo = ThemeUtils.loadImage(ThemeUtils.FOG_OF_WAR_LABS_LOGO, PUBLISHER_LOGO_SIZE, imageScale);
        HBox publisher = ComponentUtils.row(ComponentUtils.Spacing.M,
                                            ComponentUtils.imageView(publisherLogo, PUBLISHER_LOGO_SIZE),
                                            ComponentUtils.column(ComponentUtils.Spacing.XS, ComponentUtils.label(PUBLISHER_NAME, "text-subtitle"),
                                                                  website));
        VBox privacy = ComponentUtils.column(ComponentUtils.Spacing.XS,
                                             ComponentUtils.label(texts.get("about.privacy.title"), "text-heading"),
                                             ComponentUtils.paragraph(texts.get("about.privacy.address"), "text-caption"),
                                             ComponentUtils.paragraph(texts.get("about.privacy.shared"), "text-caption"),
                                             ComponentUtils.paragraph(texts.get("about.privacy.files"), "text-caption"));
        VBox content = ComponentUtils.column(ComponentUtils.Spacing.M,
                                             publisher,
                                             new Separator(),
                                             ComponentUtils.paragraph(texts.get("about.license"), "text-caption"),
                                             ComponentUtils.paragraph(texts.get("about.fonts"), "text-caption"),
                                             new Separator(),
                                             privacy);
        VBox modNotices = createModNotices(texts, mods);

        website.setOnAction(event -> openWebsite.accept(PUBLISHER_WEBSITE));
        header.getStyleClass().add("dialog-header");

        if (!modNotices.getChildren().isEmpty()) {
            content.getChildren().addAll(new Separator(), modNotices);
        }

        pane.setHeader(header);
        pane.setContent(content);
        pane.getButtonTypes().add(new ButtonType(texts.get("about.close"), ButtonBar.ButtonData.OK_DONE));
        pane.setPrefWidth(DIALOG_WIDTH);
        ThemeUtils.apply(pane, texts.getLanguage());

        this.dialog = new Dialog<>();
        this.dialog.initOwner(owner);
        this.dialog.setTitle(texts.get("about.title"));
        this.dialog.setDialogPane(pane);
    }

    /**
     * @return the dialog's pane: the header, the content and the Close button
     */
    public @NotNull DialogPane getPane() {
        return this.dialog.getDialogPane();
    }

    /**
     * Shows the dialog and waits until the user closes it.
     */
    public void showAndWait() {
        this.dialog.showAndWait();
    }
}
