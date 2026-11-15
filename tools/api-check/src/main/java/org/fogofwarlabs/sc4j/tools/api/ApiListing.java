package org.fogofwarlabs.sc4j.tools.api;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/**
 * Lists the public API of compiled classes as the JDK's {@code javap} prints it: every public or protected class whose enclosing classes are
 * public or protected too, with its public and protected constructors, methods and fields (constant values included), except the internal
 * elements.
 * <p>
 * The text form is one block per class, sorted by class name: the class declaration with {@code " {"}, one line per member indented by two
 * spaces, sorted, and {@code "}"}, with an empty line between blocks. Sorting makes the listing independent of the order of declarations in
 * the sources, so only a change of the API changes it.
 */
public final class ApiListing {
    private static final @NotNull String JAVAP = "javap";
    private static final @NotNull List<@NotNull String> JAVAP_OPTIONS = List.of("-protected", "-constants");
    private static final @NotNull String CLASS_PATH_OPTION = "-cp";
    private static final @NotNull String CLASS_FILE_SUFFIX = ".class";
    private static final @NotNull Set<@NotNull String> SKIPPED_CLASS_FILES = Set.of("module-info.class", "package-info.class");
    private static final @NotNull String COMPILED_FROM_PREFIX = "Compiled from ";
    private static final @NotNull List<@NotNull String> API_MODIFIERS = List.of("public ", "protected ");
    private static final @NotNull Set<@NotNull String> TYPE_KEYWORDS = Set.of("class", "interface");
    private static final @NotNull String BLOCK_START = " {";
    private static final @NotNull String BLOCK_END = "}";
    private static final @NotNull String MEMBER_INDENT = "  ";
    private static final @NotNull String REMOVED_PREFIX = "- ";
    private static final @NotNull String ADDED_PREFIX = "+ ";
    private static final @NotNull String LINE_SEPARATOR = "\n";
    private static final char PACKAGE_SEPARATOR = '.';
    private static final char NESTED_CLASS_SEPARATOR = '$';
    private static final char TYPE_PARAMETERS_START = '<';
    private static final int SUCCESS_EXIT = 0;

    /**
     * Holds one class of a listing: its declaration and its sorted member lines.
     */
    private static final class ApiClass {
        private final @NotNull String declaration;
        private final @NotNull List<@NotNull String> members;

        private ApiClass(@NotNull String declaration, @NotNull List<@NotNull String> members) {
            this.declaration = declaration;
            this.members     = members.stream().sorted().toList();
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         * <p>
         * The {@code equals} method implements an equivalence relation on non-null object references.
         *
         * @param other the reference object with which to compare
         * @return {@code true} if this object is the same as the other argument, {@code false} otherwise
         * @implSpec Two classes are equal when their declarations and their sorted member lines are equal.
         */
        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof ApiClass apiClass && this.declaration.equals(apiClass.declaration) && this.members.equals(apiClass.members);
        }

        /**
         * Returns a hash code value for the object.
         *
         * @return a hash code value for this object
         * @implSpec Combines the hash codes of the declaration and the member lines, consistent with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.declaration, this.members);
        }
    }

    /**
     * Creates an {@link ApiListing} instance from compiled classes with {@code javap}.
     *
     * @param classDirectories the folders of the compiled classes, as the class path of {@code javap}; a missing folder holds no class
     * @param internal         the internal elements, which the listing leaves out
     * @return the created {@link ApiListing} instance
     * @throws IOException           if a class folder cannot be read
     * @throws IllegalStateException if the JDK has no {@code javap} or {@code javap} fails
     */
    public static @NotNull ApiListing create(@NotNull List<@NotNull Path> classDirectories, @NotNull InternalElements internal) throws IOException {
        List<String> classNames = findClassNames(classDirectories);

        if (classNames.isEmpty()) {
            return new ApiListing(Collections.emptySortedMap());
        }

        ToolProvider javap = ToolProvider.findFirst(JAVAP)
                                         .orElseThrow(() -> Exceptions.newIllegalStateException("The JDK provides no {0} tool.", JAVAP));
        List<String> arguments = new ArrayList<>(JAVAP_OPTIONS);

        arguments.add(CLASS_PATH_OPTION);
        arguments.add(String.join(File.pathSeparator, classDirectories.stream().map(Path::toString).toList()));
        arguments.addAll(classNames);

        StringWriter output = new StringWriter();
        StringWriter errors = new StringWriter();
        int exitCode;

        try (PrintWriter outputWriter = new PrintWriter(output); PrintWriter errorWriter = new PrintWriter(errors)) {
            exitCode = javap.run(outputWriter, errorWriter, arguments.toArray(String[]::new));
        }

        if (exitCode != SUCCESS_EXIT) {
            throw Exceptions.newIllegalStateException("javap failed with exit code {0}: {1}", exitCode, errors.toString().strip());
        }

        SortedMap<String, ApiClass> classes = parseClasses(output.toString());
        SortedMap<String, ApiClass> listed = new TreeMap<>();

        for (Map.Entry<String, ApiClass> entry : classes.entrySet()) {
            String className = entry.getKey();

            if (isListed(className, classes) && !internal.isInternal(className)) {
                listed.put(className, entry.getValue());
            }
        }

        return new ApiListing(listed);
    }

    /**
     * Creates an {@link ApiListing} instance from its text form.
     *
     * @param text the text form
     * @return the created {@link ApiListing} instance
     * @throws IllegalArgumentException if the text is not a listing
     */
    public static @NotNull ApiListing parse(@NotNull String text) {
        return new ApiListing(parseClasses(text));
    }

    private static @NotNull List<@NotNull String> findClassNames(@NotNull List<@NotNull Path> classDirectories) throws IOException {
        Set<String> classNames = new TreeSet<>();

        for (Path directory : classDirectories) {
            if (!Files.isDirectory(directory)) {
                continue;
            }

            try (Stream<Path> files = Files.walk(directory)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String fileName = file.getFileName().toString();

                    if (!fileName.endsWith(CLASS_FILE_SUFFIX) || SKIPPED_CLASS_FILES.contains(fileName)) {
                        continue;
                    }

                    String relativePath = directory.relativize(file).toString();

                    classNames.add(relativePath.substring(0, relativePath.length() - CLASS_FILE_SUFFIX.length())
                                               .replace(File.separatorChar, PACKAGE_SEPARATOR));
                }
            }
        }

        return List.copyOf(classNames);
    }

    private static @NotNull SortedMap<@NotNull String, @NotNull ApiClass> parseClasses(@NotNull String text) {
        SortedMap<String, ApiClass> classes = new TreeMap<>();
        String declaration = null;
        List<String> members = new ArrayList<>();

        for (String line : text.split("\\R")) {
            if (line.isBlank() || line.startsWith(COMPILED_FROM_PREFIX)) {
                continue;
            }

            if (declaration == null) {
                if (!line.endsWith(BLOCK_START) || line.startsWith(MEMBER_INDENT)) {
                    throw Exceptions.newIllegalArgumentException("Expected a class declaration: {0}", line);
                }

                declaration = line.substring(0, line.length() - BLOCK_START.length());
                members.clear();
            } else if (line.equals(BLOCK_END)) {
                classes.put(findClassName(declaration), new ApiClass(declaration, members));
                declaration = null;
            } else if (line.startsWith(MEMBER_INDENT)) {
                members.add(line.strip());
            } else {
                throw Exceptions.newIllegalArgumentException("Expected a member or the end of {0}: {1}", declaration, line);
            }
        }

        if (declaration != null) {
            throw Exceptions.newIllegalArgumentException("The class has no end: {0}", declaration);
        }

        return classes;
    }

    private static @NotNull String findClassName(@NotNull String declaration) {
        String[] words = declaration.split(" ");

        for (int index = 0; index < words.length - 1; index++) {
            if (TYPE_KEYWORDS.contains(words[index])) {
                String name = words[index + 1];
                int typeParametersIndex = name.indexOf(TYPE_PARAMETERS_START);

                return typeParametersIndex < 0 ? name : name.substring(0, typeParametersIndex);
            }
        }

        throw Exceptions.newIllegalArgumentException("The class declaration names no class: {0}", declaration);
    }

    private static boolean isListed(@NotNull String className, @NotNull Map<@NotNull String, @NotNull ApiClass> classes) {
        ApiClass apiClass = classes.get(className);

        if (apiClass == null || API_MODIFIERS.stream().noneMatch(apiClass.declaration::startsWith)) {
            return false;
        }

        int nestedIndex = className.lastIndexOf(NESTED_CLASS_SEPARATOR);

        return nestedIndex < 0 || isListed(className.substring(0, nestedIndex), classes);
    }


    private final @NotNull SortedMap<@NotNull String, @NotNull ApiClass> classes;

    private ApiListing(@NotNull SortedMap<@NotNull String, @NotNull ApiClass> classes) {
        this.classes = Collections.unmodifiableSortedMap(new TreeMap<>(classes));
    }

    /**
     * @return the binary names of the listed classes, sorted
     */
    public @NotNull Set<@NotNull String> getClassNames() {
        return this.classes.keySet();
    }


    /**
     * Formats the listing as its text form.
     *
     * @return the text form, with line feeds and a line feed at the end when the listing is not empty
     */
    public @NotNull String format() {
        StringBuilder text = new StringBuilder();

        for (ApiClass apiClass : this.classes.values()) {
            if (!text.isEmpty()) {
                text.append(LINE_SEPARATOR);
            }

            text.append(apiClass.declaration).append(BLOCK_START).append(LINE_SEPARATOR);

            for (String member : apiClass.members) {
                text.append(MEMBER_INDENT).append(member).append(LINE_SEPARATOR);
            }

            text.append(BLOCK_END).append(LINE_SEPARATOR);
        }

        return text.toString();
    }

    /**
     * Describes how another listing differs from this one: per class that differs, its name, then its removed declaration or members
     * ({@code "- "}) and its added ones ({@code "+ "}).
     *
     * @param other the other listing, such as the one of the current build when this one is the checked-in file
     * @return the description lines, or an empty list if the listings are equal
     */
    public @NotNull List<@NotNull String> describeDifference(@NotNull ApiListing other) {
        Set<String> classNames = new TreeSet<>(this.classes.keySet());
        List<String> lines = new ArrayList<>();

        classNames.addAll(other.classes.keySet());

        for (String className : classNames) {
            ApiClass expected = this.classes.get(className);
            ApiClass actual = other.classes.get(className);

            if (expected != null && expected.equals(actual)) {
                continue;
            }

            lines.add(className);

            if (expected == null || actual == null || !expected.declaration.equals(actual.declaration)) {
                if (expected != null) {
                    lines.add(MEMBER_INDENT + REMOVED_PREFIX + expected.declaration);
                }

                if (actual != null) {
                    lines.add(MEMBER_INDENT + ADDED_PREFIX + actual.declaration);
                }
            }

            List<String> expectedMembers = expected == null ? List.of() : expected.members;
            List<String> actualMembers = actual == null ? List.of() : actual.members;

            expectedMembers.stream().filter(member -> !actualMembers.contains(member)).map(member -> MEMBER_INDENT + REMOVED_PREFIX + member)
                           .forEach(lines::add);
            actualMembers.stream().filter(member -> !expectedMembers.contains(member)).map(member -> MEMBER_INDENT + ADDED_PREFIX + member)
                         .forEach(lines::add);
        }

        return lines;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation on non-null object references.
     *
     * @param other the reference object with which to compare
     * @return {@code true} if this object is the same as the other argument, {@code false} otherwise
     * @implSpec Two listings are equal when they list the same classes with the same declarations and members.
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof ApiListing listing && this.classes.equals(listing.classes);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object
     * @implSpec Returns the hash code of the listed classes, consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return this.classes.hashCode();
    }
}
