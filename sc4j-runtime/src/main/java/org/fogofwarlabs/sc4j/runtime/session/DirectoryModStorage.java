package org.fogofwarlabs.sc4j.runtime.session;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.mod.ModStorage;
import org.jetbrains.annotations.NotNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Provides a mod's storage as one folder of the file system, rejecting every path that would leave it.
 */
public final class DirectoryModStorage implements ModStorage {
    private final @NotNull Path root;

    /**
     * Creates a {@link DirectoryModStorage} instance.
     *
     * @param root the storage folder, made absolute and normalized
     */
    public DirectoryModStorage(@NotNull Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /**
     * @return the absolute, normalized storage folder
     */
    @Override
    public @NotNull Path getRoot() {
        return this.root;
    }


    /**
     * Resolves a path inside the storage folder.
     *
     * @param relativePath the relative path with {@code /} separators, such as {@code replays/last-game.sc4r}
     * @return the absolute, normalized path inside the storage folder
     * @throws IllegalArgumentException if the path is absolute, invalid or leaves the storage folder
     */
    @Override
    public @NotNull Path resolve(@NotNull String relativePath) {
        Path relative;

        try {
            relative = Path.of(relativePath);
        } catch (InvalidPathException exception) {
            throw Exceptions.newIllegalArgumentException(exception, "Invalid storage path: {0}", relativePath);
        }

        if (relative.isAbsolute() || relative.getRoot() != null) {
            throw Exceptions.newIllegalArgumentException("A storage path must be relative: {0}", relativePath);
        }

        Path resolved = this.root.resolve(relative).normalize();

        if (!resolved.startsWith(this.root)) {
            throw Exceptions.newIllegalArgumentException("The storage path leaves the storage folder: {0}", relativePath);
        }

        return resolved;
    }
}
