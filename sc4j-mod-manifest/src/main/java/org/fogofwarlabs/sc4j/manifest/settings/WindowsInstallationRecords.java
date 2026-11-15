package org.fogofwarlabs.sc4j.manifest.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Reads the Windows records of installed programs: the {@code InstallLocation} of the uninstall entries in the registry (the
 * machine's 64-bit and 32-bit views and the user's), which installers write, and the program folders
 * ({@code ProgramFiles(x86)}, {@code ProgramFiles}).
 * <p>
 * The registry is read with the system's {@code reg query} ({@code %SystemRoot%\System32\reg.exe}), read-only. Its output is in the
 * system's ANSI code page, so a location with characters outside that code page does not survive and fails the check of the directory
 * instead.
 */
final class WindowsInstallationRecords implements InstallationRecords {
    private static final @NotNull List<@NotNull String> UNINSTALL_KEYS = List.of(
            "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\",
            "HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\",
            "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\"
    );
    private static final @NotNull List<@NotNull String> PROGRAM_FOLDER_VARIABLES = List.of("ProgramFiles(x86)", "ProgramFiles", "ProgramW6432");

    private static final @NotNull String SYSTEM_ROOT_VARIABLE = "SystemRoot";
    private static final @NotNull String REGISTRY_PROGRAM = "System32\\reg.exe";
    private static final @NotNull String REGISTRY_COMMAND = "reg";
    private static final @NotNull String QUERY_ARGUMENT = "query";
    private static final @NotNull String VALUE_ARGUMENT = "/v";
    private static final @NotNull String INSTALL_LOCATION_VALUE = "InstallLocation";
    private static final @NotNull String STRING_TYPE = "REG_SZ";
    private static final @NotNull String NATIVE_ENCODING_PROPERTY = "native.encoding";
    private static final @NotNull String LINE_SEPARATOR_PATTERN = "\\R";
    private static final @NotNull String QUOTE = "\"";
    private static final @NotNull String OPERATING_SYSTEM_PROPERTY = "os.name";
    private static final @NotNull String WINDOWS_NAME_PREFIX = "windows";
    private static final long QUERY_SECONDS = 10L;

    /**
     * @return {@code true} if the program runs on Windows, {@code false} otherwise
     */
    static boolean isRunning() {
        return System.getProperty(OPERATING_SYSTEM_PROPERTY, "").toLowerCase(Locale.ROOT).startsWith(WINDOWS_NAME_PREFIX);
    }

    private static @Nullable Path parsePath(@NotNull String text) {
        String stripped = text.strip();

        if (stripped.startsWith(QUOTE) && stripped.endsWith(QUOTE) && stripped.length() > 1) {
            stripped = stripped.substring(1, stripped.length() - 1);
        }

        try {
            return stripped.isEmpty() ? null : Path.of(stripped);
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private static @NotNull String findRegistryCommand() {
        String systemRoot = System.getenv(SYSTEM_ROOT_VARIABLE);
        Path program = systemRoot == null ? null : parsePath(systemRoot);

        return program != null && Files.isRegularFile(program.resolve(REGISTRY_PROGRAM))
               ? program.resolve(REGISTRY_PROGRAM).toString()
               : REGISTRY_COMMAND;
    }

    private static @Nullable String queryInstallLocation(@NotNull String key) {
        Process process;

        try {
            process = new ProcessBuilder(findRegistryCommand(), QUERY_ARGUMENT, key, VALUE_ARGUMENT, INSTALL_LOCATION_VALUE).redirectErrorStream(true)
                                                                                                                            .start();
        } catch (IOException exception) {
            return null;
        }

        try (InputStream output = process.getInputStream()) {
            process.getOutputStream().close();

            // The query ends at once; its output is read to the end before the wait.
            byte[] contents = output.readAllBytes();

            if (!process.waitFor(QUERY_SECONDS, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return null;
            }

            Charset charset = Charset.forName(System.getProperty(NATIVE_ENCODING_PROPERTY, Charset.defaultCharset().name()), Charset.defaultCharset());

            for (String line : new String(contents, charset).split(LINE_SEPARATOR_PATTERN)) {
                String stripped = line.strip();
                int type = stripped.indexOf(STRING_TYPE);

                if (stripped.startsWith(INSTALL_LOCATION_VALUE) && type > 0) {
                    return stripped.substring(type + STRING_TYPE.length());
                }
            }

            return null;
        } catch (IOException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            return null;
        } finally {
            process.destroy();
        }
    }


    /**
     * Creates a {@link WindowsInstallationRecords} instance.
     */
    WindowsInstallationRecords() {
    }

    /**
     * Returns the install locations of the uninstall entries with a name.
     *
     * @param entryName the entry name, such as {@code Example Game}
     * @return the recorded install folders, in the order they are looked up; empty when no entry has the name
     */
    @Override
    public @NotNull List<@NotNull Path> findUninstallLocations(@NotNull String entryName) {
        List<Path> locations = new ArrayList<>();

        for (String uninstallKey : UNINSTALL_KEYS) {
            String location = queryInstallLocation(uninstallKey + entryName);
            Path path = location == null ? null : parsePath(location);

            if (path != null && !locations.contains(path)) {
                locations.add(path);
            }
        }

        return locations;
    }

    /**
     * @return the folders programs are installed into by default, in the order they are looked at
     */
    @Override
    public @NotNull List<@NotNull Path> getProgramDirectories() {
        List<Path> directories = new ArrayList<>();

        for (String variable : PROGRAM_FOLDER_VARIABLES) {
            String value = System.getenv(variable);
            Path path = value == null ? null : parsePath(value);

            if (path != null && !directories.contains(path)) {
                directories.add(path);
            }
        }

        return directories;
    }
}
