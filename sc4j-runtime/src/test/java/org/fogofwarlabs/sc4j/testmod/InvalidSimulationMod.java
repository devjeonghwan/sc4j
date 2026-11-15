package org.fogofwarlabs.sc4j.testmod;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.mod.Mod;
import org.fogofwarlabs.sc4j.engine.mod.ModContext;
import org.fogofwarlabs.sc4j.engine.mod.ModRegistration;
import org.jetbrains.annotations.NotNull;

/**
 * A test mod whose simulation lists a frame phase of a system it never registers, so its game definition is invalid.
 */
public final class InvalidSimulationMod implements Mod {
    /**
     * The key of the system the mod lists as a frame phase without registering it.
     */
    public static final @NotNull SystemKey<IdleSystem> IDLE = SystemKey.of("idle", IdleSystem.class);

    /**
     * A system that is a frame phase and does nothing.
     */
    public static final class IdleSystem implements GameSystem, FramePhase {
        /**
         * Creates an {@link IdleSystem} instance.
         */
        public IdleSystem() {
            // The test system has no state.
        }

        /**
         * Resolves what this system uses from the game. Called once, after every system exists.
         *
         * @param game the game
         */
        @Override
        public void attach(@NotNull Game game) {
            // The test system uses nothing.
        }

        /**
         * Runs this phase of the current frame.
         *
         * @param game the game
         */
        @Override
        public void run(@NotNull Game game) {
            // The test system does nothing.
        }
    }

    /**
     * Creates an {@link InvalidSimulationMod} instance.
     */
    public InvalidSimulationMod() {
        // The test mod has no state.
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
        registration.getSimulation().addFramePhase(IDLE);
    }

    /**
     * Releases what {@link #initialize} opened, in reverse order.
     */
    @Override
    public void close() {
        // The test mod opened nothing.
    }
}
