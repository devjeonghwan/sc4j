package org.fogofwarlabs.sc4j.engine.mod;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a game loaded into the engine; exactly one runs per runtime session. The manifest's {@code entry} names the
 * implementing class, which needs a public constructor without arguments.
 * <p>
 * The runtime calls {@link #initialize}, then {@link #register}, and {@link #close} at shutdown when {@link #initialize}
 * succeeded.
 */
public interface Mod {
    /**
     * Opens and validates what the mod needs, such as external data and its version.
     *
     * @param context the settings, storage, logger and paths of the run
     * @throws ModException if the mod refuses the run, for example for unsupported external data
     */
    void initialize(@NotNull ModContext context) throws ModException;

    /**
     * Registers what the mod adds to the engine. The registration is closed when this method returns.
     *
     * @param registration receives the mod's registrations
     * @throws ModException if the mod cannot register what it needs
     */
    void register(@NotNull ModRegistration registration) throws ModException;

    /**
     * Releases what {@link #initialize} opened, in reverse order.
     */
    void close();
}
