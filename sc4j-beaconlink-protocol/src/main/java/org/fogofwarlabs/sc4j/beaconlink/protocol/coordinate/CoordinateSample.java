package org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate;

/**
 * Lists what a {@link CoordinateEstimator} did with a measured round trip time.
 */
public enum CoordinateSample {
    /**
     * The sample moved the coordinate.
     */
    TAKEN,
    /**
     * The sample was not used: its time is out of range, or the remote host gave a sample a short time ago.
     */
    SKIPPED,
    /**
     * The sample was not used: the coordinate is settled and the measured and the estimated time differ too much, as a lying or a delaying
     * remote host makes them. The receiver counts it against the remote host.
     */
    OUTLIER
}
