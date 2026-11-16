package org.fogofwarlabs.sc4j.launcher.release;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests turning the GitHub Markdown of release notes into the plain text the Update dialog shows.
 */
final class ReleaseNotesUtilsTest {
    @Test
    void to_plain_text_should_strip_the_markup_of_generated_release_notes() {
        String markdown = "## What's Changed\r\n" +
                          "* Fix the **update** check by @someone in https://github.com/owner/sc4j/pull/12\r\n" +
                          "- Add [the guide](https://example.org/guide) and ![logo](logo.png)\r\n" +
                          "\r\n" +
                          "\r\n" +
                          "**Full Changelog**: https://github.com/owner/sc4j/compare/v1.1.0...v1.2.0\r\n";

        Assertions.assertEquals("What's Changed\n" +
                                "- Fix the update check by @someone in https://github.com/owner/sc4j/pull/12\n" +
                                "- Add the guide and logo\n" +
                                "\n" +
                                "Full Changelog: https://github.com/owner/sc4j/compare/v1.1.0...v1.2.0", ReleaseNotesUtils.toPlainText(markdown));
    }

    @Test
    void to_plain_text_should_strip_emphasis_code_quotes_rules_tables_and_html() {
        String markdown = """
                          # Release 1.2.0 #
                          > Read *this* first, _please_.
                          ---
                          Use `--noSound` or ~~old~~ <kbd>F10</kbd> &amp; snake_case_name stays.
                          | Name | Size |
                          |------|-----:|
                          <!-- hidden
                          comment -->
                          ```
                          **kept as written**
                          ```
                          1. First \\*escaped\\* item
                          """;

        Assertions.assertEquals("Release 1.2.0\n" +
                                "Read this first, please.\n" +
                                "\n" +
                                "Use --noSound or old F10 & snake_case_name stays.\n" +
                                "| Name | Size |\n" +
                                "\n" +
                                "**kept as written**\n" +
                                "1. First *escaped* item", ReleaseNotesUtils.toPlainText(markdown));
    }

    @Test
    void to_plain_text_should_give_an_empty_text_for_empty_notes() {
        Assertions.assertEquals("", ReleaseNotesUtils.toPlainText(""));
        Assertions.assertEquals("", ReleaseNotesUtils.toPlainText("\n  \n<!-- nothing -->\n"));
    }
}
