package org.fogofwarlabs.sc4j.engine.replay;

/**
 * Holds one state hash checkpoint of an {@link EngineReplay}: the state hash of the game at the command point of a game loop, after the loops
 * before it.
 */
final class StateCheckpoint {
    private final long loop;
    private final long stateHash;

    /**
     * Creates a {@link StateCheckpoint} instance.
     *
     * @param loop      the game loop at whose command point the hash was taken, counted from {@code 0}
     * @param stateHash the state hash of the game there
     */
    StateCheckpoint(long loop, long stateHash) {
        this.loop      = loop;
        this.stateHash = stateHash;
    }

    /**
     * @return the game loop at whose command point the hash was taken, counted from {@code 0}
     */
    long getLoop() {
        return this.loop;
    }

    /**
     * @return the state hash of the game at the command point of the loop
     */
    long getStateHash() {
        return this.stateHash;
    }
}
