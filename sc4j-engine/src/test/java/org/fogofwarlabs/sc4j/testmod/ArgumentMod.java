package org.fogofwarlabs.sc4j.testmod;

import org.fogofwarlabs.sc4j.engine.mod.Mod;
import org.fogofwarlabs.sc4j.engine.mod.ModContext;
import org.fogofwarlabs.sc4j.engine.mod.ModRegistration;
import org.jetbrains.annotations.NotNull;

/**
 * A mod entry for the loader tests whose only constructor takes an argument.
 */
public final class ArgumentMod implements Mod {
    private final @NotNull String name;

    /**
     * Creates a {@link ArgumentMod} instance.
     *
     * @param name a name the loader cannot supply
     */
    public ArgumentMod(@NotNull String name) {
        this.name = name;
    }

    /**
     * @return the name given to the constructor
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * Opens and validates what the mod needs, such as external data and its version.
     *
     * @param context the settings, storage, logger and paths of the run
     */
    @Override
    public void initialize(@NotNull ModContext context) {
        // The test mod needs nothing.
    }

    /**
     * Registers what the mod adds to the engine. The registration is closed when this method returns.
     *
     * @param registration receives the mod's registrations
     */
    @Override
    public void register(@NotNull ModRegistration registration) {
        // The test mod registers nothing.
    }

    /**
     * Releases what {@link #initialize} opened, in reverse order.
     */
    @Override
    public void close() {
        // The test mod opened nothing.
    }
}
