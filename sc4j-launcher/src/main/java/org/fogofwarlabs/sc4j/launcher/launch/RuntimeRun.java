package org.fogofwarlabs.sc4j.launcher.launch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * Records one run of the runtime from the launcher: which mod with which command line, when it started, its output and,
 * once it ended, its {@link RunReport}. A run that could not be started records the reason instead.
 * <p>
 * Instances are confined to the user interface thread; the process's output thread hands its events over first.
 */
public final class RuntimeRun {
    /**
     * The number of output lines a run keeps.
     */
    public static final int OUTPUT_CAPACITY = 20000;

    private final @NotNull String modName;
    private final @NotNull String modId;
    private final @NotNull String modVersion;
    private final @NotNull RuntimeCommandLine commandLine;
    private final @NotNull String settingsText;
    private final @NotNull Instant startTime;
    private final @NotNull LogTail output;

    private @Nullable RuntimeProcess process;
    private @Nullable Instant endTime;
    private @Nullable RunReport report;
    private @Nullable String startFailure;

    /**
     * Creates a {@link RuntimeRun} instance for a run that is about to start.
     *
     * @param modName      the mod's display name
     * @param modId        the mod id
     * @param modVersion   the mod version
     * @param commandLine  the command line of the run
     * @param settingsText the contents of the settings value file the run reads, for the user's reference
     * @param startTime    when the run started
     */
    public RuntimeRun(@NotNull String modName, @NotNull String modId, @NotNull String modVersion, @NotNull RuntimeCommandLine commandLine,
                      @NotNull String settingsText, @NotNull Instant startTime) {
        this.modName      = modName;
        this.modId        = modId;
        this.modVersion   = modVersion;
        this.commandLine  = commandLine;
        this.settingsText = settingsText;
        this.startTime    = startTime;
        this.output       = new LogTail(OUTPUT_CAPACITY);

        this.process      = null;
        this.endTime      = null;
        this.report       = null;
        this.startFailure = null;
    }

    /**
     * @return the mod's display name
     */
    public @NotNull String getModName() {
        return this.modName;
    }

    /**
     * @return the mod id
     */
    public @NotNull String getModId() {
        return this.modId;
    }

    /**
     * @return the mod version
     */
    public @NotNull String getModVersion() {
        return this.modVersion;
    }

    /**
     * @return the command line of the run
     */
    public @NotNull RuntimeCommandLine getCommandLine() {
        return this.commandLine;
    }

    /**
     * @return the contents of the settings value file the run reads
     */
    public @NotNull String getSettingsText() {
        return this.settingsText;
    }

    /**
     * @return when the run started
     */
    public @NotNull Instant getStartTime() {
        return this.startTime;
    }

    /**
     * @return the output of the run so far
     */
    public @NotNull LogTail getOutput() {
        return this.output;
    }

    /**
     * @return the runtime process, or {@code null} when it has not started or could not be started
     */
    public @Nullable RuntimeProcess getProcess() {
        return this.process;
    }

    /**
     * Sets the started runtime process.
     *
     * @param process the process
     */
    public void setProcess(@NotNull RuntimeProcess process) {
        this.process = process;
    }

    /**
     * @return the report of the ended run, or {@code null} while it runs or when it could not be started
     */
    public @Nullable RunReport getReport() {
        return this.report;
    }

    /**
     * @return why the runtime could not be started, or {@code null} when it started
     */
    public @Nullable String getStartFailure() {
        return this.startFailure;
    }

    /**
     * @return {@code true} if the runtime process was started and has not ended yet, {@code false} otherwise
     */
    public boolean isRunning() {
        return this.process != null && this.endTime == null;
    }

    /**
     * Returns how long the run took, or has taken so far.
     *
     * @param now the current time, used while the run lasts
     * @return the run's duration
     */
    public @NotNull Duration getDuration(@NotNull Instant now) {
        return Duration.between(this.startTime, this.endTime != null ? this.endTime : now);
    }

    /**
     * Adds a line of the runtime's output.
     *
     * @param line the line without its line terminator
     */
    public void addOutput(@NotNull String line) {
        this.output.add(line);
    }

    /**
     * Records the end of the run and creates its report ({@link #getReport()}).
     *
     * @param status  the process exit status
     * @param endTime when the process ended
     */
    public void finish(int status, @NotNull Instant endTime) {
        this.endTime = endTime;
        this.report  = RunReport.create(status, this.output);
    }

    /**
     * Records that the runtime could not be started.
     *
     * @param reason  why the process could not be started
     * @param endTime when the start failed
     */
    public void failStart(@NotNull String reason, @NotNull Instant endTime) {
        this.startFailure = reason;
        this.endTime      = endTime;
    }
}
