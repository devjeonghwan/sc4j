package org.fogofwarlabs.sc4j.launcher.ui.options;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.BootstrapState;
import org.fogofwarlabs.sc4j.beaconlink.peer.reachability.Reachability;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.i18n.UiLanguage;
import org.fogofwarlabs.sc4j.launcher.peer.LauncherNodeRunner;
import org.fogofwarlabs.sc4j.launcher.peer.LauncherNodeStatus;
import org.fogofwarlabs.sc4j.launcher.peer.NodeRole;
import org.fogofwarlabs.sc4j.launcher.peer.NodeSettings;
import org.fogofwarlabs.sc4j.launcher.ui.LauncherFxScenario;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests the status lines of the Beacon.Link panel per node role: in the off role only the network state and the peer counts are shown; in
 * the automatic role the reachability and the addresses as well; a node that another launcher runs is named as such. The panel's node
 * runner is never started, so no socket is opened.
 */
final class BeaconLinkPanelTest {
    private static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    private static final @NotNull String LABEL_SELECTOR = ".label";
    private static final int LOCAL_PORT = 50_000;
    private static final int OBSERVED_PORT = 50_001;
    private static final int LIVE_PEERS = 3;
    private static final int LEARNED_PEERS = 2;
    private static final long FX_TIMEOUT_SECONDS = 30;

    @BeforeAll
    static void startToolkit() {
        LauncherFxScenario.startToolkit();
    }

    @Test
    void the_off_role_shows_only_the_network_state_and_the_peers(@TempDir @NotNull Path directory) throws Exception {
        LauncherTexts texts = LauncherTexts.of(UiLanguage.KOREAN);
        List<String> shown = showStatus(directory, texts, createStatus(new NodeSettings(NodeRole.OFF, NodeSettings.DEFAULT_PORT)), false);

        Assertions.assertTrue(shown.contains(texts.describeBootstrapState(BootstrapState.CONNECTED)), shown::toString);
        Assertions.assertTrue(shown.contains(texts.format("beaconlink.peers", LIVE_PEERS, LEARNED_PEERS)), shown::toString);
        Assertions.assertFalse(shown.contains(texts.describeReachability(Reachability.NOT_REACHABLE, LOCAL_PORT)), shown::toString);
        Assertions.assertTrue(shown.stream().noneMatch(text -> text.contains(Integer.toString(LOCAL_PORT))), shown::toString);
    }

    @Test
    void the_automatic_role_shows_the_help_the_reachability_and_the_addresses(@TempDir @NotNull Path directory) throws Exception {
        LauncherTexts texts = LauncherTexts.of(UiLanguage.KOREAN);
        List<String> shown = showStatus(directory, texts, createStatus(NodeSettings.DEFAULT), false);

        Assertions.assertTrue(shown.contains(texts.get("beaconlink.node.help")), shown::toString);
        Assertions.assertTrue(shown.contains(texts.describeBootstrapState(BootstrapState.CONNECTED)), shown::toString);
        Assertions.assertTrue(shown.contains(texts.format("beaconlink.peers", LIVE_PEERS, LEARNED_PEERS)), shown::toString);
        Assertions.assertTrue(shown.contains(texts.describeReachability(Reachability.NOT_REACHABLE, LOCAL_PORT)), shown::toString);
        Assertions.assertTrue(shown.stream().anyMatch(text -> text.contains(Integer.toString(LOCAL_PORT)) &&
                                                              text.contains(Integer.toString(OBSERVED_PORT))), shown::toString);
    }

    @Test
    void a_node_of_another_launcher_is_named_as_such(@TempDir @NotNull Path directory) throws Exception {
        LauncherTexts texts = LauncherTexts.of(UiLanguage.ENGLISH);
        List<String> shown = showStatus(directory, texts, null, true);

        Assertions.assertTrue(shown.contains(texts.get("beaconlink.elsewhere")), shown::toString);
        Assertions.assertFalse(shown.contains(texts.format("beaconlink.peers", LIVE_PEERS, LEARNED_PEERS)), shown::toString);
    }

    private static @NotNull LauncherNodeStatus createStatus(@NotNull NodeSettings settings) {
        return new LauncherNodeStatus(settings, new InetSocketAddress(LOOPBACK, LOCAL_PORT), new InetSocketAddress(LOOPBACK, OBSERVED_PORT),
                                      Reachability.NOT_REACHABLE, BootstrapState.CONNECTED, LIVE_PEERS, LEARNED_PEERS);
    }

    /**
     * Shows a status in a new panel and returns the texts of its shown labels.
     */
    private static @NotNull List<@NotNull String> showStatus(@NotNull Path directory, @NotNull LauncherTexts texts,
                                                             @Nullable LauncherNodeStatus status, boolean heldElsewhere) throws InterruptedException,
                                                                                                                                ExecutionException,
                                                                                                                                TimeoutException {
        CompletableFuture<List<String>> result = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                BeaconLinkPanel panel = new BeaconLinkPanel(texts, new LauncherNodeRunner(InstallLayout.of(directory)), new VBox());
                List<String> shown = new ArrayList<>();

                panel.showStatus(status, heldElsewhere, null);

                for (Node node : panel.getRoot().lookupAll(LABEL_SELECTOR)) {
                    if (node instanceof Label label && label.isVisible() && !label.getText().isEmpty()) {
                        shown.add(label.getText());
                    }
                }

                result.complete(shown);
            } catch (RuntimeException | Error exception) {
                result.completeExceptionally(exception);
            }
        });

        return result.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
