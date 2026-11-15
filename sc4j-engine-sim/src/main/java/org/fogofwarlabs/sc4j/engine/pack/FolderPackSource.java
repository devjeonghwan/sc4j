package org.fogofwarlabs.sc4j.engine.pack;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the files of a standard pack from a folder.
 */
final class FolderPackSource implements PackSource {
    private final @NotNull Path root;

    /**
     * Creates a {@link FolderPackSource} instance.
     *
     * @param root the pack folder
     */
    FolderPackSource(@NotNull Path root) {
        this.root = root;
    }

    /**
     * @return the location of the pack, for diagnostics
     */
    @Override
    public @NotNull String getLocation() {
        return this.root.toString();
    }


    /**
     * Reads one file of the pack.
     *
     * @param path the pack path
     * @return the file contents
     * @throws IOException if the path is invalid, the pack has no such file or it cannot be read
     */
    @Override
    public byte @NotNull [] read(@NotNull String path) throws IOException {
        return Files.readAllBytes(this.root.resolve(PackSource.checkPath(path)));
    }

    /**
     * Closes the source; a folder holds nothing open.
     */
    @Override
    public void close() {
    }
}
