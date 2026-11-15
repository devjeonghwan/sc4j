package org.fogofwarlabs.sc4j.engine.mod;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Gives a mod what the runtime prepared for its run: the validated manifest and settings, its own storage folder, the install folder, the
 * mod-specific command line arguments and a logger.
 */
public interface ModContext {
    /**
     * @return the validated manifest of the mod
     */
    @NotNull ModManifest getManifest();

    /**
     * @return the validated values of the pre-run settings the launcher saved
     */
    @NotNull ModSettings getSettings();

    /**
     * @return the mod's own storage, {@code <install>/userdata/mods/<modId>/} unless the command line names another folder
     */
    @NotNull ModStorage getStorage();

    /**
     * @return the SC4J install folder
     */
    @NotNull Path getInstallDirectory();

    /**
     * @return the command line arguments after {@code --}, which only the mod interprets
     */
    @NotNull List<@NotNull String> getArguments();

    /**
     * @return the logger of the mod, writing to the runtime's log
     */
    @NotNull Logger getLogger();
}
