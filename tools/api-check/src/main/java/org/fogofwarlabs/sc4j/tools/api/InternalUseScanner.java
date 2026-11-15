package org.fogofwarlabs.sc4j.tools.api;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scans Java sources for uses of the internal elements of the engine. Comments, string, text block and character literals are removed
 * first, so only code counts: a Javadoc sentence or a message text that names an internal class is no use.
 */
public final class InternalUseScanner {
    private static final @NotNull Pattern COMMENTS_AND_LITERALS = Pattern.compile(
            "(?s)\"\"\".*?\"\"\"|/\\*.*?\\*/|//[^\\n]*|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'"
    );
    private static final @NotNull String JAVA_FILE_SUFFIX = ".java";
    private static final @NotNull String USE_SEPARATOR = ": ";
    private static final @NotNull String CODE_REPLACEMENT = " ";
    private static final char PATH_SEPARATOR = '/';


    private final @NotNull InternalElements internal;

    /**
     * Creates an {@link InternalUseScanner} instance.
     *
     * @param internal the internal elements to look for
     */
    public InternalUseScanner(@NotNull InternalElements internal) {
        this.internal = internal;
    }

    /**
     * Scans the Java sources of source folders.
     *
     * @param base          the folder that the reported paths are relative to, such as the module folder
     * @param sourceFolders the source folders; a missing folder holds no source
     * @return one line per source file and internal element it uses, as {@code <path>: <element>} with {@code /} between path segments,
     * sorted
     * @throws IOException if a source folder or file cannot be read
     */
    public @NotNull List<@NotNull String> scan(@NotNull Path base, @NotNull List<@NotNull Path> sourceFolders) throws IOException {
        Set<String> uses = new TreeSet<>();

        for (Path folder : sourceFolders) {
            if (!Files.isDirectory(folder)) {
                continue;
            }

            try (Stream<Path> files = Files.walk(folder)) {
                for (Path file : files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(JAVA_FILE_SUFFIX)).toList()) {
                    String code = COMMENTS_AND_LITERALS.matcher(Files.readString(file, StandardCharsets.UTF_8)).replaceAll(CODE_REPLACEMENT);
                    String path = base.relativize(file).toString().replace(file.getFileSystem().getSeparator().charAt(0), PATH_SEPARATOR);

                    for (String element : this.internal.findReferences(code)) {
                        uses.add(path + USE_SEPARATOR + element);
                    }
                }
            }
        }

        return List.copyOf(uses);
    }
}
