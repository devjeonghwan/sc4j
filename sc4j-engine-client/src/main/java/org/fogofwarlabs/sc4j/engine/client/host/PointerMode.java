package org.fogofwarlabs.sc4j.engine.client.host;

/**
 * Names how the platform pointer behaves over a {@link HostWindow} ({@link HostWindow#setPointerMode}).
 */
public enum PointerMode {
    /**
     * The pointer is shown and moves freely.
     */
    NORMAL,
    /**
     * The pointer is invisible while it is over the window's content area and moves freely; a client draws its own cursor there.
     */
    HIDDEN,
    /**
     * The pointer is shown but kept inside the window's content area while the window is focused.
     */
    CAPTURED
}
