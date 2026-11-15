package org.fogofwarlabs.sc4j.launcher.launch;

import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.i18n.UiLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the report of how a run ended: the meaning of each exit code and the last output lines.
 */
final class RunReportTest {
    @Test
    void from_status_should_explain_the_runtime_exit_codes() {
        Assertions.assertEquals(RuntimeExitStatus.SUCCESS, RuntimeExitStatus.fromStatus(0));
        Assertions.assertEquals(RuntimeExitStatus.RUN_FAILURE, RuntimeExitStatus.fromStatus(1));
        Assertions.assertEquals(RuntimeExitStatus.CONFIGURATION_ERROR, RuntimeExitStatus.fromStatus(2));
        Assertions.assertEquals(RuntimeExitStatus.MOD_FAILURE, RuntimeExitStatus.fromStatus(3));
        Assertions.assertEquals(RuntimeExitStatus.UNKNOWN, RuntimeExitStatus.fromStatus(-1));
        Assertions.assertEquals(RuntimeExitStatus.UNKNOWN, RuntimeExitStatus.fromStatus(130));
    }

    @Test
    void format_should_report_a_failed_run_with_its_meaning_and_last_lines() {
        LogTail output = new LogTail(RuntimeRun.OUTPUT_CAPACITY);

        for (int line = 1; line <= RunReport.REPORT_LINE_COUNT + 3; line++) {
            output.add("line " + line);
        }

        RunReport report = RunReport.create(3, output);
        List<String> lines = report.getLastLines();
        String english = report.format(LauncherTexts.of(UiLanguage.ENGLISH));

        Assertions.assertTrue(report.isFailure());
        Assertions.assertEquals(RunReport.REPORT_LINE_COUNT, lines.size());
        Assertions.assertEquals("line 4", lines.getFirst());
        Assertions.assertEquals("line 15", lines.getLast());
        Assertions.assertTrue(english.startsWith("Runtime stopped, exit code 3\nMod not loaded or refused to run.\n"), english);
        Assertions.assertTrue(english.endsWith("Last output lines:\nline 4\nline 5\nline 6\nline 7\nline 8\nline 9\nline 10\nline 11\n" +
                                               "line 12\nline 13\nline 14\nline 15"),
                              english);
        Assertions.assertEquals("런타임 종료, 종료 코드 3", report.getHeadline(LauncherTexts.of(UiLanguage.KOREAN)));
    }

    @Test
    void format_should_explain_each_exit_code_in_every_language() {
        for (UiLanguage language : UiLanguage.values()) {
            LauncherTexts texts = LauncherTexts.of(language);

            for (int status : new int[]{0, 1, 2, 3, 137}) {
                String explanation = RunReport.create(status, new LogTail(1)).getExplanation(texts);

                Assertions.assertFalse(explanation.isBlank(), language + " " + status);
            }
        }

        Assertions.assertEquals("Runtime ended normally\nEnded normally.",
                                RunReport.create(0, new LogTail(1)).format(LauncherTexts.of(UiLanguage.ENGLISH)));
        Assertions.assertEquals("Invalid command line, path or saved settings.",
                                RunReport.create(2, new LogTail(1)).getExplanation(LauncherTexts.of(UiLanguage.ENGLISH)));
    }
}
