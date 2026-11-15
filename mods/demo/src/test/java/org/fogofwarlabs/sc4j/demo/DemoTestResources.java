package org.fogofwarlabs.sc4j.demo;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Names the build outputs of the demo that its tests use.
 */
public final class DemoTestResources {
    /**
     * The system property, set by the demo's build for every test task, that names the built mod jar {@code demo.jar}.
     */
    public static final @NotNull String MOD_JAR_PROPERTY = "sc4j.demo.jar";

    /**
     * @return the built mod jar of the demo
     * @throws IllegalStateException if the build did not name the jar
     */
    public static @NotNull Path getModJar() {
        String jar = System.getProperty(MOD_JAR_PROPERTY, "");

        if (jar.isBlank()) {
            throw Exceptions.newIllegalStateException("The demo tests need the system property {0} of the demo's build.", MOD_JAR_PROPERTY);
        }

        return Path.of(jar);
    }

    private DemoTestResources() {
    }
}
