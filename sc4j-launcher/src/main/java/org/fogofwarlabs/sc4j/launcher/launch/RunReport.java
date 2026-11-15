package org.fogofwarlabs.sc4j.launcher.launch;

import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports how a run of the runtime ended: the exit status, what it means and, for a failed run, the last lines of the
 * output. The launcher shows the report when the runtime ends with a status other than 0.
 */
public final class RunReport {
    /**
     * The number of output lines a report shows.
     */
    public static final int REPORT_LINE_COUNT = 12;

    private static final @NotNull String SUCCESS_HEADLINE_KEY = "report.headline.success";
    private static final @NotNull String FAILURE_HEADLINE_KEY = "report.headline.failure";
    private static final @NotNull String LINES_HEADING_KEY = "report.lines";
    private static final @NotNull String LINE_SEPARATOR = "\n";

    /**
     * Creates a {@link RunReport} instance from an exit status and the run's output.
     *
     * @param status the process exit status
     * @param output the output of the run
     * @return the created {@link RunReport} instance with the last {@value #REPORT_LINE_COUNT} output lines
     */
    public static @NotNull RunReport create(int status, @NotNull LogTail output) {
        return new RunReport(status, output.getLastLines(REPORT_LINE_COUNT));
    }


    private final int status;
    private final @NotNull RuntimeExitStatus exitStatus;
    private final @NotNull List<@NotNull String> lastLines;

    /**
     * Creates a {@link RunReport} instance.
     *
     * @param status    the process exit status
     * @param lastLines the last output lines, oldest first
     */
    private RunReport(int status, @NotNull List<@NotNull String> lastLines) {
        this.status     = status;
        this.exitStatus = RuntimeExitStatus.fromStatus(status);
        this.lastLines  = List.copyOf(lastLines);
    }

    /**
     * @return the process exit status
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * @return the last output lines, oldest first
     */
    public @NotNull List<@NotNull String> getLastLines() {
        return this.lastLines;
    }

    /**
     * @return {@code true} if the runtime did not end normally (status other than 0), {@code false} otherwise
     */
    public boolean isFailure() {
        return this.exitStatus != RuntimeExitStatus.SUCCESS;
    }

    /**
     * Returns the report's headline, such as "The runtime stopped with exit code 3".
     *
     * @param texts the launcher texts of the display language
     * @return the localized headline
     */
    public @NotNull String getHeadline(@NotNull LauncherTexts texts) {
        return this.isFailure() ? texts.format(FAILURE_HEADLINE_KEY, this.status) : texts.get(SUCCESS_HEADLINE_KEY);
    }

    /**
     * Returns what the exit status means.
     *
     * @param texts the launcher texts of the display language
     * @return the localized explanation
     */
    public @NotNull String getExplanation(@NotNull LauncherTexts texts) {
        return texts.format(this.exitStatus.getTextKey(), this.status);
    }

    /**
     * Writes the whole report as text: the headline, the explanation and, for a failed run, the last output lines.
     *
     * @param texts the launcher texts of the display language
     * @return the report text, lines separated by line feeds
     */
    public @NotNull String format(@NotNull LauncherTexts texts) {
        List<String> lines = new ArrayList<>();

        lines.add(this.getHeadline(texts));
        lines.add(this.getExplanation(texts));

        if (this.isFailure() && !this.lastLines.isEmpty()) {
            lines.add("");
            lines.add(texts.get(LINES_HEADING_KEY));
            lines.addAll(this.lastLines);
        }

        return String.join(LINE_SEPARATOR, lines);
    }
}
