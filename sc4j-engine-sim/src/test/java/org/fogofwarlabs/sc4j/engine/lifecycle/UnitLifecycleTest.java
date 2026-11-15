package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.fogofwarlabs.sc4j.engine.game.system.SystemContext;
import org.fogofwarlabs.sc4j.engine.game.system.SystemRegistry;
import org.fogofwarlabs.sc4j.engine.hook.HookRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests that the unit lifecycle calls the hooks of its points in registration order, stops at the first unit remains answer and chains the
 * terrain release results.
 */
final class UnitLifecycleTest {
    private static final int INSTRUCTION_LIMIT = 7;

    @Test
    void notify_completed_should_call_the_hooks_in_registration_order() {
        List<String> calls = new ArrayList<>();
        HookRegistry registry = new HookRegistry();
        Unit unit = new Unit(1);

        registry.add(UnitLifecycle.UNIT_COMPLETED, context -> completed -> calls.add("first " + completed.getIndex()));
        registry.add(UnitLifecycle.UNIT_COMPLETED, context -> completed -> calls.add("second " + completed.getIndex()));

        new UnitLifecycle(registry.createHooks(createContext())).notifyCompleted(unit);

        Assertions.assertEquals(List.of("first 1", "second 1"), calls);
    }

    @Test
    void notify_owner_changed_should_pass_the_instruction_limit_to_every_hook() {
        List<String> calls = new ArrayList<>();
        HookRegistry registry = new HookRegistry();
        Unit unit = new Unit(2);

        registry.add(UnitLifecycle.UNIT_OWNER_CHANGED, context -> (changed, limit) -> calls.add("first " + changed.getIndex() + " " + limit));
        registry.add(UnitLifecycle.UNIT_OWNER_CHANGED, context -> (changed, limit) -> calls.add("second " + changed.getIndex() + " " + limit));

        new UnitLifecycle(registry.createHooks(createContext())).notifyOwnerChanged(unit, INSTRUCTION_LIMIT);

        Assertions.assertEquals(List.of("first 2 7", "second 2 7"), calls);
    }

    @Test
    void notify_remains_should_stop_at_the_first_hook_that_keeps_the_unit() {
        List<String> calls = new ArrayList<>();
        HookRegistry registry = new HookRegistry();

        registry.add(UnitLifecycle.UNIT_REMAINS, context -> (dying, limit) -> {
            calls.add("first " + limit);

            return false;
        });
        registry.add(UnitLifecycle.UNIT_REMAINS, context -> (dying, limit) -> calls.add("second " + limit));
        registry.add(UnitLifecycle.UNIT_REMAINS, context -> (dying, limit) -> calls.add("third " + limit));

        UnitLifecycle lifecycle = new UnitLifecycle(registry.createHooks(createContext()));

        Assertions.assertTrue(lifecycle.notifyRemains(new Unit(1), INSTRUCTION_LIMIT));
        Assertions.assertEquals(List.of("first 7", "second 7"), calls);
        Assertions.assertFalse(new UnitLifecycle(new HookRegistry().createHooks(createContext())).notifyRemains(new Unit(1), INSTRUCTION_LIMIT));
    }

    @Test
    void notify_terrain_release_should_chain_the_results_of_every_hook() {
        List<String> calls = new ArrayList<>();
        HookRegistry registry = new HookRegistry();

        registry.add(UnitLifecycle.TERRAIN_RELEASE, context -> (dying, released) -> {
            calls.add("first " + released);

            return false;
        });
        registry.add(UnitLifecycle.TERRAIN_RELEASE, context -> (dying, released) -> {
            calls.add("second " + released);

            return released;
        });

        UnitLifecycle lifecycle = new UnitLifecycle(registry.createHooks(createContext()));

        Assertions.assertFalse(lifecycle.notifyTerrainRelease(new Unit(1)));
        Assertions.assertEquals(List.of("first true", "second false"), calls);
        Assertions.assertTrue(new UnitLifecycle(new HookRegistry().createHooks(createContext())).notifyTerrainRelease(new Unit(1)));
    }

    @Test
    void notify_completed_should_do_nothing_without_hooks() {
        UnitLifecycle lifecycle = new UnitLifecycle(new HookRegistry().createHooks(createContext()));

        Assertions.assertDoesNotThrow(() -> lifecycle.notifyCompleted(new Unit(1)));
    }

    private static @NotNull SystemContext createContext() {
        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).build();

        return new SystemRegistry().createSystems(setup).createContext(setup);
    }
}
