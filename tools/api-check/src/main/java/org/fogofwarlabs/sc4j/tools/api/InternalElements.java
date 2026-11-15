package org.fogofwarlabs.sc4j.tools.api;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Holds the internal elements of compiled classes: the packages whose {@code package-info} and the classes that carry the JetBrains annotation
 * {@code ApiStatus.Internal}. They are public for the engine's own modules but are not API, so the API listing leaves them out.
 * <p>
 * An internal package covers its own classes, not its subpackages; an internal class covers its nested classes.
 */
public final class InternalElements {
    private static final @NotNull ClassDesc INTERNAL_ANNOTATION = ClassDesc.of("org.jetbrains.annotations.ApiStatus$Internal");
    private static final @NotNull String CLASS_FILE_SUFFIX = ".class";
    private static final @NotNull String PACKAGE_INFO_SUFFIX = ".package-info";
    private static final char PACKAGE_SEPARATOR = '.';
    private static final char NESTED_CLASS_SEPARATOR = '$';
    private static final char INTERNAL_NAME_SEPARATOR = '/';

    /**
     * Reads the internal elements of compiled classes from the annotations in their class files.
     *
     * @param classDirectories the folders of the compiled classes; a missing folder holds no class
     * @return the internal elements of the classes
     * @throws IOException if a class folder or a class file cannot be read
     */
    public static @NotNull InternalElements read(@NotNull List<@NotNull Path> classDirectories) throws IOException {
        ClassFile classFile = ClassFile.of();
        Set<String> packageNames = new TreeSet<>();
        Set<String> classNames = new TreeSet<>();

        for (Path directory : classDirectories) {
            if (!Files.isDirectory(directory)) {
                continue;
            }

            try (Stream<Path> files = Files.walk(directory)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    if (!file.getFileName().toString().endsWith(CLASS_FILE_SUFFIX)) {
                        continue;
                    }

                    ClassModel model = classFile.parse(file);

                    if (!isAnnotatedInternal(model)) {
                        continue;
                    }

                    String name = model.thisClass().asInternalName().replace(INTERNAL_NAME_SEPARATOR, PACKAGE_SEPARATOR);

                    if (name.endsWith(PACKAGE_INFO_SUFFIX)) {
                        packageNames.add(name.substring(0, name.length() - PACKAGE_INFO_SUFFIX.length()));
                    } else {
                        classNames.add(name);
                    }
                }
            }
        }

        return new InternalElements(packageNames, classNames);
    }

    private static boolean isAnnotatedInternal(@NotNull ClassModel model) {
        return model.findAttribute(Attributes.runtimeInvisibleAnnotations()).stream()
                    .flatMap(attribute -> attribute.annotations().stream())
                    .anyMatch(annotation -> annotation.classSymbol().equals(INTERNAL_ANNOTATION));
    }


    private final @NotNull Set<@NotNull String> packageNames;
    private final @NotNull Set<@NotNull String> classNames;

    private InternalElements(@NotNull Set<@NotNull String> packageNames, @NotNull Set<@NotNull String> classNames) {
        this.packageNames = Set.copyOf(packageNames);
        this.classNames   = Set.copyOf(classNames);
    }

    /**
     * Checks whether a compiled class is internal: its package is internal, or it or one of its enclosing classes is an internal class.
     *
     * @param className the binary class name with dots between the package segments, as {@code org.example.Outer$Nested}
     * @return {@code true} if the class is internal, {@code false} otherwise
     */
    public boolean isInternal(@NotNull String className) {
        int packageEnd = className.lastIndexOf(PACKAGE_SEPARATOR);

        if (this.packageNames.contains(packageEnd < 0 ? "" : className.substring(0, packageEnd))) {
            return true;
        }

        String name = className;

        while (!this.classNames.contains(name)) {
            int nestedIndex = name.lastIndexOf(NESTED_CLASS_SEPARATOR);

            if (nestedIndex <= packageEnd) {
                return false;
            }

            name = name.substring(0, nestedIndex);
        }

        return true;
    }
}
