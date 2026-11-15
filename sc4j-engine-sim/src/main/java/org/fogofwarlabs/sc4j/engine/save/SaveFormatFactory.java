package org.fogofwarlabs.sc4j.engine.save;

import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.jetbrains.annotations.NotNull;

/**
 * Creates a mod's {@link SaveFormat} once the run's mod identity is known: a mod registers it while its registration is open
 * ({@code ModRegistration#registerSaveFormat} of the mod API), before the runtime computes the identity, and the runtime hands the
 * created format to the mod's application ({@code ModLaunch#getSaveFormats()}).
 */
@FunctionalInterface
public interface SaveFormatFactory {
    /**
     * Creates the save format of a run.
     *
     * @param identity the identity of the running mod, which the engine save format records and requires ({@link EngineSaveFormat})
     * @return the created {@link SaveFormat} instance
     */
    @NotNull SaveFormat createSaveFormat(@NotNull ModIdentity identity);
}
