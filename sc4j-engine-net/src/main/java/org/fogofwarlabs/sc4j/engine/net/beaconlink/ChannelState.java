package org.fogofwarlabs.sc4j.engine.net.beaconlink;

/**
 * Lists the states of a {@link ParticipantChannel}.
 */
public enum ChannelState {
    /**
     * The participants exchange their keys and addresses through the nodes and look for a path between them.
     */
    CONNECTING,
    /**
     * The channel has a path to the participant and carries datagrams.
     */
    OPEN,
    /**
     * The channel was closed by its owner, or failed ({@link ParticipantChannel#getFailure()}); it carries nothing more.
     */
    CLOSED
}
