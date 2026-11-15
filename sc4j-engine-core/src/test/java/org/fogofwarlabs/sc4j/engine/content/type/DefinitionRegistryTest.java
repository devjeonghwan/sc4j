package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link DefinitionRegistry} keeps each definition at the registration-order index of its identity, and its freeze.
 */
final class DefinitionRegistryTest {
    @Test
    void definition_registry_keeps_each_definition_at_the_index_of_its_type() {
        DefinitionRegistry<SoundType, @NotNull String> registry = new DefinitionRegistry<>("sound type");
        SoundType click = registry.register(new SoundType("click"), "click definition");
        SoundType beep = registry.register(new SoundType("beep"), "beep definition");

        Assertions.assertEquals("beep definition", registry.getDefinition(beep));
        Assertions.assertEquals("click definition", registry.getDefinition(click.getIndex()));

        registry.freeze();

        Assertions.assertThrows(IllegalStateException.class, () -> registry.register(new SoundType("late"), "late definition"));
    }
}
