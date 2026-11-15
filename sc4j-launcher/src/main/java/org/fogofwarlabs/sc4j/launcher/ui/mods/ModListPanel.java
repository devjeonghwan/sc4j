package org.fogofwarlabs.sc4j.launcher.ui.mods;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fogofwarlabs.sc4j.launcher.catalog.ModCatalog;
import org.fogofwarlabs.sc4j.launcher.catalog.ModEntry;
import org.fogofwarlabs.sc4j.launcher.catalog.ModIssue;
import org.fogofwarlabs.sc4j.launcher.catalog.ModStatus;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.ui.ComponentUtils;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Shows the mod list: one cell per jar of {@code mods/} with its icon, name, id, version, authors, the first reason of a
 * warning or error, and the status badge. The footer opens the folder and scans it again.
 */
public final class ModListPanel {
    private static final double PANEL_WIDTH = 360;
    private static final double ICON_SIZE = 44;
    private static final double ISSUE_WIDTH = 200;
    private static final @NotNull String SEPARATOR = " | ";

    /**
     * Shows one mod of the list.
     */
    private final class ModCell extends ListCell<ModEntry> {
        /**
         * Updates the item associated with this cell.
         *
         * @param entry the new item for the cell
         * @param empty whether or not this cell represents data from the list
         */
        @Override
        protected void updateItem(@Nullable ModEntry entry, boolean empty) {
            super.updateItem(entry, empty);

            if (empty || entry == null) {
                this.setText(null);
                this.setGraphic(null);

                return;
            }

            ModManifest manifest = entry.getManifest();
            VBox text = ComponentUtils.column(ComponentUtils.Spacing.XS,
                                              ComponentUtils.label(ModArt.getName(entry, ModListPanel.this.texts), "list-title"));

            if (manifest != null) {
                String identity = manifest.getId() + SEPARATOR + manifest.getVersion() + SEPARATOR + String.join(", ", manifest.getAuthors());

                text.getChildren().add(ComponentUtils.label(identity, "text-caption"));
            } else {
                text.getChildren().add(ComponentUtils.label(ModListPanel.this.texts.get("mods.no.manifest"), "text-caption"));
            }

            if (!entry.getIssues().isEmpty()) {
                ModIssue issue = entry.getIssues().getFirst();
                Label reason = ComponentUtils.label(issue.describe(ModListPanel.this.texts), "text-caption",
                                                    issue.getStatus() == ModStatus.ERROR ? "text-danger" : "text-warning");

                reason.setMaxWidth(ISSUE_WIDTH);
                text.getChildren().add(reason);
            }

            Image icon = ModListPanel.this.art.getIcon(entry, ICON_SIZE);
            HBox graphic = ComponentUtils.row(ComponentUtils.Spacing.M, ComponentUtils.thumbnail(icon, ICON_SIZE, ICON_SIZE), text,
                                              ComponentUtils.growingSpacer(), ComponentUtils.statusBadge(entry.getStatus(), ModListPanel.this.texts));

            ComponentUtils.setCellGraphic(this, graphic);
        }
    }


    private final @NotNull LauncherTexts texts;
    private final @NotNull ModArt art;
    private final @NotNull ListView<ModEntry> list;
    private final @NotNull VBox root;

    /**
     * Creates a {@link ModListPanel} instance.
     *
     * @param catalog      the scanned mods
     * @param texts        the launcher texts of the display language
     * @param art          the decoded mod images
     * @param onSelect     called with the entry the user selects
     * @param onOpenFolder called when the user opens the mods folder
     * @param onRefresh    called when the user asks to scan the folder again
     */
    public ModListPanel(@NotNull ModCatalog catalog, @NotNull LauncherTexts texts, @NotNull ModArt art,
                        @NotNull Consumer<@NotNull ModEntry> onSelect, @NotNull Runnable onOpenFolder, @NotNull Runnable onRefresh) {
        Button openFolder = ComponentUtils.smallButton(texts.get("mods.open.folder"), ComponentUtils.ButtonRole.SECONDARY);
        Button refresh = ComponentUtils.smallButton(texts.get("mods.refresh"), ComponentUtils.ButtonRole.SECONDARY);
        VBox body;

        this.texts = texts;
        this.art   = art;
        this.list  = new ListView<>();

        this.list.getItems().setAll(catalog.getEntries());
        this.list.setCellFactory(view -> new ModCell());
        this.list.setPlaceholder(ComponentUtils.paragraph(texts.format("mods.empty", catalog.getModsDirectory()), "text-caption"));
        this.list.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> {
            if (current != null) {
                onSelect.accept(current);
            }
        });
        VBox.setVgrow(this.list, Priority.ALWAYS);

        openFolder.setOnAction(event -> onOpenFolder.run());
        refresh.setOnAction(event -> onRefresh.run());

        body      = ComponentUtils.flushPanelBody(this.list);
        this.root = ComponentUtils.panel(texts.get("mods.title"), body);
        this.root.getChildren().add(ComponentUtils.panelFooter(openFolder, ComponentUtils.growingSpacer(), refresh));
        this.root.setPrefWidth(PANEL_WIDTH);
        this.root.setMinWidth(PANEL_WIDTH);
        this.root.setMaxHeight(Double.MAX_VALUE);
    }

    /**
     * @return the panel's root node
     */
    public @NotNull VBox getRoot() {
        return this.root;
    }

    /**
     * Selects an entry, which calls the selection callback.
     *
     * @param entry the entry of the list
     */
    public void select(@NotNull ModEntry entry) {
        this.list.getSelectionModel().select(entry);
        this.list.scrollTo(entry);
    }
}
