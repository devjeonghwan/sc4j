package org.fogofwarlabs.sc4j.manifest.validation;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.manifest.text.LocalizedText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the problems of one validation, in the order they were found. A result without problems is valid.
 */
public final class ValidationResult {
    /**
     * The result without problems.
     */
    public static final @NotNull ValidationResult VALID = new ValidationResult(List.of());

    private static final @NotNull String LINE_SEPARATOR = "\n";

    /**
     * Builds {@link ValidationResult} instances.
     */
    public static final class Builder {
        private final @NotNull List<@NotNull ValidationProblem> problems;

        /**
         * Creates a {@link Builder} instance without problems.
         */
        public Builder() {
            this.problems = new ArrayList<>();
        }

        /**
         * @return {@code true} if no problem was added, {@code false} otherwise
         */
        public boolean isValid() {
            return this.problems.isEmpty();
        }

        /**
         * Adds a problem.
         *
         * @param problem the problem
         * @return this instance for chaining
         */
        public @NotNull Builder add(@NotNull ValidationProblem problem) {
            this.problems.add(problem);

            return this;
        }

        /**
         * Adds a problem with a formatted generic message and no localized message.
         *
         * @param code      the kind of the problem
         * @param subject   where the problem was found
         * @param text      the generic message format with {@code {index}} placeholders
         * @param arguments the placeholder arguments
         * @return this instance for chaining
         */
        public @NotNull Builder add(@NotNull ProblemCode code, @NotNull String subject,
                                    @NotNull String text, @Nullable Object @NotNull ... arguments) {
            return this.add(new ValidationProblem(code, subject, Exceptions.format(text, arguments), null));
        }

        /**
         * Adds the problem of a failed declarative setting check, with the setting's localized message.
         *
         * @param code      the kind of the problem
         * @param subject   the setting key
         * @param message   the setting's localized message, or {@code null} when it has none
         * @param text      the generic message format with {@code {index}} placeholders
         * @param arguments the placeholder arguments
         * @return this instance for chaining
         */
        public @NotNull Builder addCheckFailure(@NotNull ProblemCode code, @NotNull String subject, @Nullable LocalizedText message,
                                                @NotNull String text, @Nullable Object @NotNull ... arguments) {
            return this.add(new ValidationProblem(code, subject, Exceptions.format(text, arguments), message));
        }

        /**
         * Adds every problem of a result.
         *
         * @param result the result
         * @return this instance for chaining
         */
        public @NotNull Builder addAll(@NotNull ValidationResult result) {
            this.problems.addAll(result.problems);

            return this;
        }

        /**
         * Builds a {@link ValidationResult} instance with the problems added so far.
         *
         * @return the built {@link ValidationResult} instance
         */
        public @NotNull ValidationResult build() {
            return this.problems.isEmpty() ? VALID : new ValidationResult(List.copyOf(this.problems));
        }
    }


    private final @NotNull List<@NotNull ValidationProblem> problems;

    /**
     * Creates a {@link ValidationResult} instance.
     *
     * @param problems the unmodifiable problems
     */
    private ValidationResult(@NotNull List<@NotNull ValidationProblem> problems) {
        this.problems = problems;
    }

    /**
     * @return the problems in the order they were found, empty for a valid result
     */
    public @NotNull List<@NotNull ValidationProblem> getProblems() {
        return this.problems;
    }

    /**
     * @return {@code true} if there is no problem, {@code false} otherwise
     */
    public boolean isValid() {
        return this.problems.isEmpty();
    }

    /**
     * Checks whether a problem of a kind was found.
     *
     * @param code the kind of problem
     * @return {@code true} if a problem has the code, {@code false} otherwise
     */
    public boolean hasProblem(@NotNull ProblemCode code) {
        for (ValidationProblem problem : this.problems) {
            if (problem.getCode() == code) {
                return true;
            }
        }

        return false;
    }

    /**
     * Describes every problem, one per line ({@link ValidationProblem#describe()}).
     *
     * @return the lines joined by line feeds, empty for a valid result
     */
    public @NotNull String describe() {
        List<String> lines = new ArrayList<>(this.problems.size());

        for (ValidationProblem problem : this.problems) {
            lines.add(problem.describe());
        }

        return String.join(LINE_SEPARATOR, lines);
    }
}
