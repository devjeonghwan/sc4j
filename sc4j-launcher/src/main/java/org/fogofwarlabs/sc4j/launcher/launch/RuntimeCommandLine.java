package org.fogofwarlabs.sc4j.launcher.launch;

import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.PeerStore;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListFile;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the command line that starts the runtime for one mod:
 * {@snippet lang = "text":
 * <install>/runtime/bin/sc4j-runtime
 *     --install <install>
 *     --mod <install>/mods/<jar>
 *     --settings <install>/userdata/settings/<modId>.json
 *     --storage <install>/userdata/mods/<modId>
 *     --log <install>/userdata/logs/<modId>
 *     --account <install>/userdata/beaconlink/accounts/<account id>
 *     --seeds <install>/userdata/beaconlink/nodes.json, or <install>/launcher/seeds.json until the user saved a node list
 *     --peers <install>/userdata/beaconlink/peers.json
 *}
 * The runtime takes its Beacon.Link account, seeds and peers file only from these options; the seeds follow the one seed rule of the
 * install's programs, and {@code --seeds} is left out while neither list exists. The start script of the runtime distribution starts its own
 * JVM, so the runtime always runs in a separate process. Each path is one argument; the process API quotes arguments with spaces.
 */
public final class RuntimeCommandLine {
    /**
     * The runtime's start script on Windows.
     */
    public static final @NotNull String WINDOWS_SCRIPT = "sc4j-runtime.bat";

    /**
     * The runtime's start script on other systems.
     */
    public static final @NotNull String UNIX_SCRIPT = "sc4j-runtime";

    private static final @NotNull String INSTALL_OPTION = "--install";
    private static final @NotNull String MOD_OPTION = "--mod";
    private static final @NotNull String SETTINGS_OPTION = "--settings";
    private static final @NotNull String STORAGE_OPTION = "--storage";
    private static final @NotNull String LOG_OPTION = "--log";
    private static final @NotNull String ACCOUNT_OPTION = "--account";
    private static final @NotNull String SEEDS_OPTION = "--seeds";
    private static final @NotNull String PEERS_OPTION = "--peers";
    private static final @NotNull String QUOTE = "\"";
    private static final @NotNull String ARGUMENT_SEPARATOR = " ";
    private static final @NotNull String WINDOWS_NAME_PREFIX = "windows";
    private static final @NotNull String OPERATING_SYSTEM_PROPERTY = "os.name";

    /**
     * Creates a {@link RuntimeCommandLine} instance for a mod of an install.
     *
     * @param install          the install
     * @param modId            the mod id, which names the settings file, the storage folder and the log folder
     * @param modJar           the mod jar
     * @param accountDirectory the folder of the account the game runs with
     * @param windows          whether the runtime runs on Windows (start script {@value #WINDOWS_SCRIPT})
     * @return the created {@link RuntimeCommandLine} instance
     */
    public static @NotNull RuntimeCommandLine create(@NotNull InstallLayout install, @NotNull String modId, @NotNull Path modJar,
                                                     @NotNull Path accountDirectory, boolean windows) {
        List<String> arguments = new ArrayList<>();
        Path settingsFile = install.getSettingsFile(modId);
        Path seedsFile = new NodeListFile(install.getNodesFile()).selectExistingOr(new NodeListFile(install.getSeedsFile())).getPath();

        arguments.add(install.getRuntimeScript(windows ? WINDOWS_SCRIPT : UNIX_SCRIPT).toString());
        arguments.add(INSTALL_OPTION);
        arguments.add(install.getRoot().toString());
        arguments.add(MOD_OPTION);
        arguments.add(modJar.toAbsolutePath().normalize().toString());
        arguments.add(SETTINGS_OPTION);
        arguments.add(settingsFile.toString());
        arguments.add(STORAGE_OPTION);
        arguments.add(install.getStorageDirectory(modId).toString());
        arguments.add(LOG_OPTION);
        arguments.add(install.getLogDirectory(modId).toString());
        arguments.add(ACCOUNT_OPTION);
        arguments.add(accountDirectory.toAbsolutePath().normalize().toString());

        if (Files.exists(seedsFile)) {
            arguments.add(SEEDS_OPTION);
            arguments.add(seedsFile.toString());
        }

        arguments.add(PEERS_OPTION);
        arguments.add(install.getBeaconLinkDirectory().resolve(PeerStore.FILE_NAME).toString());

        return new RuntimeCommandLine(arguments, install.getRoot(), settingsFile, install.getLogDirectory(modId));
    }

    /**
     * @return {@code true} if the launcher runs on Windows, {@code false} otherwise
     */
    public static boolean isWindows() {
        return System.getProperty(OPERATING_SYSTEM_PROPERTY, "").toLowerCase(Locale.ROOT).startsWith(WINDOWS_NAME_PREFIX);
    }


    private final @NotNull List<@NotNull String> arguments;
    private final @NotNull Path workingDirectory;
    private final @NotNull Path settingsFile;
    private final @NotNull Path logDirectory;

    /**
     * Creates a {@link RuntimeCommandLine} instance.
     *
     * @param arguments        the start script followed by its arguments
     * @param workingDirectory the install folder, the runtime's working folder
     * @param settingsFile     the settings value file the runtime reads
     * @param logDirectory     the folder of the runtime's log files
     */
    private RuntimeCommandLine(@NotNull List<@NotNull String> arguments, @NotNull Path workingDirectory, @NotNull Path settingsFile,
                               @NotNull Path logDirectory) {
        this.arguments        = List.copyOf(arguments);
        this.workingDirectory = workingDirectory;
        this.settingsFile     = settingsFile;
        this.logDirectory     = logDirectory;
    }

    /**
     * @return the start script followed by its arguments, one path per argument
     */
    public @NotNull List<@NotNull String> getArguments() {
        return this.arguments;
    }

    /**
     * @return the start script of the runtime
     */
    public @NotNull Path getScript() {
        return Path.of(this.arguments.getFirst());
    }

    /**
     * @return the install folder, the runtime's working folder
     */
    public @NotNull Path getWorkingDirectory() {
        return this.workingDirectory;
    }

    /**
     * @return the settings value file the runtime reads
     */
    public @NotNull Path getSettingsFile() {
        return this.settingsFile;
    }

    /**
     * @return the folder of the runtime's log files
     */
    public @NotNull Path getLogDirectory() {
        return this.logDirectory;
    }

    /**
     * Describes the command line on one line for the user, with arguments that hold spaces in double quotes.
     *
     * @return the command line text
     */
    public @NotNull String describe() {
        List<String> parts = new ArrayList<>(this.arguments.size());

        for (String argument : this.arguments) {
            parts.add(argument.isEmpty() || argument.contains(ARGUMENT_SEPARATOR) ? QUOTE + argument + QUOTE : argument);
        }

        return String.join(ARGUMENT_SEPARATOR, parts);
    }
}
