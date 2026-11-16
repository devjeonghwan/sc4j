package org.fogofwarlabs.sc4j.beaconlink.test.simulation;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.*;

/**
 * Marks a large simulation on the in-memory network ({@link SimulatedNetwork}): it runs minutes of signature work, so it runs only in the
 * Gradle task {@code simulationTest} of this module, never in {@code test} or {@code check}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Tag("simulation")
public @interface LargeSimulation {
}
