package org.fogofwarlabs.sc4j.engine.game.system;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameDefinition;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the system registry: creation and attach in registration order, the systems a factory sees, replacement in place and lookup by key.
 */
final class SystemRegistryTest {
    private static final @NotNull SystemKey<RecordingSystem> FIRST = SystemKey.of("first", RecordingSystem.class);
    private static final @NotNull SystemKey<RecordingSystem> SECOND = SystemKey.of("second", RecordingSystem.class);
    private static final @NotNull SystemKey<RecordingSystem> UNREGISTERED = SystemKey.of("unregistered", RecordingSystem.class);

    /**
     * Records its creation and attach in a shared log and resolves the first system when attached.
     */
    private static final class RecordingSystem implements GameSystem {
        private final @NotNull String name;
        private final @NotNull List<@NotNull String> log;

        private @Nullable RecordingSystem first;

        private RecordingSystem(@NotNull String name, @NotNull List<@NotNull String> log) {
            this.name  = name;
            this.log   = log;
            this.first = null;

            log.add("create " + name);
        }

        @Override
        public void attach(@NotNull Game game) {
            this.first = game.getModSystem(FIRST);
            this.log.add("attach " + this.name);
        }
    }

    @Test
    void systems_are_created_and_attached_in_registration_order() {
        List<String> log = new ArrayList<>();
        GameDefinition definition = new GameDefinition.Builder()
                .useSystem(FIRST, context -> new RecordingSystem("first", log))
                .useSystem(SECOND, context -> new RecordingSystem("second", log))
                .build();
        Game game = Game.create(definition, createSetup());

        Assertions.assertEquals(List.of("create first", "create second", "attach first", "attach second"), log);
        Assertions.assertSame(game.getModSystem(FIRST), game.getModSystem(SECOND).first);
        Assertions.assertTrue(game.getSystems().isAttached());
        Assertions.assertThrows(IllegalStateException.class, () -> game.getSystems().attach(game));
    }

    @Test
    void use_should_replace_a_registered_factory_at_its_position() {
        List<String> log = new ArrayList<>();
        SystemRegistry registry = new SystemRegistry();

        registry.use(FIRST, context -> new RecordingSystem("first", log));
        registry.use(SECOND, context -> new RecordingSystem("second", log));
        registry.use(FIRST, context -> new RecordingSystem("replacement", log));

        SystemSet systems = registry.createSystems(createSetup());

        Assertions.assertEquals(2, registry.getCount());
        Assertions.assertSame(FIRST, systems.getKey(0));
        Assertions.assertEquals("replacement", systems.get(FIRST).name);
        Assertions.assertEquals(List.of("create replacement", "create second"), log);
    }

    @Test
    void lookups_should_fail_or_return_null_for_an_unregistered_key() {
        SystemRegistry registry = new SystemRegistry();

        registry.use(FIRST, context -> new RecordingSystem("first", new ArrayList<>()));

        SystemSet systems = registry.createSystems(createSetup());

        Assertions.assertTrue(registry.contains(FIRST));
        Assertions.assertFalse(registry.contains(UNREGISTERED));
        Assertions.assertNull(systems.find(UNREGISTERED));
        Assertions.assertThrows(IllegalArgumentException.class, () -> systems.get(UNREGISTERED));
        Assertions.assertEquals(1, new SystemRegistry(registry).getCount());
    }

    @Test
    void create_systems_should_give_each_factory_the_setup_and_the_systems_created_before_it() {
        List<String> log = new ArrayList<>();
        GameSetup setup = createSetup();
        SystemRegistry registry = new SystemRegistry();
        List<RecordingSystem> seen = new ArrayList<>();

        registry.use(FIRST, context -> new RecordingSystem(context.getSetup().getMap().getName(), log));
        registry.use(SECOND, context -> {
            seen.add(context.require(FIRST));

            return new RecordingSystem("second", log);
        });
        registry.use(UNREGISTERED, context -> context.require(UNREGISTERED));

        Assertions.assertThrows(IllegalStateException.class, () -> registry.createSystems(setup));
        Assertions.assertEquals(List.of("create empty", "create second"), log);

        registry.use(UNREGISTERED, context -> new RecordingSystem("third", log));

        SystemSet systems = registry.createSystems(setup);
        SystemContext context = systems.createContext(setup);

        Assertions.assertSame(systems.get(FIRST), seen.getLast());
        Assertions.assertSame(systems.get(UNREGISTERED), context.require(UNREGISTERED));
        Assertions.assertSame(setup, context.getSetup());
    }

    private static @NotNull GameSetup createSetup() {
        return new GameSetup.Builder(new MapReference("empty", new byte[0])).build();
    }
}
