package org.fogofwarlabs.sc4j.launcher.ui;

import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.launch.RuntimeCommandLine;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseException;
import org.fogofwarlabs.sc4j.launcher.update.LauncherRelease;
import org.fogofwarlabs.sc4j.launcher.update.LauncherUpdater;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shows the Update dialog of a newer launcher version: Download fetches the user package with the dialog's progress, checks it and unpacks
 * it; Restart then starts the updater and closes the launcher, which the updater starts again once the programs are replaced. Restart waits
 * while a game runs, as the update replaces the runtime the game uses.
 */
public final class LauncherUpdateDialog {
    private final @NotNull LauncherShell shell;
    private final @NotNull LauncherTexts texts;
    private final @NotNull LauncherUpdater updater;
    private final @NotNull LauncherRelease release;
    private final @NotNull ObservableBooleanValue running;
    private final @NotNull Runnable close;

    /**
     * Creates a {@link LauncherUpdateDialog} instance.
     *
     * @param shell   shows the dialog
     * @param texts   the launcher texts of the display language
     * @param updater downloads, unpacks and applies the update
     * @param release the newer version
     * @param running {@code true} while a game runs
     * @param close   closes the launcher once the updater runs
     */
    public LauncherUpdateDialog(@NotNull LauncherShell shell, @NotNull LauncherTexts texts, @NotNull LauncherUpdater updater,
                                @NotNull LauncherRelease release, @NotNull ObservableBooleanValue running, @NotNull Runnable close) {
        this.shell   = shell;
        this.texts   = texts;
        this.updater = updater;
        this.release = release;
        this.running = running;
        this.close   = close;
    }

    /**
     * Opens the dialog.
     */
    public void open() {
        ModalDialog shown = new ModalDialog(this.texts.get("update.title"));
        Label text = ComponentUtils.paragraph(this.texts.format("update.available", this.release.getVersion()), "text-body");
        Button cancel = ComponentUtils.button(this.texts.get("update.cancel"), ComponentUtils.ButtonRole.SECONDARY);
        Button download = ComponentUtils.button(this.texts.get("update.download"), ComponentUtils.ButtonRole.PRIMARY);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        shown.addContent(text);
        shown.setButtons(cancel, download);
        cancel.setOnAction(event -> {
            cancelled.set(true);
            this.shell.closeDialog(shown);
        });
        download.setOnAction(event -> {
            download.setDisable(true);
            shown.runWork(this.texts.get("update.downloading"), cancelled, progress -> this.updater.download(this.release, progress),
                          packageFile -> this.unpack(shown, text, cancelled, download, packageFile), this::describe, () -> download.setDisable(false));
        });

        this.shell.showDialog(shown);
        download.requestFocus();
    }

    private void unpack(@NotNull ModalDialog shown, @NotNull Label text, @NotNull AtomicBoolean cancelled, @NotNull Button download,
                        @NotNull Path packageFile) {
        shown.runWork(this.texts.get("update.unpacking"), cancelled, progress -> this.updater.unpack(this.release, packageFile),
                      packageRoot -> this.showReady(shown, text, packageRoot), this::describe, () -> download.setDisable(false));
    }

    private void showReady(@NotNull ModalDialog shown, @NotNull Label text, @NotNull Path packageRoot) {
        Button later = ComponentUtils.button(this.texts.get("update.later"), ComponentUtils.ButtonRole.SECONDARY);
        Button restart = ComponentUtils.button(this.texts.get("update.restart"), ComponentUtils.ButtonRole.PRIMARY);

        text.setText(this.texts.format("update.ready", this.release.getVersion()));
        shown.setButtons(later, restart);
        later.setOnAction(event -> this.shell.closeDialog(shown));
        restart.setOnAction(event -> this.restart(shown, packageRoot));
        restart.requestFocus();
    }

    private void restart(@NotNull ModalDialog shown, @NotNull Path packageRoot) {
        if (this.running.get()) {
            shown.showMessage(this.texts.get("update.running"));

            return;
        }

        try {
            this.updater.startUpdater(packageRoot, ProcessHandle.current().pid(), RuntimeCommandLine.isWindows());
        } catch (IOException exception) {
            shown.showMessage(this.describe(exception));

            return;
        }

        this.close.run();
    }

    private @NotNull String describe(@NotNull Exception exception) {
        return exception instanceof ReleaseException problem ? this.texts.describeReleaseProblem(problem.getProblem())
                                                             : this.texts.format("update.failed", exception.getMessage());
    }
}
