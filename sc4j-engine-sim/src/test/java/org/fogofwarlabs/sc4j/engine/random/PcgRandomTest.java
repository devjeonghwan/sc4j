package org.fogofwarlabs.sc4j.engine.random;

import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default generator: the PCG32 reference sequence, the active gate, ranges and saved states.
 */
final class PcgRandomTest {
    private static final int SOURCE = 3;
    private static final int SOURCE_COUNT = 8;

    @Test
    void next_should_return_the_high_bits_of_the_pcg32_reference_sequence() {
        // The 32-bit outputs of the PCG32 reference generator for seed 42 and stream 54.
        int[] reference = {0xa15c02b7, 0x7b47f409, 0xba1d3330, 0x83d2f293, 0xbfa4784b, 0xcbed606e};
        PcgRandom random = createActive(42L, 54L);

        for (int expected : reference) {
            Assertions.assertEquals(expected >>> 1, random.next(SOURCE));
        }

        Assertions.assertEquals(reference.length, random.getCallCount(SOURCE));
        Assertions.assertEquals(0, random.getCallCount(0));
        Assertions.assertEquals(31, random.getOutputBits());
    }

    @Test
    void inactive_generator_returns_fixed_values_without_advancing() {
        PcgRandom random = new PcgRandom(7L, PcgRandom.DEFAULT_STREAM, SOURCE_COUNT);
        RandomState before = random.saveState();

        Assertions.assertFalse(random.isActive());
        Assertions.assertEquals(0, random.next(SOURCE));
        Assertions.assertEquals(-5, random.nextRange(SOURCE, -5, 5));
        Assertions.assertEquals(before, random.saveState());

        Assertions.assertFalse(random.setActive(true));
        Assertions.assertTrue(random.setActive(true));
    }

    @Test
    void next_range_should_cover_the_inclusive_range() {
        PcgRandom random = createActive(11L, PcgRandom.DEFAULT_STREAM);
        boolean[] seen = new boolean[4];

        for (int draw = 0; draw < 1000; draw++) {
            int value = random.nextRange(SOURCE, 10, 13);

            Assertions.assertTrue(value >= 10 && value <= 13, () -> "out of range: " + value);
            seen[value - 10] = true;
        }

        for (boolean value : seen) {
            Assertions.assertTrue(value);
        }

        Assertions.assertEquals(Integer.MIN_VALUE, createActive(1L, 1L).nextRange(SOURCE, Integer.MIN_VALUE, Integer.MIN_VALUE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> random.nextRange(SOURCE, 2, 1));
    }

    @Test
    void restore_state_should_reproduce_the_draws_counters_and_gate() {
        PcgRandom random = createActive(99L, PcgRandom.DEFAULT_STREAM);

        random.next(1);

        RandomState saved = random.saveState();
        int[] expected = new int[16];

        for (int index = 0; index < expected.length; index++) {
            expected[index] = random.next(index % SOURCE_COUNT);
        }

        random.restoreState(saved);

        for (int index = 0; index < expected.length; index++) {
            Assertions.assertEquals(expected[index], random.next(index % SOURCE_COUNT));
        }

        PcgRandom other = new PcgRandom(5L, 5L, SOURCE_COUNT);

        other.restoreState(saved);

        Assertions.assertEquals(saved, other.saveState());
        Assertions.assertEquals(1, other.getCallCount(1));
        Assertions.assertTrue(other.isActive());
        Assertions.assertEquals(computeHash(saved), computeHash(other.saveState()));
    }

    @Test
    void restore_state_should_reject_a_state_of_another_source_count() {
        PcgRandom random = new PcgRandom(1L, 1L, SOURCE_COUNT);
        RandomState other = new PcgRandom(1L, 1L, SOURCE_COUNT + 1).saveState();

        Assertions.assertThrows(IllegalArgumentException.class, () -> random.restoreState(other));
        Assertions.assertThrows(IllegalArgumentException.class, () -> random.restoreState(RandomState.of(1, 2, 3)));
    }

    @Test
    void factory_should_create_equal_generators_for_equal_seeds() {
        RandomFactory factory = PcgRandom.factory(SOURCE_COUNT);

        Assertions.assertEquals(factory.create(123L).saveState(), factory.create(123L).saveState());
        Assertions.assertNotEquals(factory.create(123L).saveState(), factory.create(124L).saveState());
    }

    private static @NotNull PcgRandom createActive(long seed, long stream) {
        PcgRandom random = new PcgRandom(seed, stream, SOURCE_COUNT);

        random.setActive(true);

        return random;
    }

    private static long computeHash(@NotNull RandomState state) {
        PcgRandom random = new PcgRandom(0L, 0L, SOURCE_COUNT);
        StateHash hash = new StateHash();

        random.restoreState(state);
        random.contributeStateHash(hash);

        return hash.getValue();
    }
}
