package org.fogofwarlabs.sc4j.manifest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

/**
 * Holds what reading a mod jar gives without loading any of its classes: the jar's path, its validated manifest and the
 * contents of the images the manifest names.
 */
public final class ModArchive {
    private final @NotNull Path path;
    private final @NotNull ModManifest manifest;
    private final @NotNull Map<@NotNull String, byte @NotNull []> images;

    /**
     * Creates a {@link ModArchive} instance.
     *
     * @param path     the mod jar
     * @param manifest the validated manifest
     * @param images   the PNG contents of every image the manifest names, by jar entry
     */
    ModArchive(@NotNull Path path, @NotNull ModManifest manifest, @NotNull Map<@NotNull String, byte @NotNull []> images) {
        this.path     = path;
        this.manifest = manifest;
        this.images   = Map.copyOf(images);
    }

    /**
     * @return the mod jar
     */
    public @NotNull Path getPath() {
        return this.path;
    }

    /**
     * @return the validated manifest
     */
    public @NotNull ModManifest getManifest() {
        return this.manifest;
    }

    /**
     * Returns the contents of an image the manifest names.
     *
     * @param entry the jar entry, such as {@code meta/icon.png} ({@link ModImages})
     * @return a copy of the PNG contents, or {@code null} when the manifest names no such image
     */
    public byte @Nullable [] getImage(@NotNull String entry) {
        byte[] contents = this.images.get(entry);

        return contents == null ? null : contents.clone();
    }
}
