package org.fogofwarlabs.sc4j.launcher.launch;

import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.PeerStore;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListFile;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the command line that starts the runtime for one mod: the Java program, the arguments of the runtime's {@link #LAUNCH_FILE} and
 * the runtime's options:
 * {@snippet lang = "text":
 * <java home>/bin/java <the lines of <install>/runtime/bin/sc4j-runtime.launch, ${APP_HOME} being <install>/runtime>
 *     --install <install>
 *     --mod <install>/mods/<jar>
 *     --settings <install>/userdata/settings/<modId>.json
 *     --storage <install>/userdata/mods/<modId>
 *     --log <install>/userdata/logs/<modId>
 *     --account <install>/userdata/beaconlink/accounts/<account id>
 *     --seeds <install>/userdata/beaconlink/nodes.json, or <install>/launcher/seeds.json until the user saved a node list
 *     --peers <install>/userdata/beaconlink/peers.json
 *}
 * {@code --seeds} is left out while neither seed list exists. Each path is one argument. A program file the install lacks is reported by
 * {@link #getMissingProgram()}.
 */
public final class RuntimeCommandLine {
    /**
     * The file of the runtime distribution's {@code bin} folder that holds the Java arguments that start the runtime, one per line.
     */
    public static final @NotNull String LAUNCH_FILE = "sc4j-runtime.launch";

    /**
     * The token of {@link #LAUNCH_FILE} that stands for the runtime distribution's folder.
     */
    public static final @NotNull String APPLICATION_HOME_TOKEN = "${APP_HOME}";

    private static final @NotNull String WINDOWS_JAVA = "java.exe";
    private static final @NotNull String UNIX_JAVA = "java";
    private static final @NotNull String BINARY_FOLDER = "bin";
    private static final @NotNull String LIBRARY_FOLDER = "lib";
    private static final @NotNull String JAVA_HOME_PROPERTY = "java.home";
    private static final @NotNull String TEMPORARY_DIRECTORY_OPTION = "-Djava.io.tmpdir=";

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
     * @param javaHome         the Java runtime that runs the runtime
     * @param windows          whether the runtime runs on Windows (Java program {@value #WINDOWS_JAVA})
     * @return the created {@link RuntimeCommandLine} instance
     */
    public static @NotNull RuntimeCommandLine create(@NotNull InstallLayout install, @NotNull String modId, @NotNull Path modJar,
                                                     @NotNull Path accountDirectory, @NotNull Path javaHome, boolean windows) {
        List<String> arguments = new ArrayList<>();
        Path settingsFile = install.getSettingsFile(modId);
        Path seedsFile = new NodeListFile(install.getNodesFile()).selectExistingOr(new NodeListFile(install.getSeedsFile())).getPath();
        Path java = javaHome.resolve(BINARY_FOLDER).resolve(windows ? WINDOWS_JAVA : UNIX_JAVA);
        Path launchFile = install.getRuntimeScript(LAUNCH_FILE);
        Path libraries = install.getRuntimeDirectory().resolve(LIBRARY_FOLDER);
        Path missingProgram = null;

        arguments.add(java.toString());

        try {
            for (String line : Files.readAllLines(launchFile, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    arguments.add(line.strip().replace(APPLICATION_HOME_TOKEN, install.getRuntimeDirectory().toString()));
                }
            }
        } catch (IOException exception) {
            missingProgram = launchFile;
        }

        if (!Files.isRegularFile(java)) {
            missingProgram = java;
        } else if (missingProgram == null && !Files.isDirectory(libraries, LinkOption.NOFOLLOW_LINKS)) {
            missingProgram = libraries;
        }

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

        return new RuntimeCommandLine(arguments, install.getRoot(), settingsFile, install.getLogDirectory(modId), missingProgram);
    }

    /**
     * @return the Java runtime the launcher runs on
     */
    public static @NotNull Path getLauncherJavaHome() {
        return Path.of(System.getProperty(JAVA_HOME_PROPERTY));
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
    private final @Nullable Path missingProgram;

    /**
     * Creates a {@link RuntimeCommandLine} instance.
     *
     * @param arguments        the Java program followed by its arguments
     * @param workingDirectory the install folder, the runtime's working folder
     * @param settingsFile     the settings value file the runtime reads
     * @param logDirectory     the folder of the runtime's log files
     * @param missingProgram   the program file the install lacks, or {@code null} when it has them all
     */
    private RuntimeCommandLine(@NotNull List<@NotNull String> arguments, @NotNull Path workingDirectory, @NotNull Path settingsFile,
                               @NotNull Path logDirectory, @Nullable Path missingProgram) {
        this.arguments        = List.copyOf(arguments);
        this.workingDirectory = workingDirectory;
        this.settingsFile     = settingsFile;
        this.logDirectory     = logDirectory;
        this.missingProgram   = missingProgram;
    }

    /**
     * @return the Java program followed by its arguments, one path per argument
     */
    public @NotNull List<@NotNull String> getArguments() {
        return this.arguments;
    }

    /**
     * @return the Java program that starts the runtime
     */
    public @NotNull Path getProgram() {
        return Path.of(this.arguments.getFirst());
    }

    /**
     * @return the JVM's temporary folder the command line names, or {@code null} when it names none
     */
    public @Nullable Path getTemporaryDirectory() {
        for (String argument : this.arguments) {
            if (argument.startsWith(TEMPORARY_DIRECTORY_OPTION)) {
                return Path.of(argument.substring(TEMPORARY_DIRECTORY_OPTION.length()));
            }
        }

        return null;
    }

    /**
     * @return the Java program, the launch file or the runtime library folder that the install lacks, or {@code null} when it has them all
     */
    public @Nullable Path getMissingProgram() {
        return this.missingProgram;
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
