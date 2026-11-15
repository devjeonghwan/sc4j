package org.fogofwarlabs.sc4j.testmod;

import org.fogofwarlabs.sc4j.engine.mod.Mod;
import org.fogofwarlabs.sc4j.engine.mod.ModContext;
import org.fogofwarlabs.sc4j.engine.mod.ModException;
import org.fogofwarlabs.sc4j.engine.mod.ModRegistration;
import org.jetbrains.annotations.NotNull;

/**
 * A test mod that refuses every run in {@link #initialize}, as a mod does for an unsupported installation.
 */
public final class RefusingMod implements Mod {
    /**
     * Creates a {@link RefusingMod} instance.
     */
    public RefusingMod() {
        // The test mod has no state.
    }

    /**
     * Opens and validates what the mod needs, such as external data and its version.
     *
     * @param context the settings, storage, logger and paths of the run
     * @throws ModException if the mod refuses the run, which this test mod always does for its unsupported installation
     */
    @Override
    public void initialize(@NotNull ModContext context) throws ModException {
        throw ModException.create("The installation {0} is not supported.", context.getSettings().getDirectory("dataDirectory"));
    }

    /**
     * Registers what the mod adds to the engine. The registration is closed when this method returns.
     *
     * @param registration receives the mod's registrations
     */
    @Override
    public void register(@NotNull ModRegistration registration) {
        // Never reached: the mod refuses the run in initialize.
    }

    /**
     * Releases what {@link #initialize} opened, in reverse order.
     */
    @Override
    public void close() {
        // The test mod opened nothing.
    }
}
