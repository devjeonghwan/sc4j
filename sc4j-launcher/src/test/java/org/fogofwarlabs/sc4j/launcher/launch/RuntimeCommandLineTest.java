package org.fogofwarlabs.sc4j.launcher.launch;

import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests the runtime command line that the launcher builds: every install path of the mod and the Beacon.Link account folder, seeds by the one
 * seed rule and the shared peers file.
 */
final class RuntimeCommandLineTest {
    private static final @NotNull String ACCOUNT_ID = "0123456789abcdef";
    private static final @NotNull String NODE_LIST = "{\"format\": 1, \"nodes\": []}";

    private static @NotNull Path getAccountDirectory(@NotNull InstallLayout install) {
        return install.getBeaconLinkDirectory().resolve("accounts").resolve(ACCOUNT_ID);
    }

    @Test
    void create_should_pass_every_install_path_of_the_mod(@TempDir @NotNull Path directory) throws IOException {
        Path root = directory.resolve("Games").resolve("SC4J Install");
        InstallLayout install = InstallLayout.of(root);
        Path jar = root.resolve("mods").resolve("my mod.jar");
        Path userData = root.resolve("userdata");

        Files.createDirectories(install.getSeedsFile().getParent());
        Files.writeString(install.getSeedsFile(), NODE_LIST);

        RuntimeCommandLine commandLine = RuntimeCommandLine.create(install, "my-mod", jar, getAccountDirectory(install), true);

        Assertions.assertEquals(List.of(root.resolve("runtime").resolve("bin").resolve("sc4j-runtime.bat").toString(),
                                        "--install", root.toString(),
                                        "--mod", jar.toString(),
                                        "--settings", userData.resolve("settings").resolve("my-mod.json").toString(),
                                        "--storage", userData.resolve("mods").resolve("my-mod").toString(),
                                        "--log", userData.resolve("logs").resolve("my-mod").toString(),
                                        "--account", userData.resolve("beaconlink").resolve("accounts").resolve(ACCOUNT_ID).toString(),
                                        "--seeds", root.resolve("launcher").resolve("seeds.json").toString(),
                                        "--peers", userData.resolve("beaconlink").resolve("peers.json").toString()),
                                commandLine.getArguments());
        Assertions.assertEquals(root, commandLine.getWorkingDirectory());
        Assertions.assertEquals(userData.resolve("settings").resolve("my-mod.json"), commandLine.getSettingsFile());
        Assertions.assertEquals(userData.resolve("logs").resolve("my-mod"), commandLine.getLogDirectory());
    }

    @Test
    void create_should_pass_the_user_node_list_once_saved_and_no_seeds_without_a_list(@TempDir @NotNull Path directory) throws IOException {
        InstallLayout install = InstallLayout.of(directory);
        Path jar = directory.resolve("mods").resolve("demo.jar");

        Assertions.assertFalse(RuntimeCommandLine.create(install, "demo", jar, getAccountDirectory(install), true).getArguments().contains("--seeds"),
                               "without a list the peers file is the only seed source");

        Files.createDirectories(install.getNodesFile().getParent());
        Files.writeString(install.getNodesFile(), NODE_LIST);

        List<String> arguments = RuntimeCommandLine.create(install, "demo", jar, getAccountDirectory(install), true).getArguments();

        Assertions.assertEquals(install.getNodesFile().toString(), arguments.get(arguments.indexOf("--seeds") + 1));
    }

    @Test
    void create_should_use_the_unix_start_script_outside_windows(@TempDir @NotNull Path directory) {
        Path jar = directory.resolve("mods").resolve("demo.jar");
        InstallLayout install = InstallLayout.of(directory);
        RuntimeCommandLine commandLine = RuntimeCommandLine.create(install, "demo", jar, getAccountDirectory(install), false);

        Assertions.assertEquals(directory.resolve("runtime").resolve("bin").resolve("sc4j-runtime"), commandLine.getScript());
    }

    @Test
    void describe_should_quote_arguments_with_spaces(@TempDir @NotNull Path directory) {
        Path root = directory.resolve("SC4J Install");
        Path jar = root.resolve("mods").resolve("my-mod.jar");
        InstallLayout install = InstallLayout.of(root);
        RuntimeCommandLine commandLine = RuntimeCommandLine.create(install, "my-mod", jar, getAccountDirectory(install), true);
        String text = commandLine.describe();
        String script = root.resolve("runtime").resolve("bin").resolve("sc4j-runtime.bat").toString();

        Assertions.assertTrue(text.startsWith("\"" + script + "\" --install \"" + root + "\" --mod "), text);
        Assertions.assertEquals(15, commandLine.getArguments().size());
    }
}
