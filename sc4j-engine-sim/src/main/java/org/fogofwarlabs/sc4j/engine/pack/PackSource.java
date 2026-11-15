package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * Reads the files of a standard pack, from a folder, a zip archive or the resources of a class loader. A pack path uses {@code /} separators,
 * is relative to the pack root and never leaves it.
 */
sealed interface PackSource extends Closeable permits FolderPackSource, ZipPackSource, ResourcePackSource {
    /**
     * Joins a path relative to the folder of another pack path, such as the picture of a sprite sheet definition.
     *
     * @param definitionPath the pack path of the file that names the other one
     * @param relativePath   the path relative to that file's folder
     * @return the pack path
     * @throws IOException if the joined path leaves the pack
     */
    static @NotNull String resolveSibling(@NotNull String definitionPath, @NotNull String relativePath) throws IOException {
        int separator = definitionPath.lastIndexOf('/');

        return checkPath(separator < 0 ? relativePath : definitionPath.substring(0, separator + 1) + relativePath);
    }

    /**
     * Checks a pack path.
     *
     * @param path the pack path
     * @return the path
     * @throws IOException if the path is empty, absolute, uses {@code \} or {@code :} or has an empty, {@code .} or {@code ..} segment
     */
    static @NotNull String checkPath(@NotNull String path) throws IOException {
        if (path.isEmpty() || path.indexOf('\\') >= 0 || path.indexOf(':') >= 0) {
            throw Exceptions.newIOException("Invalid pack path: {0}", path);
        }

        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw Exceptions.newIOException("Invalid pack path: {0}", path);
            }
        }

        return path;
    }

    /**
     * @return the location of the pack, for diagnostics
     */
    @NotNull String getLocation();

    /**
     * Reads one file of the pack.
     *
     * @param path the pack path
     * @return the file contents
     * @throws IOException if the path is invalid, the pack has no such file or it cannot be read
     */
    byte @NotNull [] read(@NotNull String path) throws IOException;
}
