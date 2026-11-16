package org.fogofwarlabs.sc4j.launcher.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.i18n.UiLanguage;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseClient;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseRepository;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseServerScenario;
import org.fogofwarlabs.sc4j.launcher.update.LauncherRelease;
import org.fogofwarlabs.sc4j.launcher.update.LauncherUpdater;
import org.fogofwarlabs.sc4j.launcher.update.PackageSwap;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tests the Update dialog offscreen, against a local stand-in for GitHub: Download fetches and unpacks the user package, and Restart waits
 * while a game runs.
 */
final class LauncherUpdateDialogTest {
    private static final @NotNull String REPOSITORY = "owner/sc4j";
    private static final @NotNull String TAG = "v1.2.0";
    private static final @NotNull String PACKAGE = "SC4J-1.2.0";
    private static final @NotNull String RUNNING_VERSION = "1.1.0";
    private static final double SCENE_WIDTH = 1280;
    private static final double SCENE_HEIGHT = 800;

    @BeforeAll
    static void startToolkit() {
        LauncherFxScenario.startToolkit();
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "ko"})
    void download_should_unpack_the_package_and_restart_should_wait_for_a_running_game(@NotNull String languageCode,
                                                                                       @TempDir @NotNull Path directory) throws Exception {
        UiLanguage language = languageCode.equals(UiLanguage.KOREAN.getCode()) ? UiLanguage.KOREAN : UiLanguage.ENGLISH;
        LauncherTexts texts = LauncherTexts.of(language);
        InstallLayout install = InstallLayout.of(directory);
        AtomicInteger closed = new AtomicInteger();

        try (ReleaseServerScenario server = ReleaseServerScenario.start()) {
            LauncherUpdater updater = new LauncherUpdater(install, new ReleaseClient(server.getApi()));
            ReleaseRepository repository = ReleaseRepository.parse(REPOSITORY);
            LauncherRelease release;
            LauncherShell shell;

            Assertions.assertNotNull(repository);
            server.addCheckedRelease(REPOSITORY, TAG, Map.of(PACKAGE + ".zip", createPackage()));
            release = updater.findUpdate(repository, RUNNING_VERSION);
            Assertions.assertNotNull(release);

            shell = LauncherFxScenario.run(() -> {
                LauncherShell created = new LauncherShell(texts, install, 1, section -> {
                }, () -> {
                }, () -> {
                });

                ThemeUtils.apply(new Scene(created.getRoot(), SCENE_WIDTH, SCENE_HEIGHT), language);
                new LauncherUpdateDialog(created, texts, updater, release, new SimpleBooleanProperty(true), closed::incrementAndGet).open();
                LauncherFxScenario.saveSample(created.getRoot(), 1, "launcher-update-" + language.getCode());
                LauncherFxScenario.findButton(created.getRoot(), texts.get("update.download")).fire();

                return created;
            });
            LauncherFxScenario.await(() -> hasButton(shell.getRoot(), texts.get("update.restart")), "the package is ready");
            LauncherFxScenario.run(() -> {
                Assertions.assertTrue(hasLabel(shell.getRoot(), texts.format("update.ready", release.getVersion())));
                LauncherFxScenario.findButton(shell.getRoot(), texts.get("update.restart")).fire();
                Assertions.assertTrue(hasLabel(shell.getRoot(), texts.get("update.running")), "Restart waits for the game");
                shell.getRoot().applyCss();
                shell.getRoot().layout();
                LauncherFxScenario.saveSample(shell.getRoot(), 1, "launcher-update-ready-" + language.getCode());

                return null;
            });

            Assertions.assertEquals(0, closed.get());
        }
    }

    private static byte @NotNull [] createPackage() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (String item : PackageSwap.ITEMS) {
                zip.putNextEntry(new ZipEntry(PACKAGE + "/" + item));
                zip.write(item.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }

        return output.toByteArray();
    }

    private static boolean hasButton(@NotNull Node root, @NotNull String text) {
        for (Node node : root.lookupAll(".button")) {
            if (node instanceof Button button && button.getText().equals(text) && LauncherFxScenario.isShown(button)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasLabel(@NotNull Node root, @NotNull String text) {
        for (Node node : root.lookupAll(".label")) {
            if (node instanceof Label label && label.getText().equals(text) && LauncherFxScenario.isShown(label)) {
                return true;
            }
        }

        return false;
    }
}
