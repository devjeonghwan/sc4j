package org.fogofwarlabs.sc4j.engine.client.host;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the OpenGL context version a window is created with: the engine defaults, or the version a mod sets.
 */
final class WindowSettingsTest {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final int MAJOR_VERSION = 4;
    private static final int MINOR_VERSION = 1;

    @Test
    void set_context_version_should_replace_the_default_version() {
        WindowSettings defaults = WindowSettings.builder("window", WIDTH, HEIGHT).build();
        WindowSettings settings = WindowSettings.builder("window", WIDTH, HEIGHT).setContextVersion(MAJOR_VERSION, MINOR_VERSION).build();

        Assertions.assertEquals(WindowSettings.DEFAULT_CONTEXT_MAJOR_VERSION, defaults.getContextMajorVersion());
        Assertions.assertEquals(WindowSettings.DEFAULT_CONTEXT_MINOR_VERSION, defaults.getContextMinorVersion());
        Assertions.assertEquals(MAJOR_VERSION, settings.getContextMajorVersion());
        Assertions.assertEquals(MINOR_VERSION, settings.getContextMinorVersion());
    }
}
