package org.fogofwarlabs.sc4j.engine.client.console;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a long command reply into pages for the client's message area: a page is at most {@link #getPageLines} reply lines and, when more follow,
 * a footer naming {@code /more}.
 */
final class ConsoleReplyPager {
    /**
     * The reply of {@code /more} when the last reply has no further page.
     */
    static final @NotNull String NO_MORE = "No more lines.";
    private static final int LINE_FEED_CODE = 0x0A;
    /**
     * The separator of the lines of a reply.
     */
    static final @NotNull String LINE_FEED = String.valueOf((char) LINE_FEED_CODE);

    private final int pageLines;

    private final @NotNull List<String> lines;

    private int shownLineCount;

    /**
     * Creates a {@link ConsoleReplyPager} instance without a reply.
     *
     * @param pageLines the reply lines of one page before its footer
     * @throws IllegalArgumentException if a page holds no line
     */
    ConsoleReplyPager(int pageLines) {
        if (pageLines < 1) {
            throw Exceptions.newIllegalArgumentException("Invalid reply page line count: {0}", pageLines);
        }

        this.pageLines = pageLines;

        this.lines = new ArrayList<>();

        this.shownLineCount = 0;
    }

    /**
     * @return the reply lines of one page before its footer
     */
    int getPageLines() {
        return this.pageLines;
    }

    /**
     * Starts paging a reply.
     *
     * @param reply the reply, its lines separated by line feeds
     * @return the first page, empty for an empty reply
     */
    @NotNull List<String> startReply(@NotNull String reply) {
        this.lines.clear();
        this.shownLineCount = 0;

        if (reply.isEmpty()) {
            return List.of();
        }

        this.lines.addAll(List.of(reply.split(LINE_FEED)));

        return this.getNextPage();
    }

    /**
     * Returns the next page of the last reply and marks its lines as shown.
     *
     * @return the next page of the last reply, or a page holding only {@link #NO_MORE}
     */
    @NotNull List<String> getNextPage() {
        if (this.shownLineCount >= this.lines.size()) {
            return List.of(NO_MORE);
        }

        int end = Math.min(this.lines.size(), this.shownLineCount + this.pageLines);
        List<String> page = new ArrayList<>(this.lines.subList(this.shownLineCount, end));

        this.shownLineCount = end;

        if (end < this.lines.size()) {
            int pageNumber = (end - 1) / this.pageLines + 1;
            int pageCount = (this.lines.size() + this.pageLines - 1) / this.pageLines;

            page.add("-- page " + pageNumber + " of " + pageCount + ", /more shows the next --");
        }

        return page;
    }
}
