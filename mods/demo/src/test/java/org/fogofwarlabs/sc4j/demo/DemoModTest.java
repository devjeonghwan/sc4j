package org.fogofwarlabs.sc4j.demo;

import org.fogofwarlabs.sc4j.base.log.Logging;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.fogofwarlabs.sc4j.runtime.cli.ExitCode;
import org.fogofwarlabs.sc4j.runtime.session.RuntimeSession;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the demo's jar through the runtime: a headless run plays a computer game to its end and writes its replay, a headless run of that
 * replay reproduces the state hash of every game loop, and the runtime refuses a headless run of an unknown map and of a damaged replay. (A
 * run with a window opens the demo's client; its scenes are tested offscreen.)
 */
final class DemoModTest {
    private static final @NotNull String MOD_ID = "demo";
    private static final @NotNull String SEED = "20261107";

    @Test
    void a_headless_game_writes_a_replay_that_reproduces_every_game_loop(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = createInstall(directory);
        Path liveHashes = directory.resolve("live-hashes.txt");
        Path replayHashes = directory.resolve("replay-hashes.txt");
        Path replay = install.getStorageDirectory(MOD_ID).resolve(DemoHeadlessRunner.REPLAY_PATH);

        Assertions.assertEquals(ExitCode.SUCCESS, run(install, "--headless", "--", DemoHeadlessRunner.SEED_ARGUMENT, SEED,
                                                      DemoHeadlessRunner.FRAME_HASHES_ARGUMENT, liveHashes.toString()));
        Assertions.assertTrue(Files.isRegularFile(replay));
        Assertions.assertEquals(ExitCode.SUCCESS, run(install, "--headless", "--replay", replay.toString(), "--",
                                                      DemoHeadlessRunner.FRAME_HASHES_ARGUMENT, replayHashes.toString()));

        List<String> live = Files.readAllLines(liveHashes);

        Assertions.assertFalse(live.isEmpty());
        Assertions.assertEquals(live, Files.readAllLines(replayHashes));
        Assertions.assertTrue(Files.readString(install.getLogDirectory(MOD_ID).resolve(Logging.FILE_NAME)).contains("DEMO: replay"));
    }

    @Test
    void the_runtime_refuses_an_unknown_map_and_a_damaged_replay(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = createInstall(directory);
        Path damaged = Files.write(directory.resolve("damaged.sc4r"), new byte[]{'S', 'C', '4', 'R', 0, 1, 0, 0, 0, 0});

        Assertions.assertEquals(ExitCode.MOD_FAILURE, run(install, "--headless", "--", DemoHeadlessRunner.MAP_ARGUMENT, "Nowhere"));
        Assertions.assertEquals(ExitCode.MOD_FAILURE, run(install, "--headless", "--replay", damaged.toString()));
    }

    private static @NotNull ExitCode run(@NotNull InstallLayout install, @NotNull String @NotNull ... options) {
        List<String> arguments = new ArrayList<>(List.of("--install", install.getRoot().toString(),
                                                         "--mod", install.getModsDirectory().resolve(MOD_ID + ".jar").toString()));

        arguments.addAll(List.of(options));

        return RuntimeSession.run(arguments.toArray(String[]::new));
    }

    private static @NotNull InstallLayout createInstall(@NotNull Path root) throws IOException {
        InstallLayout install = InstallLayout.of(root);

        Files.createDirectories(install.getModsDirectory());
        Files.copy(DemoTestResources.getModJar(), install.getModsDirectory().resolve(MOD_ID + ".jar"));

        return install;
    }
}
