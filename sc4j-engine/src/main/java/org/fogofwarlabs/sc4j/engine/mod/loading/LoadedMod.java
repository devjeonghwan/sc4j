package org.fogofwarlabs.sc4j.engine.mod.loading;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.mod.Mod;
import org.jetbrains.annotations.NotNull;

/**
 * Holds a mod created from its jar and the class loader of that jar. Closing it closes the class loader; the mod's own
 * {@link Mod#close()} is the caller's part of the shutdown sequence.
 */
public final class LoadedMod implements AutoCloseable {
    private final @NotNull Mod mod;
    private final @NotNull ModClassLoader classLoader;

    /**
     * Creates a {@link LoadedMod} instance.
     *
     * @param mod         the created mod
     * @param classLoader the class loader of the mod jar
     */
    LoadedMod(@NotNull Mod mod, @NotNull ModClassLoader classLoader) {
        this.mod         = mod;
        this.classLoader = classLoader;
    }

    /**
     * @return the created mod
     */
    public @NotNull Mod getMod() {
        return this.mod;
    }

    /**
     * @return the class loader of the mod jar
     */
    public @NotNull ModClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Closes the class loader of the mod jar; classes already loaded stay usable.
     */
    @Override
    public void close() {
        IgnoreException.execute(
                this.classLoader::close
        );
    }
}
