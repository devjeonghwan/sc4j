package org.fogofwarlabs.sc4j.engine.entity;

import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;

/**
 * Represents one slot of an entity pool. The index is fixed for the lifetime of the pool; the pool decides whether the
 * slot is in use and which generation it has.
 * <p>
 * A subclass holds the engine fields of one entity kind as plain fields. Mods that pool their own kinds (paths, orders,
 * effects) extend this class too. The pool never touches the fields: the system that allocates an entity initializes
 * them, and a reused slot keeps the values of its previous use until then.
 * <p>
 * The state snapshot of the pool holds every slot, free or not: a subclass writes all its fields, those of a free slot too, whose values a
 * later allocation may still read.
 */
public abstract class Entity implements StateHashContributor, SnapshotContributor {
    private final int index;

    /**
     * Creates an {@link Entity} instance for one pool slot.
     *
     * @param index the slot index, {@code 1} up to the pool capacity
     */
    protected Entity(int index) {
        this.index = index;
    }

    /**
     * @return the slot index, {@code 1} up to the pool capacity; {@code 0} never names an entity
     */
    public final int getIndex() {
        return this.index;
    }
}
