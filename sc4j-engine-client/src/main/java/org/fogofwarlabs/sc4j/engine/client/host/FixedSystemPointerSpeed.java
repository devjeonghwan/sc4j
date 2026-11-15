package org.fogofwarlabs.sc4j.engine.client.host;

/**
 * Serves a platform without a pointer speed a client may change: the default {@link SystemPointerSpeed} off Windows, and of offscreen runs and
 * tests. It reads no speed and sets nothing.
 */
public final class FixedSystemPointerSpeed implements SystemPointerSpeed {
    /**
     * Creates a {@link FixedSystemPointerSpeed} instance.
     */
    public FixedSystemPointerSpeed() {
    }

    /**
     * Reads the current system pointer speed.
     *
     * @return {@code -1}: no speed can be read
     */
    @Override
    public int readSpeed() {
        return -1;
    }

    /**
     * Sets the system pointer speed for the session (not saved).
     * <p>
     * Nothing is set.
     *
     * @param speed the speed from {@link #MINIMUM_SPEED} to {@link #MAXIMUM_SPEED}
     */
    @Override
    public void writeSpeed(int speed) {
    }
}
