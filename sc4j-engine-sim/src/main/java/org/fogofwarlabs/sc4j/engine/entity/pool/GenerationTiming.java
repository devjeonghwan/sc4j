package org.fogofwarlabs.sc4j.engine.entity.pool;

/**
 * Decides when an {@link EntityPool} advances the generation of a slot. The policy of the pool chooses it
 * ({@link SlotPolicy#getGenerationTiming()}) and the next value ({@link SlotPolicy#advanceGeneration(int)}).
 */
public enum GenerationTiming {
    /**
     * {@link EntityPool#free} advances the generation, so a handle kept after the release resolves to nothing at once. The
     * engine default.
     */
    ON_FREE,
    /**
     * The allocating system advances the generation of the first free slot with {@link EntityPool#advanceGeneration}
     * after it initialized the slot and before {@link EntityPool#commit}; {@link EntityPool#allocate()} does both. A
     * release keeps the generation, and an initialization that fails after the advance keeps the advanced generation on
     * the still free slot.
     */
    ON_INITIALIZE
}
