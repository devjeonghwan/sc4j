package org.fogofwarlabs.sc4j.engine.client.console;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Declares one console command of the client: its syntax, one-line summary, the argument forms with their handlers, where it works and examples.
 * The command dispatches on the declared forms (a form with no arguments, sub-commands keyed by their first word, and a fallback for any other first
 * word), and {@code /help <command>} prints the detailed help generated from the same declaration, so the dispatch and its documentation cannot
 * diverge.
 */
public final class ConsoleCommand {
    /**
     * The column the help text wraps at.
     */
    public static final int HELP_WIDTH = 72;
    /**
     * The argument word that switches a client feature on.
     */
    public static final @NotNull String ON = "on";
    /**
     * The argument word that switches a client feature off.
     */
    public static final @NotNull String OFF = "off";

    private static final @NotNull String ENTRY_INDENT = "  ";
    private static final @NotNull String CONTINUATION_INDENT = "      ";
    private static final @NotNull String ENTRY_SEPARATOR = " - ";
    private static final @NotNull String WORD_SEPARATOR = "|";

    /**
     * Runs one form of a command.
     */
    public interface Handler {
        /**
         * Runs the command.
         *
         * @param arguments the words after the command name, or after the sub-command word
         * @return the message describing the effect or the error
         */
        @NotNull String run(@NotNull String @NotNull [] arguments);
    }

    /**
     * One documented term: an argument form or an item of a help section.
     */
    private static final class Entry {
        private final @NotNull String term;
        private final @NotNull String meaning;

        private Entry(@NotNull String term, @NotNull String meaning) {
            this.term    = term;
            this.meaning = meaning;
        }
    }

    /**
     * One titled list of entries after the forms, for example the views a form accepts.
     */
    private static final class Section {
        private final @NotNull String title;
        private final @NotNull List<Entry> entries;

        private Section(@NotNull String title) {
            this.title   = title;
            this.entries = new ArrayList<>();
        }
    }

    /**
     * A condition the command needs, with the reply when it does not hold.
     */
    private static final class Requirement {
        private final @NotNull BooleanSupplier available;
        private final @NotNull String refusal;

        private Requirement(@NotNull BooleanSupplier available, @NotNull String refusal) {
            this.available = available;
            this.refusal   = refusal;
        }
    }

    /**
     * Creates a {@link ConsoleCommand} instance.
     *
     * @param name    the name with its {@code /}, for example {@code /zoom}
     * @param syntax  the argument syntax of the command list, for example {@code [on|off|reset|<percent>]}, or empty
     * @param summary the one-line summary of the command list, starting in lower case
     * @return the created {@link ConsoleCommand} instance
     * @throws IllegalArgumentException if the name does not start with {@code /}
     */
    public static @NotNull ConsoleCommand of(@NotNull String name, @NotNull String syntax, @NotNull String summary) {
        if (!ConsoleCommandRegistry.isCommand(name)) {
            throw Exceptions.newIllegalArgumentException("A command name starts with {0}: {1}", ConsoleCommandRegistry.PREFIX, name);
        }

        return new ConsoleCommand(name.toLowerCase(Locale.ROOT), syntax, summary);
    }

    /**
     * Reads a switch argument: {@link #ON} or {@link #OFF}, in any case.
     *
     * @param word the argument
     * @return {@link Boolean#TRUE} for {@link #ON}, {@link Boolean#FALSE} for {@link #OFF}, or {@code null} for any other word
     */
    public static @Nullable Boolean parseSwitch(@NotNull String word) {
        if (word.equalsIgnoreCase(ON)) {
            return Boolean.TRUE;
        }

        if (word.equalsIgnoreCase(OFF)) {
            return Boolean.FALSE;
        }

        return null;
    }

    /**
     * Appends a summary line wrapped at {@link #HELP_WIDTH}, its continuation lines indented.
     *
     * @param text  the line
     * @param lines the lines to append to
     */
    static void wrapSummary(@NotNull String text, @NotNull List<String> lines) {
        wrap(text, "", CONTINUATION_INDENT, lines);
    }

    /**
     * Appends a text wrapped at {@link #HELP_WIDTH}, the first line after {@code firstIndent}, the others after {@code nextIndent}.
     *
     * @param text        the text
     * @param firstIndent the indent of the first line
     * @param nextIndent  the indent of the continuation lines
     * @param lines       the lines to append to
     */
    private static void wrap(@NotNull String text, @NotNull String firstIndent, @NotNull String nextIndent, @NotNull List<String> lines) {
        StringBuilder line = new StringBuilder(firstIndent);
        boolean empty = true;

        for (String word : text.split(" ")) {
            if (!empty && line.length() + 1 + word.length() > HELP_WIDTH) {
                lines.add(line.toString());
                line  = new StringBuilder(nextIndent);
                empty = true;
            }

            if (!empty) {
                line.append(' ');
            }

            line.append(word);
            empty = false;
        }

        lines.add(line.toString());
    }

    private final @NotNull String name;
    private final @NotNull String syntax;
    private final @NotNull String summary;

    private final @NotNull List<String> description;
    private final @NotNull List<Entry> forms;
    private final @NotNull List<Section> sections;
    private final @NotNull List<String> availability;
    private final @NotNull List<String> examples;

    private final @NotNull List<Requirement> requirements;
    private final @NotNull Map<String, Handler> subcommands;
    private @Nullable Handler emptyHandler;
    private @Nullable Handler otherHandler;

    /**
     * Creates a {@link ConsoleCommand} instance without forms, help sections or requirements.
     *
     * @param name    the lower-case name with its {@code /}
     * @param syntax  the argument syntax of the command list, or empty
     * @param summary the one-line summary of the command list
     */
    private ConsoleCommand(@NotNull String name, @NotNull String syntax, @NotNull String summary) {
        this.name    = name;
        this.syntax  = syntax;
        this.summary = summary;

        this.description  = new ArrayList<>();
        this.forms        = new ArrayList<>();
        this.sections     = new ArrayList<>();
        this.availability = new ArrayList<>();
        this.examples     = new ArrayList<>();

        this.requirements = new ArrayList<>();
        this.subcommands  = new LinkedHashMap<>();
        this.emptyHandler = null;
        this.otherHandler = null;
    }

    /**
     * @return the name with its {@code /}
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the argument syntax of the command list, or an empty string
     */
    public @NotNull String getSyntax() {
        return this.syntax;
    }

    /**
     * @return the one-line summary
     */
    public @NotNull String getSummary() {
        return this.summary;
    }

    /**
     * @return the command list line, {@code "/name syntax - summary"}
     */
    public @NotNull String getSummaryLine() {
        return this.getNameWithSyntax() + ENTRY_SEPARATOR + this.summary;
    }

    /**
     * @return the reply to a malformed use: the syntax and where the detailed help is
     */
    public @NotNull String getUsageReply() {
        return "Use " + this.getNameWithSyntax() + " (/help " + this.name.substring(ConsoleCommandRegistry.PREFIX.length()) + ").";
    }

    /**
     * Returns the detailed help of {@code /help <command>}: the summary line, the description, every argument form with its meaning, the sections,
     * where the command works and the examples, wrapped at {@link #HELP_WIDTH}.
     *
     * @return the help lines
     */
    public @NotNull List<String> getHelpLines() {
        List<String> lines = new ArrayList<>();

        wrapSummary(this.getSummaryLine(), lines);

        for (String paragraph : this.description) {
            wrap(paragraph, ENTRY_INDENT, ENTRY_INDENT, lines);
        }

        if (!this.forms.isEmpty()) {
            lines.add("Forms:");
        }

        for (Entry form : this.forms) {
            wrap(form.term + ENTRY_SEPARATOR + form.meaning, ENTRY_INDENT, CONTINUATION_INDENT, lines);
        }

        for (Section section : this.sections) {
            lines.add(section.title);

            for (Entry entry : section.entries) {
                wrap(entry.term + ENTRY_SEPARATOR + entry.meaning, ENTRY_INDENT, CONTINUATION_INDENT, lines);
            }
        }

        for (String text : this.availability) {
            wrap("Where: " + text, "", CONTINUATION_INDENT, lines);
        }

        for (String example : this.examples) {
            lines.add("Example: " + example);
        }

        return lines;
    }

    /**
     * @return the name followed by the argument syntax, or the name alone without a syntax
     */
    private @NotNull String getNameWithSyntax() {
        return this.syntax.isEmpty() ? this.name : this.name + " " + this.syntax;
    }


    /**
     * Adds a paragraph to the description.
     *
     * @param text the paragraph
     * @return this instance for chaining
     */
    public @NotNull ConsoleCommand describe(@NotNull String text) {
        this.description.add(text);

        return this;
    }

    /**
     * Declares the form without arguments.
     *
     * @param meaning what the form does
     * @param handler the handler, called with no arguments
     * @return this instance for chaining
     */
    public @NotNull ConsoleCommand whenEmpty(@NotNull String meaning, @NotNull Handler handler) {
        this.forms.add(new Entry(this.name, meaning));
        this.emptyHandler = handler;

        return this;
    }

    /**
     * Declares a sub-command keyed by its first word.
     *
     * @param words     the first word, or several equivalent words separated by {@code |}, for example {@code lighting|lights}
     * @param arguments the syntax of the arguments after the word, or an empty string
     * @param meaning   what the form does
     * @param handler   the handler, called with the arguments after the word
     * @return this instance for chaining
     */
    public @NotNull ConsoleCommand subcommand(@NotNull String words, @NotNull String arguments, @NotNull String meaning, @NotNull Handler handler) {
        this.forms.add(new Entry(this.name + " " + (arguments.isEmpty() ? words : words + " " + arguments), meaning));

        for (String word : words.split("\\" + WORD_SEPARATOR)) {
            this.subcommands.put(word.toLowerCase(Locale.ROOT), handler);
        }

        return this;
    }

    /**
     * Declares the form of any other first argument, for example a number.
     *
     * @param arguments the syntax of the form, for example {@code <percent>}
     * @param meaning   what the form does
     * @param handler   the handler, called with every argument
     * @return this instance for chaining
     */
    public @NotNull ConsoleCommand otherwise(@NotNull String arguments, @NotNull String meaning, @NotNull Handler handler) {
        this.forms.add(new Entry(this.name + " " + arguments, meaning));
        this.otherHandler = handler;

        return this;
    }

    /**
     * Starts a titled help section after the forms.
     *
     * @param title the title, for example {@code "Lighting views:"}
     * @return this instance for chaining
     */
    public @NotNull ConsoleCommand section(@NotNull String title) {
        this.sections.add(new Section(title));

        return this;
    }

    /**
     * Adds an entry to the last section.
     *
     * @param term    the documented term
     * @param meaning its meaning
     * @return this instance for chaining
     * @throws IllegalStateException if no section was started
     */
    public @NotNull ConsoleCommand item(@NotNull String term, @NotNull String meaning) {
        if (this.sections.isEmpty()) {
            throw Exceptions.newIllegalStateException("No help section started for {0}", this.name);
        }

        this.sections.getLast().entries.add(new Entry(term, meaning));

        return this;
    }

    /**
     * Declares a condition the command needs; while it does not hold, the command replies with the refusal and does nothing.
     *
     * @param available whether the command may run
     * @param refusal   the reply otherwise
     * @return this instance for chaining
     */
    public @NotNull ConsoleCommand requires(@NotNull BooleanSupplier available, @NotNull String refusal) {
        this.requirements.add(new Requirement(available, refusal));

        return this;
    }

    /**
     * Describes where the command works, for example in which kinds of games.
     *
     * @param text the description
     * @return this instance for chaining
     */
    public @NotNull ConsoleCommand availability(@NotNull String text) {
        this.availability.add(text);

        return this;
    }

    /**
     * Adds an example line.
     *
     * @param line the typed line
     * @return this instance for chaining
     */
    public @NotNull ConsoleCommand example(@NotNull String line) {
        this.examples.add(line);

        return this;
    }


    /**
     * Runs the command: a failed requirement replies with its refusal; otherwise the form without arguments, the sub-command of the first word or the
     * fallback runs, and an argument no form takes replies with the usage.
     *
     * @param arguments the words after the command name
     * @return the message describing the effect or the error
     */
    public @NotNull String run(@NotNull String @NotNull [] arguments) {
        for (Requirement requirement : this.requirements) {
            if (!requirement.available.getAsBoolean()) {
                return requirement.refusal;
            }
        }

        if (arguments.length == 0) {
            return this.emptyHandler != null ? this.emptyHandler.run(arguments) : this.getUsageReply();
        }

        Handler subcommand = this.subcommands.get(arguments[0].toLowerCase(Locale.ROOT));

        if (subcommand != null) {
            return subcommand.run(Arrays.copyOfRange(arguments, 1, arguments.length));
        }

        if (this.otherHandler != null) {
            return this.otherHandler.run(arguments);
        }

        return "Unknown argument: " + arguments[0] + ". " + this.getUsageReply();
    }
}
