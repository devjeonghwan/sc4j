package org.fogofwarlabs.sc4j.runtime;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.*;

/**
 * Marks a test of the built user package ({@link RuntimeTestResources#getDistribution()}): the package holds every mod, so the test runs only
 * in the Gradle task {@code distributionTest} of this module, which builds the package first, never in {@code test}, {@code check} or
 * {@code integrationTest}. It needs no input from outside the repository.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Tag("distribution")
public @interface RequiresDistribution {
}
