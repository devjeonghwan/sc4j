package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.engine.entity.EntityKind;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the range checks of the engine limits builder.
 */
final class EngineLimitsTest {
    @Test
    void build_should_accept_every_entity_pool_at_the_maximum_capacity() {
        EngineLimits limits = new EngineLimits.Builder().setUnits(EntityPool.MAXIMUM_CAPACITY).setSprites(EntityPool.MAXIMUM_CAPACITY)
                                                        .setImages(EntityPool.MAXIMUM_CAPACITY).setProjectiles(EntityPool.MAXIMUM_CAPACITY).build();

        for (EntityKind kind : EntityKind.values()) {
            Assertions.assertEquals(EntityPool.MAXIMUM_CAPACITY, limits.getCapacity(kind), kind.name());
        }
    }

    @Test
    void build_should_reject_an_entity_pool_above_the_maximum_capacity() {
        int capacity = EntityPool.MAXIMUM_CAPACITY + 1;

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setUnits(capacity).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setSprites(capacity).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setImages(capacity).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setProjectiles(capacity).build());
    }

    @Test
    void build_should_reject_a_negative_limit() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setOrders(-1).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setPaths(-1).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EngineLimits.Builder().setProjectiles(-1).build());
    }
}
