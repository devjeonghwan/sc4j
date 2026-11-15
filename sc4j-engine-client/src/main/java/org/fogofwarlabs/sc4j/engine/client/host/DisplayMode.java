package org.fogofwarlabs.sc4j.engine.client.host;

/**
 * Names how a {@link HostWindow} occupies the screen.
 */
public enum DisplayMode {
    /**
     * A decorated window of its own size.
     */
    WINDOWED,
    /**
     * An undecorated window that covers the primary monitor at the desktop's video mode.
     */
    BORDERLESS,
    /**
     * The primary monitor taken over at its current video mode and refresh rate.
     */
    FULLSCREEN
}
