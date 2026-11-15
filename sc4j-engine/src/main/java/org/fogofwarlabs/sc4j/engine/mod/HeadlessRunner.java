package org.fogofwarlabs.sc4j.engine.mod;

import org.jetbrains.annotations.NotNull;

/**
 * Runs an engine-only mod without window, audio and input ({@code --headless}): the simulation only, for tests, benchmarks and research. A
 * mod that registers none cannot run headless.
 */
@FunctionalInterface
public interface HeadlessRunner {
    /**
     * Runs the mod headless until the launch request is done.
     *
     * @param launch the launch request of the command line and the mod's identity
     * @throws ModException if the mod refuses the launch request or its run fails
     */
    void run(@NotNull ModLaunch launch) throws ModException;
}
