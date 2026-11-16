package org.fogofwarlabs.sc4j.launcher.release;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns the GitHub Markdown of release notes into plain text: headings, quotes, emphasis, code marks, links, images, HTML tags and comments,
 * table rules and horizontal rules lose their markup, list items start with {@code - }, and runs of blank lines become one blank line.
 */
public final class ReleaseNotesUtils {
    private static final @NotNull String LIST_MARK = "- ";
    private static final char ESCAPED_BASE = '\uE000';
    private static final int ASCII_SIZE = 128;
    private static final @NotNull Pattern LINE_BREAK_PATTERN = Pattern.compile("\\r\\n?");
    private static final @NotNull Pattern COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final @NotNull Pattern FENCE_PATTERN = Pattern.compile("\\s*(```|~~~).*");
    private static final @NotNull Pattern HEADING_PATTERN = Pattern.compile("^\\s{0,3}#{1,6}\\s+(.*?)(\\s+#+)?\\s*$");
    private static final @NotNull Pattern QUOTE_PATTERN = Pattern.compile("^(\\s{0,3}>\\s?)+");
    private static final @NotNull Pattern RULE_PATTERN = Pattern.compile("\\s{0,3}([-*_])(\\s*\\1){2,}\\s*");
    private static final @NotNull Pattern TABLE_RULE_PATTERN = Pattern.compile("\\s*\\|?(\\s*:?-+:?\\s*\\|)+(\\s*:?-+:?\\s*)?");
    private static final @NotNull Pattern LIST_ITEM_PATTERN = Pattern.compile("^(\\s*)[-*+]\\s+");
    private static final @NotNull Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\([^)]*\\)");
    private static final @NotNull Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\([^)]*\\)");
    private static final @NotNull Pattern AUTOLINK_PATTERN = Pattern.compile("<((?:https?|mailto):[^>\\s]+)>");
    private static final @NotNull Pattern HTML_TAG_PATTERN = Pattern.compile("</?[A-Za-z][A-Za-z0-9-]*(\\s[^<>]*)?/?>");
    private static final @NotNull Pattern CODE_PATTERN = Pattern.compile("`+([^`]+?)`+");
    private static final @NotNull Pattern STRONG_PATTERN = Pattern.compile("(\\*\\*|__)(?=\\S)(.+?)(?<=\\S)\\1");
    private static final @NotNull Pattern STAR_EMPHASIS_PATTERN = Pattern.compile("\\*(?=\\S)(.+?)(?<=\\S)\\*");
    private static final @NotNull Pattern UNDERSCORE_EMPHASIS_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}_])_(?=\\S)(.+?)(?<=\\S)_" +
                                                                                        "(?![\\p{L}\\p{N}_])");
    private static final @NotNull Pattern STRIKE_PATTERN = Pattern.compile("~~(?=\\S)(.+?)(?<=\\S)~~");
    private static final @NotNull Pattern ESCAPE_PATTERN = Pattern.compile("\\\\([\\\\`*_{}\\[\\]()#+\\-.!>|~])");
    private static final @NotNull Pattern ENTITY_PATTERN = Pattern.compile("&(amp|lt|gt|quot|nbsp);");

    /**
     * Creates the plain text of release notes. Text inside code fences keeps its lines without the fences.
     *
     * @param markdown the release notes in GitHub's Markdown
     * @return the plain text without leading or trailing blank lines, empty when the notes hold no text
     */
    public static @NotNull String toPlainText(@NotNull String markdown) {
        String text = COMMENT_PATTERN.matcher(LINE_BREAK_PATTERN.matcher(markdown).replaceAll("\n")).replaceAll("");
        List<String> lines = new ArrayList<>();
        boolean code = false;
        boolean blank = true;

        for (String line : text.split("\n", -1)) {
            String plain;

            if (FENCE_PATTERN.matcher(line).matches()) {
                code = !code;

                continue;
            }

            if (!code && TABLE_RULE_PATTERN.matcher(line).matches()) {
                continue;
            }

            plain = code ? line.stripTrailing() : toPlainLine(line);

            if (plain.isBlank()) {
                if (!blank) {
                    lines.add("");
                }

                blank = true;
            } else {
                lines.add(plain);
                blank = false;
            }
        }

        if (!lines.isEmpty() && lines.getLast().isEmpty()) {
            lines.removeLast();
        }

        return String.join("\n", lines);
    }

    private static @NotNull String toPlainLine(@NotNull String line) {
        // Escaped characters become private-use characters until the markup is gone, so that no pattern takes them as markup.
        String text = ESCAPE_PATTERN.matcher(line).replaceAll(match -> String.valueOf((char) (ESCAPED_BASE + match.group(1).charAt(0))));
        StringBuilder plain;

        text = QUOTE_PATTERN.matcher(text).replaceFirst("");

        if (RULE_PATTERN.matcher(text).matches()) {
            return "";
        }

        text  = HEADING_PATTERN.matcher(text).replaceFirst("$1");
        text  = LIST_ITEM_PATTERN.matcher(text).replaceFirst("$1" + LIST_MARK);
        text  = IMAGE_PATTERN.matcher(text).replaceAll("$1");
        text  = LINK_PATTERN.matcher(text).replaceAll("$1");
        text  = AUTOLINK_PATTERN.matcher(text).replaceAll("$1");
        text  = CODE_PATTERN.matcher(text).replaceAll("$1");
        text  = HTML_TAG_PATTERN.matcher(text).replaceAll("");
        text  = STRONG_PATTERN.matcher(text).replaceAll("$2");
        text  = STAR_EMPHASIS_PATTERN.matcher(text).replaceAll("$1");
        text  = UNDERSCORE_EMPHASIS_PATTERN.matcher(text).replaceAll("$1");
        text  = STRIKE_PATTERN.matcher(text).replaceAll("$1");
        text  = ENTITY_PATTERN.matcher(text).replaceAll(match -> switch (match.group(1)) {
            case "amp" -> "&";
            case "lt" -> "<";
            case "gt" -> ">";
            case "quot" -> "\"";
            default -> " ";
        });
        plain = new StringBuilder(text.length());

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            plain.append(character >= ESCAPED_BASE && character < ESCAPED_BASE + ASCII_SIZE ? (char) (character - ESCAPED_BASE) : character);
        }

        return plain.toString().stripTrailing();
    }


    private ReleaseNotesUtils() {
    }
}
