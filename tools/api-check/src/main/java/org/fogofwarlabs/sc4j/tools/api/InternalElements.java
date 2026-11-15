package org.fogofwarlabs.sc4j.tools.api;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the internal elements of the engine: packages (with their subpackages) and top-level classes that are public for the engine's own
 * modules but are not API, so the API listing leaves them out and mods must not use them.
 * <p>
 * An element is a package when the last segment of its name starts with a lower case letter (as {@code org.example.loading}) and a class
 * when it starts with an upper case letter (as {@code org.example.host.GlfwInput}).
 */
public final class InternalElements {
    private static final @NotNull Pattern ELEMENT_PATTERN = Pattern.compile("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*(\\.[A-Z][A-Za-z0-9_]*)?");
    private static final @NotNull String PACKAGE_REFERENCE_SUFFIX = "\\.((?:[a-z][a-z0-9_]*\\.)*(?:[A-Z][A-Za-z0-9_]*|\\*))";
    private static final @NotNull String IMPORT_ON_DEMAND_FORMAT = "\\bimport\\s+%s\\s*\\.\\s*\\*\\s*;";
    private static final @NotNull String WORD_BOUNDARY = "\\b";
    private static final char PACKAGE_SEPARATOR = '.';
    private static final char NESTED_CLASS_SEPARATOR = '$';

    private static boolean isClassName(@NotNull String name) {
        if (!ELEMENT_PATTERN.matcher(name).matches() || name.indexOf(PACKAGE_SEPARATOR) < 0) {
            throw Exceptions.newIllegalArgumentException("Not a qualified package or class name: {0}", name);
        }

        return Character.isUpperCase(name.charAt(name.lastIndexOf(PACKAGE_SEPARATOR) + 1));
    }

    private static @NotNull String getPackageName(@NotNull String name) {
        return isClassName(name) ? name.substring(0, name.lastIndexOf(PACKAGE_SEPARATOR)) : name;
    }

    private static @NotNull String getSimpleName(@NotNull String name) {
        return name.substring(name.lastIndexOf(PACKAGE_SEPARATOR) + 1);
    }


    private final @NotNull List<@NotNull String> names;
    private final @NotNull List<@NotNull Pattern> referencePatterns;
    private final @NotNull List<@NotNull Pattern> importOnDemandPatterns;
    private final @NotNull List<@NotNull Pattern> simpleNamePatterns;

    /**
     * Creates an {@link InternalElements} instance.
     *
     * @param names the qualified names of the internal packages and classes
     * @throws IllegalArgumentException if a name is not a qualified package or class name
     */
    public InternalElements(@NotNull List<@NotNull String> names) {
        this.names                  = List.copyOf(new TreeSet<>(names));
        this.referencePatterns      = this.names.stream()
                                                .map(name -> Pattern.compile(WORD_BOUNDARY + Pattern.quote(name) +
                                                                             (isClassName(name) ? WORD_BOUNDARY : PACKAGE_REFERENCE_SUFFIX)))
                                                .toList();
        this.importOnDemandPatterns = this.names.stream()
                                                .map(name -> Pattern.compile(String.format(IMPORT_ON_DEMAND_FORMAT, Pattern.quote(getPackageName(name)))))
                                                .toList();
        this.simpleNamePatterns     = this.names.stream()
                                                .map(name -> Pattern.compile(WORD_BOUNDARY + Pattern.quote(getSimpleName(name)) + WORD_BOUNDARY))
                                                .toList();
    }

    /**
     * Checks whether a compiled class is internal: its top-level class is an internal class, or its package is an internal package or lies
     * inside one.
     *
     * @param className the binary class name with dots between the package segments, as {@code org.example.Outer$Nested}
     * @return {@code true} if the class is internal, {@code false} otherwise
     */
    public boolean isInternal(@NotNull String className) {
        int nestedIndex = className.indexOf(NESTED_CLASS_SEPARATOR);
        String topLevelName = nestedIndex < 0 ? className : className.substring(0, nestedIndex);
        int packageEnd = topLevelName.lastIndexOf(PACKAGE_SEPARATOR);
        String packageName = packageEnd < 0 ? "" : topLevelName.substring(0, packageEnd);

        for (String name : this.names) {
            if (topLevelName.equals(name) || packageName.equals(name) || packageName.startsWith(name + PACKAGE_SEPARATOR)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the internal elements that Java code names: a qualified name of an internal class, a class or on-demand import of an internal
     * package, and the simple name of an internal class whose package the code imports on demand. The code must have its comments and
     * literals removed.
     *
     * @param code the Java code without comments and literals
     * @return the named elements, sorted: an internal class by its name, a class of an internal package by its qualified name and an on-demand
     * import of an internal package by the package name with {@code .*}
     */
    public @NotNull Set<@NotNull String> findReferences(@NotNull String code) {
        Set<String> references = new TreeSet<>();

        for (int index = 0; index < this.names.size(); index++) {
            String name = this.names.get(index);
            boolean className = isClassName(name);
            Matcher matcher = this.referencePatterns.get(index).matcher(code);

            while (matcher.find()) {
                references.add(className ? name : name + PACKAGE_SEPARATOR + matcher.group(1));
            }

            if (className && this.importOnDemandPatterns.get(index).matcher(code).find() && this.simpleNamePatterns.get(index).matcher(code).find()) {
                references.add(name);
            }
        }

        return references;
    }
}
