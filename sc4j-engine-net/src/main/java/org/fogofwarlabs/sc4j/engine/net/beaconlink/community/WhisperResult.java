package org.fogofwarlabs.sc4j.engine.net.beaconlink.community;

/**
 * Lists the outcomes of a whisper the session sent.
 */
public enum WhisperResult {
    /**
     * A home peer of the target relayed the whisper to the target.
     */
    DELIVERED,
    /**
     * The target is not online: the session knows no presence of it, or none of its home peers holds a registration of it. Whispers are not
     * stored for later.
     */
    NOT_ONLINE
}
