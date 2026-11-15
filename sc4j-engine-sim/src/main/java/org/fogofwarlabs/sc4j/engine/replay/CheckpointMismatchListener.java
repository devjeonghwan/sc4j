package org.fogofwarlabs.sc4j.engine.replay;

/**
 * Receives the state hash checkpoints of a replay that the replayed game does not reach ({@link ReplayController}): the game runs differently
 * from the recorded one from that game loop on, for example because its content changed.
 */
@FunctionalInterface
public interface CheckpointMismatchListener {
    /**
     * Called when the replayed game's state hash differs from a checkpoint.
     *
     * @param loop          the game loop of the checkpoint, counted from {@code 0}
     * @param recordedHash  the state hash the recording took
     * @param localHash     the state hash of the replayed game
     * @param mismatchCount the mismatches detected so far, this one included
     */
    void onCheckpointMismatch(long loop, long recordedHash, long localHash, int mismatchCount);
}
