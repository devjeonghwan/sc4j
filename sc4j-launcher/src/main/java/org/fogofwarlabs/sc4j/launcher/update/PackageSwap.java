package org.fogofwarlabs.sc4j.launcher.update;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the programs of an install with those of an unpacked user package: the items {@link #ITEMS}, that is {@code java/},
 * {@code launcher/}, {@code runtime/}, the start files, {@code LICENSE}, {@code NOTICE} and {@code licenses/}. {@code userdata/},
 * {@code mods/} and every other file of the install stay as they are.
 * <p>
 * Each item of the install is moved into the backup folder first, then the package's item is copied into its place; the package itself is
 * not changed, so the updater can run from it. When a step fails, the items replaced so far are removed and their backups moved back, so the
 * install keeps its old programs. A move is tried again a few times, as a program that has just ended may hold its files for a moment.
 */
public final class PackageSwap {
    /**
     * The Java runtime folder of the user package.
     */
    public static final @NotNull String JAVA_FOLDER = "java";

    /**
     * The start file of the launcher on Windows.
     */
    public static final @NotNull String WINDOWS_START_FILE = "SC4J.bat";

    /**
     * The start file of the launcher on other systems.
     */
    public static final @NotNull String UNIX_START_FILE = "SC4J";

    /**
     * The items of the install an update replaces, in the order they are replaced.
     */
    public static final @NotNull List<@NotNull String> ITEMS = List.of(JAVA_FOLDER, InstallLayout.LAUNCHER_FOLDER, InstallLayout.RUNTIME_FOLDER,
                                                                       WINDOWS_START_FILE, UNIX_START_FILE, "LICENSE", "NOTICE", "licenses");

    /**
     * Checks that an unpacked user package holds every item an update replaces.
     *
     * @param packageRoot the package's folder
     * @throws IOException if an item is missing
     */
    public static void checkPackage(@NotNull Path packageRoot) throws IOException {
        for (String item : ITEMS) {
            if (!Files.exists(packageRoot.resolve(item), LinkOption.NOFOLLOW_LINKS)) {
                throw Exceptions.newIOException("The package {0} has no {1}.", packageRoot, item);
            }
        }
    }


    private final @NotNull Path installRoot;
    private final @NotNull Path packageRoot;
    private final @NotNull Path backupRoot;
    private final int moveAttempts;
    private final long retryDelayMillis;

    /**
     * Creates a {@link PackageSwap} instance.
     *
     * @param installRoot      the install folder
     * @param packageRoot      the unpacked user package
     * @param backupRoot       the folder that keeps the replaced items until the swap has ended; it holds none of the items
     * @param moveAttempts     how often a move is tried, at least {@code 1}
     * @param retryDelayMillis the wait before a move is tried again, in milliseconds
     */
    public PackageSwap(@NotNull Path installRoot, @NotNull Path packageRoot, @NotNull Path backupRoot, int moveAttempts, long retryDelayMillis) {
        this.installRoot      = installRoot;
        this.packageRoot      = packageRoot;
        this.backupRoot       = backupRoot;
        this.moveAttempts     = Math.max(1, moveAttempts);
        this.retryDelayMillis = retryDelayMillis;
    }

    /**
     * Replaces the items of the install with those of the package. On a failure the old items are restored before the failure is thrown.
     *
     * @throws IOException if the package lacks an item or an item cannot be replaced; the install then has its old items again unless the
     *                     message says that they could not all be restored
     */
    public void apply() throws IOException {
        List<String> replaced = new ArrayList<>();

        checkPackage(this.packageRoot);
        Files.createDirectories(this.backupRoot);

        try {
            for (String item : ITEMS) {
                Path installed = this.installRoot.resolve(item);

                if (Files.exists(installed, LinkOption.NOFOLLOW_LINKS)) {
                    this.move(installed, this.backupRoot.resolve(item));
                }

                replaced.add(item);
                FileTreeUtils.copy(this.packageRoot.resolve(item), installed);
            }
        } catch (IOException exception) {
            try {
                this.restore(replaced);
            } catch (IOException restoreFailure) {
                exception.addSuppressed(restoreFailure);

                throw Exceptions.newIOException(exception, "The update failed and the old files were not all restored; they are in {0}.",
                                                this.backupRoot);
            }

            throw exception;
        }
    }

    private void restore(@NotNull List<@NotNull String> replaced) throws IOException {
        for (String item : replaced.reversed()) {
            Path installed = this.installRoot.resolve(item);
            Path backup = this.backupRoot.resolve(item);

            FileTreeUtils.delete(installed);

            if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) {
                this.move(backup, installed);
            }
        }
    }

    private void move(@NotNull Path source, @NotNull Path target) throws IOException {
        for (int attempt = 1; ; attempt++) {
            try {
                Files.move(source, target);

                return;
            } catch (IOException exception) {
                if (attempt >= this.moveAttempts) {
                    throw exception;
                }
            }

            try {
                Thread.sleep(this.retryDelayMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                throw Exceptions.newIOException(exception, "Interrupted while moving {0}.", source);
            }
        }
    }
}
