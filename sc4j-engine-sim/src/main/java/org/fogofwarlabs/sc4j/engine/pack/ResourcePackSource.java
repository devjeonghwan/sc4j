package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the files of a standard pack from the resources of a class loader under a root path, such as a pack inside a mod's own jar.
 */
final class ResourcePackSource implements PackSource {
    private final @NotNull ClassLoader loader;
    private final @NotNull String root;

    /**
     * Creates a {@link ResourcePackSource} instance.
     *
     * @param loader the class loader whose resources hold the pack
     * @param root   the resource path of the pack root, a pack path without leading or trailing {@code /}
     * @throws IOException if the root is not a valid pack path
     */
    ResourcePackSource(@NotNull ClassLoader loader, @NotNull String root) throws IOException {
        this.loader = loader;
        this.root   = PackSource.checkPath(root);
    }

    /**
     * @return the location of the pack, for diagnostics
     */
    @Override
    public @NotNull String getLocation() {
        return "resources:" + this.root;
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
        String resource = this.root + "/" + PackSource.checkPath(path);

        try (InputStream input = this.loader.getResourceAsStream(resource)) {
            if (input == null) {
                throw Exceptions.newIOException("The pack {0} has no file {1}.", this.getLocation(), path);
            }

            return input.readAllBytes();
        }
    }

    /**
     * Closes the source; the class loader stays open.
     */
    @Override
    public void close() {
    }
}
