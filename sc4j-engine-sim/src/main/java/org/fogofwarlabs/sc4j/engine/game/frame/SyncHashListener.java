package org.fogofwarlabs.sc4j.engine.game.frame;

/**
 * Receives the state hash at the sync hash point, for example to exchange it with the other players or to compare it
 * with a recording.
 */
@FunctionalInterface
public interface SyncHashListener {
    /**
     * Called when the state hash of a frame is computed.
     *
     * @param frame the frame number the hash belongs to
     * @param hash  the state hash after the frame
     */
    void onSyncHash(int frame, long hash);
}
