package org.fogofwarlabs.sc4j.engine.hook;

import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.fogofwarlabs.sc4j.engine.game.system.SystemContext;
import org.fogofwarlabs.sc4j.engine.game.system.SystemRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the registration of hooks: their order per point, the at-most-one composition and the created hook types.
 */
final class HookRegistryTest {
    private static final @NotNull HookPoint<Listener> EVENT = HookPoint.of("test.event", Listener.class, HookComposition.EVERY_IN_ORDER);
    private static final @NotNull HookPoint<Rule> RULE = HookPoint.of("test.rule", Rule.class, HookComposition.CHAINED);
    private static final @NotNull HookPoint<Rule> SINGLE_RULE = HookPoint.of("test.single-rule", Rule.class, HookComposition.AT_MOST_ONE);

    /**
     * A notification hook of the tests.
     */
    private interface Listener {
        void onEvent();
    }

    /**
     * A decision hook of the tests.
     */
    private interface Rule {
        int decide(int value);
    }

    @Test
    void create_hooks_should_keep_the_registration_order_per_point() {
        HookRegistry registry = new HookRegistry();

        registry.add(RULE, context -> value -> value + 1);
        registry.add(EVENT, context -> () -> {
        });
        registry.add(RULE, context -> value -> value * 3);

        HookTable table = registry.createHooks(createContext());
        HookList<Rule> rules = table.get(RULE);

        Assertions.assertEquals(3, table.getCount());
        Assertions.assertEquals(2, registry.getCount(RULE));
        Assertions.assertEquals(2, rules.size());
        Assertions.assertSame(RULE, rules.getPoint());
        Assertions.assertEquals(2, rules.get(0).decide(1));
        Assertions.assertEquals(3, rules.get(1).decide(1));
        Assertions.assertEquals(1, table.get(EVENT).size());
    }

    @Test
    void a_point_without_hooks_gives_an_empty_list() {
        HookTable table = new HookRegistry().createHooks(createContext());

        Assertions.assertTrue(table.get(EVENT).isEmpty());
        Assertions.assertEquals(0, table.get(EVENT).size());
    }

    @Test
    void add_should_refuse_a_second_hook_at_an_at_most_one_point() {
        HookRegistry registry = new HookRegistry();

        registry.add(SINGLE_RULE, context -> value -> value);

        Assertions.assertThrows(IllegalStateException.class, () -> registry.add(SINGLE_RULE, context -> value -> value));
        Assertions.assertEquals(1, registry.getCount(SINGLE_RULE));
    }

    @Test
    void of_should_refuse_a_hook_type_that_is_not_an_interface() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> HookPoint.of("test.class", Object.class, HookComposition.EVERY_IN_ORDER));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void create_hooks_should_refuse_a_hook_of_another_type() {
        HookRegistry registry = new HookRegistry();
        HookPoint raw = RULE;

        registry.add(raw, context -> "not a rule");

        Assertions.assertThrows(IllegalStateException.class, () -> registry.createHooks(createContext()));
    }

    @Test
    void a_copied_registry_keeps_the_registrations_and_stays_independent() {
        HookRegistry registry = new HookRegistry();

        registry.add(RULE, context -> value -> value);

        HookRegistry copy = new HookRegistry(registry);

        registry.add(RULE, context -> value -> value);

        Assertions.assertEquals(1, copy.getCount());
        Assertions.assertEquals(2, registry.getCount());
    }

    private static @NotNull SystemContext createContext() {
        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).build();

        return new SystemRegistry().createSystems(setup).createContext(setup);
    }
}
