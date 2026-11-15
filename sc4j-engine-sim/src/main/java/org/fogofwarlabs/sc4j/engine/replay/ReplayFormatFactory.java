package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.jetbrains.annotations.NotNull;

/**
 * Creates a mod's {@link ReplayFormat} once the run's mod identity is known: a mod registers it while its registration is open
 * ({@code ModRegistration#registerReplayFormat} of the mod API), before the runtime computes the identity, and the runtime hands the
 * created format to the mod's application ({@code ModLaunch#getReplayFormats()}).
 */
@FunctionalInterface
public interface ReplayFormatFactory {
    /**
     * Creates the replay format of a run.
     *
     * @param identity the identity of the running mod, which the engine replay format records and requires ({@link EngineReplayFormat})
     * @return the created {@link ReplayFormat} instance
     */
    @NotNull ReplayFormat createReplayFormat(@NotNull ModIdentity identity);
}
