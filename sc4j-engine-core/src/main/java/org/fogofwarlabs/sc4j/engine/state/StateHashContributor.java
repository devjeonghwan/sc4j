package org.fogofwarlabs.sc4j.engine.state;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a part of the simulation state that contributes to the state hash.
 * <p>
 * Determinism contract: a contributor mixes the same values in the same order for the same state, on every machine. It
 * iterates arrays and lists by index, never a hash-ordered collection, mixes no identity hash code, no clock and no
 * floating-point value, and changes no state.
 */
public interface StateHashContributor {
    /**
     * Mixes this part of the simulation state into the hash.
     *
     * @param hash the hash being computed
     */
    void contributeStateHash(@NotNull StateHash hash);
}
