package org.fogofwarlabs.sc4j.launcher.update;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.launcher.launch.RuntimeCommandLine;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs the updater step of a launcher update: {@code UpdaterMain --install <dir> --package <dir> --wait <process id>}.
 * <p>
 * The launcher starts it from the unpacked package of the new version with that package's Java runtime ({@link LauncherUpdater#startUpdater})
 * and exits. The updater waits for the launcher's process to end, replaces the programs of the install with those of the package
 * ({@link PackageSwap}) and starts the launcher of the install: the new one, or the old one when the replacement failed and was undone. On
 * Windows the start program {@code SC4J.exe} starts it, or {@code SC4J.bat} in an install without that program. The
 * reason of a failure is written to the update folder's failure file ({@link UpdateFolder#getFailureFile()}), which the launcher shows at
 * its next start; the steps are printed on the standard output, which the launcher writes to the update folder's log.
 */
public final class UpdaterMain {
    /**
     * The option naming the install folder.
     */
    public static final @NotNull String INSTALL_OPTION = "--install";

    /**
     * The option naming the unpacked package.
     */
    public static final @NotNull String PACKAGE_OPTION = "--package";

    /**
     * The option naming the process id of the launcher to wait for.
     */
    public static final @NotNull String WAIT_OPTION = "--wait";

    private static final @NotNull String USAGE = "Usage: UpdaterMain --install <dir> --package <dir> --wait <process id>";
    private static final long WAIT_SECONDS = 120;
    private static final int MOVE_ATTEMPTS = 20;
    private static final long RETRY_DELAY_MILLIS = 500;
    private static final @NotNull String WINDOWS_SHELL = "cmd.exe";
    private static final @NotNull String WINDOWS_SHELL_COMMAND_OPTION = "/c";
    private static final @NotNull String UNIX_SHELL = "/bin/sh";
    private static final int SUCCESS_STATUS = 0;
    private static final int FAILURE_STATUS = 1;
    private static final int USAGE_STATUS = 2;

    /**
     * Runs the updater step.
     *
     * @param arguments the command line
     */
    public static void main(@NotNull String @NotNull [] arguments) {
        Path installRoot = null;
        Path packageRoot = null;
        Long processId = null;

        try {
            for (int index = 0; index + 1 < arguments.length; index += 2) {
                switch (arguments[index]) {
                    case INSTALL_OPTION -> installRoot = Path.of(arguments[index + 1]);
                    case PACKAGE_OPTION -> packageRoot = Path.of(arguments[index + 1]);
                    case WAIT_OPTION -> processId = Long.parseLong(arguments[index + 1]);
                    default -> {
                        // An unknown option leaves a value missing below.
                    }
                }
            }
        } catch (InvalidPathException | NumberFormatException exception) {
            installRoot = null;
        }

        if (installRoot == null || packageRoot == null || processId == null || arguments.length != 6) {
            System.err.println(USAGE);
            System.exit(USAGE_STATUS);

            return;
        }

        System.exit(run(InstallLayout.of(installRoot), packageRoot, processId, RuntimeCommandLine.isWindows()));
    }

    private static int run(@NotNull InstallLayout install, @NotNull Path packageRoot, long processId, boolean windows) {
        UpdateFolder folder = UpdateFolder.of(install);
        Optional<ProcessHandle> launcher = ProcessHandle.of(processId);

        log("Waiting for the launcher process " + processId + " to end.");

        try {
            if (launcher.isPresent()) {
                launcher.get().onExit().get(WAIT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (TimeoutException | ExecutionException exception) {
            writeFailure(folder, Exceptions.format("The launcher did not close within {0} s.", WAIT_SECONDS));

            return FAILURE_STATUS;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            writeFailure(folder, "The updater was interrupted.");

            return FAILURE_STATUS;
        }

        int status = SUCCESS_STATUS;

        try {
            log("Replacing the programs of " + install.getRoot() + " with " + packageRoot + ".");
            FileTreeUtils.delete(folder.getBackupDirectory());
            new PackageSwap(install.getRoot(), packageRoot, folder.getBackupDirectory(), MOVE_ATTEMPTS, RETRY_DELAY_MILLIS).apply();
            log("The programs were replaced.");
            deleteBackup(folder);
        } catch (IOException exception) {
            writeFailure(folder, String.valueOf(exception.getMessage()));
            status = FAILURE_STATUS;
        }

        Path program = install.getRoot().resolve(PackageSwap.WINDOWS_PROGRAM);
        Path startFile = install.getRoot().resolve(windows ? PackageSwap.WINDOWS_START_FILE : PackageSwap.UNIX_START_FILE);
        List<String> command;

        if (windows && Files.isRegularFile(program)) {
            command = List.of(program.toString());
        } else if (windows) {
            command = List.of(WINDOWS_SHELL, WINDOWS_SHELL_COMMAND_OPTION, startFile.toString());
        } else {
            command = List.of(UNIX_SHELL, startFile.toString());
        }

        try {
            log("Starting " + command.getLast() + ".");
            new ProcessBuilder(command).directory(install.getRoot().toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                       .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException exception) {
            writeFailure(folder, Exceptions.format("The launcher cannot be started: {0}", exception.getMessage()));

            return FAILURE_STATUS;
        }

        return status;
    }

    /**
     * Deletes the replaced programs; a file that cannot be deleted is left for the launcher's cleanup.
     */
    private static void deleteBackup(@NotNull UpdateFolder folder) {
        try {
            FileTreeUtils.delete(folder.getBackupDirectory());
        } catch (IOException exception) {
            log("The replaced programs are removed later: " + exception.getMessage());
        }
    }

    private static void writeFailure(@NotNull UpdateFolder folder, @Nullable String reason) {
        String text = reason == null ? "The update failed." : reason;

        log("Failed: " + text);

        try {
            Files.writeString(folder.getFailureFile(), text + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log("The failure cannot be noted: " + exception.getMessage());
        }
    }

    private static void log(@NotNull String line) {
        System.out.println(Instant.now() + " " + line);
    }


    private UpdaterMain() {
    }
}
