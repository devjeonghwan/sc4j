package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads the files of a standard pack from a zip archive whose root holds the pack root.
 */
final class ZipPackSource implements PackSource {
    private final @NotNull Path archive;
    private final @NotNull ZipFile zip;

    /**
     * Creates a {@link ZipPackSource} instance and opens the archive.
     *
     * @param archive the zip archive
     * @throws IOException if the archive cannot be opened
     */
    ZipPackSource(@NotNull Path archive) throws IOException {
        this.archive = archive;
        this.zip     = new ZipFile(archive.toFile());
    }

    /**
     * @return the location of the pack, for diagnostics
     */
    @Override
    public @NotNull String getLocation() {
        return this.archive.toString();
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
        ZipEntry entry = this.zip.getEntry(PackSource.checkPath(path));

        if (entry == null || entry.isDirectory()) {
            throw Exceptions.newIOException("The pack {0} has no file {1}.", this.archive, path);
        }

        try (InputStream input = this.zip.getInputStream(entry)) {
            byte[] contents = input.readAllBytes();

            if (entry.getSize() >= 0 && contents.length != entry.getSize()) {
                throw Exceptions.newIOException("The pack file {0} of {1} is truncated.", path, this.archive);
            }

            return contents;
        }
    }

    /**
     * Closes the archive.
     *
     * @throws IOException if the archive cannot be closed
     */
    @Override
    public void close() throws IOException {
        this.zip.close();
    }
}
