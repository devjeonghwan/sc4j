package org.fogofwarlabs.sc4j.engine.client.app;

import org.fogofwarlabs.sc4j.engine.mod.ModException;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the first scene of an engine-only mod's client: the runtime calls it once the window and the devices are open, and the
 * {@link SceneClient} opens the scene. The launch request ({@link SceneClient#getLaunch()}) says whether the run plays a replay or joins a
 * game, so the mod may start with its replay viewer or its lobby instead of its menu.
 */
@FunctionalInterface
public interface FirstSceneFactory {
    /**
     * Creates the first scene.
     *
     * @param client the client the scene runs on
     * @return the created {@link ClientScene} instance, not opened yet
     * @throws ModException if the mod refuses the launch request
     */
    @NotNull ClientScene createFirstScene(@NotNull SceneClient client) throws ModException;
}
