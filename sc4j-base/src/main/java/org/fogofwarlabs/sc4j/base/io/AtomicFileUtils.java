package org.fogofwarlabs.sc4j.base.io;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes files through a temporary file in the same folder that then replaces the file, so that a failed or interrupted
 * write never leaves a partly written file in its place.
 */
public final class AtomicFileUtils {
    /**
     * The suffix of the temporary file a write fills before it replaces the file.
     */
    private static final @NotNull String TEMPORARY_SUFFIX = ".tmp";

    /**
     * Writes a file: the bytes go to a temporary file in the file's folder (created when missing), which then replaces the
     * file, atomically where the file system allows. The temporary file is removed when the write fails.
     *
     * @param path     the file
     * @param contents the file bytes
     * @throws IOException if the path does not name a file or the file cannot be written
     */
    public static void write(@NotNull Path path, byte @NotNull [] contents) throws IOException {
        Path directory = path.toAbsolutePath().getParent();
        Path fileName = path.getFileName();

        if (directory == null || fileName == null) {
            throw Exceptions.newIOException("The path does not name a file: {0}", path);
        }

        Files.createDirectories(directory);

        Path temporary = Files.createTempFile(directory, fileName.toString(), TEMPORARY_SUFFIX);

        try {
            Files.write(temporary, contents);

            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private AtomicFileUtils() {
    }
}
