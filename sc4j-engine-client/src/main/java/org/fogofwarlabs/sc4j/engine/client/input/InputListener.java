package org.fogofwarlabs.sc4j.engine.client.input;

import org.jetbrains.annotations.NotNull;

/**
 * Receives the input events of a window in the order they happened: a host input adapter
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.WindowInput}), an input script or a test feeds it.
 */
public interface InputListener {
    /**
     * Handles one input event.
     *
     * @param event the event
     */
    void onInput(@NotNull InputEvent event);
}
