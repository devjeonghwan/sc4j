package org.fogofwarlabs.sc4j.engine.client.host;

/**
 * Names what one frame of a {@link HostApplication} did, which decides whether the host paces it and whether the loop goes on.
 */
public enum FrameResult {
    /**
     * The frame was presented: the host waits for the next frame under the frame rate limit and runs the next frame.
     */
    PRESENTED,
    /**
     * Nothing was presented (loading, a minimized window): the host runs the next frame at once.
     */
    SKIPPED,
    /**
     * The frame was presented and was the last one: the host waits under the frame rate limit and ends the loop.
     */
    LAST;

    /**
     * @return {@code true} if the frame was presented and is paced, {@code false} otherwise
     */
    public boolean isPresented() {
        return this != SKIPPED;
    }
}
