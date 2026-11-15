package org.fogofwarlabs.sc4j.engine.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the missing resource failure.
 */
final class ResourceNotFoundExceptionTest {
    @Test
    void create_should_name_type_and_key() {
        ResourceNotFoundException exception = ResourceNotFoundException.create(ResourceType.SOUND, ResourceKey.of("click"));

        Assertions.assertEquals(ResourceType.SOUND, exception.getType());
        Assertions.assertEquals(ResourceKey.of("click"), exception.getKey());
        Assertions.assertTrue(exception.getMessage().contains("click"));
    }
}
