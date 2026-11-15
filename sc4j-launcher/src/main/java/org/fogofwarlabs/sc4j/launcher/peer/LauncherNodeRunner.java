package org.fogofwarlabs.sc4j.launcher.peer;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs the launcher's node for the launcher application: it starts the node with the install's saved settings, restarts it when the user
 * changes them, and keeps why the node does not run (another launcher of the install holds it, or a broken key file). While another launcher
 * holds the node, it tries again every {@value #LOCK_RETRY_MILLIS} ms and takes the node once that launcher has closed. A node that cannot
 * start leaves the launcher running without it. Used from the user interface thread; the retries run on a daemon thread of their own.
 */
public final class LauncherNodeRunner implements Closeable {
    /**
     * The time between two tries to take the node while another launcher of the install holds it, in milliseconds.
     */
    public static final long LOCK_RETRY_MILLIS = 300_000L;

    private static final @NotNull Logger LOGGER = Logger.of(LauncherNodeRunner.class);
    private static final @NotNull String THREAD_NAME = "sc4j-launcher-node-retry";

    private final @NotNull InstallLayout install;
    private final @NotNull NodeSettingsFile settingsFile;
    private final @NotNull ScheduledExecutorService retries;

    private @NotNull NodeSettings settings;
    private @Nullable LauncherNode node;
    private boolean heldElsewhere;
    private @Nullable String problem;
    private boolean closed;

    /**
     * Creates a {@link LauncherNodeRunner} instance; the node starts with {@link #start()}.
     *
     * @param install the install
     */
    public LauncherNodeRunner(@NotNull InstallLayout install) {
        this.install      = install;
        this.settingsFile = NodeSettingsFile.of(install);
        this.retries      = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name(THREAD_NAME).daemon().factory());

        this.settings      = NodeSettings.DEFAULT;
        this.node          = null;
        this.heldElsewhere = false;
        this.problem       = null;
        this.closed        = false;
    }

    /**
     * @return the settings the node runs with, or was asked to run with
     */
    public synchronized @NotNull NodeSettings getSettings() {
        return this.settings;
    }

    /**
     * @return the running node's state, or {@code null} if the node does not run in this launcher
     */
    public synchronized @Nullable LauncherNodeStatus getStatus() {
        LauncherNode running = this.node;

        return running == null ? null : running.getStatus();
    }

    /**
     * @return {@code true} if another launcher of the install runs the node, {@code false} otherwise
     */
    public synchronized boolean isHeldElsewhere() {
        return this.heldElsewhere;
    }

    /**
     * @return why the node does not run or the settings were not saved, or {@code null}
     */
    public synchronized @Nullable String getProblem() {
        return this.problem;
    }


    /**
     * Starts the node with the saved settings and schedules the tries to take the node while another launcher holds it; a settings file that
     * cannot be read is logged and the default settings are used.
     */
    public synchronized void start() {
        try {
            this.settings = this.settingsFile.read();
        } catch (IOException exception) {
            LOGGER.warning("BEACON.LINK: {0}", exception.getMessage());
        }

        this.startNode();
        this.retries.scheduleWithFixedDelay(this::retry, LOCK_RETRY_MILLIS, LOCK_RETRY_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Saves other settings and restarts the node with them; while another launcher holds the node, they apply when this launcher takes it.
     *
     * @param changed the settings
     * @return why the settings could not be saved or the node could not start with them, or {@code null} otherwise
     */
    public synchronized @Nullable String changeSettings(@NotNull NodeSettings changed) {
        try {
            this.settingsFile.write(changed);
        } catch (IOException exception) {
            this.problem = String.valueOf(exception.getMessage());

            return this.problem;
        }

        this.settings = changed;

        if (this.heldElsewhere) {
            return null;
        }

        this.stopNode();
        this.startNode();

        return this.problem;
    }

    private synchronized void retry() {
        if (!this.closed && this.node == null && this.heldElsewhere) {
            this.startNode();
        }
    }

    private void startNode() {
        try {
            LauncherNode started = LauncherNode.start(this.install, this.settings);

            this.node          = started;
            this.heldElsewhere = started == null;
            this.problem       = null;
        } catch (IOException exception) {
            this.node          = null;
            this.heldElsewhere = false;
            this.problem       = String.valueOf(exception.getMessage());
            LOGGER.warning("BEACON.LINK: the launcher node cannot start: {0}", this.problem);
        }
    }

    private void stopNode() {
        LauncherNode running = this.node;

        this.node = null;

        if (running != null) {
            running.close();
        }
    }


    /**
     * Stops the tries to take the node and the node.
     */
    @Override
    public synchronized void close() {
        this.closed = true;
        this.retries.shutdownNow();
        this.stopNode();
    }
}
