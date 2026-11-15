package org.fogofwarlabs.sc4j.engine.client.app;

import org.fogofwarlabs.sc4j.engine.client.display.UiSurface;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.scene.Scene;

/**
 * One screen of an engine-only mod's client: a {@link Scene} that opens with the {@link SceneClient} it runs on and draws on its
 * {@link Canvas}, between the canvas's {@link Canvas#begin} and {@link Canvas#end}.
 * <p>
 * A scene of a fixed size, for example a dialog, states the logical area it needs ({@link #getMinimumWidth}, {@link #getMinimumHeight}); the
 * client draws every frame in a logical area of at least the largest need of its scenes and the engine minimum
 * ({@link UiSurface#MINIMUM_WIDTH} by {@link UiSurface#MINIMUM_HEIGHT}), at a lower display scale where the window is smaller
 * ({@link UiSurface#fitTo}), so that no scene is cut at the window's edge.
 */
public interface ClientScene extends Scene<SceneClient, Canvas> {
    /**
     * Returns the logical width the scene needs to show whole. The default is {@code 0}: the scene lays itself out at any width from the
     * engine minimum.
     *
     * @return the logical width the scene needs
     */
    default int getMinimumWidth() {
        return 0;
    }

    /**
     * Returns the logical height the scene needs to show whole. The default is {@code 0}: the scene lays itself out at any height from the
     * engine minimum.
     *
     * @return the logical height the scene needs
     */
    default int getMinimumHeight() {
        return 0;
    }
}
