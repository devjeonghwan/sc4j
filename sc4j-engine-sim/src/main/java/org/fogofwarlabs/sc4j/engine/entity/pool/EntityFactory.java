package org.fogofwarlabs.sc4j.engine.entity.pool;

import org.fogofwarlabs.sc4j.engine.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the entity object of one pool slot. A pool calls it once per slot, in ascending index order, when it is
 * created.
 *
 * @param <E> the entity type
 */
@FunctionalInterface
public interface EntityFactory<E extends Entity> {
    /**
     * Creates the entity object of a slot.
     *
     * @param index the slot index, {@code 1} up to the pool capacity
     * @return the created entity, whose {@link Entity#getIndex()} is {@code index}
     */
    @NotNull E create(int index);
}
